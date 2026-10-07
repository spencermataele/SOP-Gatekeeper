import {formattedSopTitle, SopHierarchyNode} from './sop-title';
import {Component, HostListener, Injectable, OnInit} from '@angular/core';
import {CanDeactivate} from '@angular/router';

import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {firstValueFrom, forkJoin} from 'rxjs';
import {environment} from '../../../environments/environment';
import {AuthService} from '../authorization/auth.service';
import {SopEditorComponent} from './sop-editor.component';
import {SopContentComponent} from './sop-content.component';
import {GovernedProcess, ProcessSelectorComponent} from './process-selector.component';
import {blankTemplate, readyToSubmit} from './sop-template';

interface Snapshot { id?: number; title: string; description: string; details: string; }
interface Copy extends Snapshot { copyId: number; version: number; state: string; }
interface RequestView {
  requestId: number; documentId: number; processId?: number; version: number; state: string; title: string; author: string;
  candidateId: number; candidate?: Snapshot; published?: Snapshot; actions: string[];
  ownCopies: {copyId: number; state: string; version: number}[];
  versions: Snapshot[]; activity: {action: string; actor: string; reason: string; recordedAt: string}[];
}
interface DocumentView extends Snapshot {processId?: number; nodeKey?: string; publishedVersion?: number; isProcessOwner?: boolean; documentId: number; revisionId: number; kind: string;}

@Component({selector: 'app-workflow', standalone: true, imports: [CommonModule, FormsModule, SopEditorComponent, SopContentComponent, ProcessSelectorComponent],
  templateUrl: './workflow.component.html', styleUrls: ['./workflow.component.css']})
export class WorkflowComponent implements OnInit {
  readonly api = environment.apiBaseUrl + '/api/lifecycle';
  tab = 'library'; busy = false; error = ''; message = ''; creating = false; dirty = false;
  documents: DocumentView[] = []; requests: RequestView[] = [];
  processes: GovernedProcess[] = [];
  subgroups: {id: number; name: string; processId: number}[] = [];
  readyToSubmit = readyToSubmit;
  notices: {id: string; requestId: number; suggestionId?:string; action: string; recordedAt: string; title: string; actor: string}[] = [];
  request?: RequestView; copy?: Copy; document?: DocumentView;
  hierarchy: SopHierarchyNode[] = [];
  librarySelection: string[] = [];
  readonly libraryLevels = ['Organization', 'Organization group', 'Department', 'Subdepartment', 'Process family', 'Process'];
  private readonly libraryKinds = ['org', 'group', 'department', 'subgroup', 'family', 'process'];
  private readonly numericOrder = new Intl.Collator('en', {numeric: true, sensitivity: 'base'});
  private compareNodes(a: SopHierarchyNode, b: SopHierarchyNode): number {
    if (!!a.code !== !!b.code) return a.code ? -1 : 1;
    return this.numericOrder.compare(a.code || '', b.code || '') || a.name.localeCompare(b.name);
  }
  libraryOptions(index: number): SopHierarchyNode[] {
    if (index > 0 && !this.librarySelection[index - 1]) return [];
    return this.hierarchy.filter(n => n.nodeKey.startsWith(this.libraryKinds[index] + ':') &&
      (n.parentKey || '') === (index ? this.librarySelection[index - 1] : '')).sort((a,b) => this.compareNodes(a,b));
  }
  chooseLibraryLevel(index: number, key: string): void {
    this.librarySelection = this.librarySelection.slice(0,index);
    if (key) this.librarySelection.push(key);
  }
  private documentPath(doc: DocumentView): SopHierarchyNode[] {
    const path: SopHierarchyNode[] = []; const seen = new Set<string>();
    let node = this.hierarchy.find(n => n.nodeKey === (doc.nodeKey || `process:${doc.processId}`));
    while (node && !seen.has(node.nodeKey)) {
      seen.add(node.nodeKey); path.unshift(node);
      const parent = node.parentKey; node = this.hierarchy.find(n => n.nodeKey === parent);
    }
    return path;
  }
  get filteredDocuments(): DocumentView[] {
    const selected = this.librarySelection[this.librarySelection.length - 1];
    return this.documents.map(doc => ({doc,path:this.documentPath(doc)}))
      .filter(item => !selected || item.path.some(n => n.nodeKey === selected))
      .sort((a,b) => {
        for (let i=0;i<Math.max(a.path.length,b.path.length);i++) {
          if (!a.path[i] || !b.path[i]) return a.path[i] ? -1 : 1;
          const order = this.compareNodes(a.path[i],b.path[i]); if (order) return order;
        }
        return a.doc.title.localeCompare(b.doc.title) || a.doc.documentId-b.doc.documentId;
      }).map(item => item.doc);
  }
  documentLabel(doc: DocumentView): string {
    return formattedSopTitle(this.hierarchy, doc.nodeKey || `process:${doc.processId}`, doc.title,
      doc.publishedVersion === undefined ? 'Published' : `v${doc.publishedVersion}`);
  }
  publicationVersions = new Map<number, number>();
  revisionRationale=''; revisionSuggestion=''; eligibleSuggestions:{id:string;title:string}[]=[]; preparingRevision=false;
  history: Snapshot[] = []; left?: Snapshot; right?: Snapshot;
  processId = 0; title = ''; description = ''; details = ''; reason = '';
  private commands = new Map<string, string>();
  get availableSubgroups(): {id: number; name: string}[] { return this.subgroups.filter(s=>s.processId === (this.creating ? this.processId : this.request?.processId)); }
  processChanged(id: number): void {
    this.processId = id; this.dirty = true;
    const value=JSON.parse(this.details); value.subgroupId=null; this.details=JSON.stringify(value);
  }
  constructor(private http: HttpClient, public auth: AuthService) {}
  ngOnInit(): void { void this.refresh(); }
  can(action: string): boolean { return !!this.request?.actions.includes(action); }
  get canDecide(): boolean { return ['APPROVE', 'SELF_APPROVE', 'REJECT'].some(action => this.can(action)); }
  get canProvideReason(): boolean { return this.canDecide || this.can('SUGGEST_EDITS') || this.can('REASSIGN'); }
  label(value: string): string { return value.split('_').join(' '); }
  get comparingCurrentCandidate(): boolean { return this.right?.id === this.request?.candidateId; }
  canLeave(): boolean { return this.leaveEditor(); }
  @HostListener('window:beforeunload', ['$event'])
  beforeUnload(event: BeforeUnloadEvent): void { if (this.dirty) { event.preventDefault(); event.returnValue = ''; } }
  async refresh(): Promise<void> {
    this.busy = true; this.error = '';
    try { await this.loadLists(); } catch (e) { this.fail(e); } finally { this.busy = false; }
  }
  private async loadLists(): Promise<void> {
    const data = await firstValueFrom(forkJoin({
      hierarchy: this.http.get<SopHierarchyNode[]>(this.api + '/library/hierarchy'),
      documents: this.http.get<DocumentView[]>(this.api + '/documents'),
      requests: this.http.get<RequestView[]>(this.api + '/requests'),
      processes: this.http.get<typeof this.processes>(this.api + '/processes'),
      subgroups: this.http.get<typeof this.subgroups>(this.api + '/subgroups'),
      notices: this.http.get<typeof this.notices>(this.api + '/notifications')
    }));
    Object.assign(this, data);
  }
  private leaveEditor(): boolean {
    return !this.dirty || window.confirm('Discard your unsaved changes? Saved drafts are kept.');
  }
  async openRequest(id: number): Promise<void> {
    if (!this.leaveEditor()) return;
    this.busy = true; this.error = ''; this.message = '';
    try { await this.loadRequest(id); } catch (e) { this.fail(e); } finally { this.busy = false; }
  }
  private async loadRequest(id: number): Promise<void> {
    this.request = await firstValueFrom(this.http.get<RequestView>(`${this.api}/requests/${id}`));
    const publications = await firstValueFrom(this.http.get<Snapshot[]>(`${this.api}/documents/${this.request.documentId}/history`));
    this.setPublicationVersions(publications);
    this.copy = undefined; this.dirty = false; this.reason = ''; this.document = undefined; this.creating = false;
    this.history = this.request.versions || [];
    this.left = this.request.published || this.history.find(v => v.id !== this.request!.candidateId); this.right = this.request.candidate;
    this.tab = 'work';
    const editable = this.request.ownCopies.find(c => c.state === 'EDITABLE');
    if (editable && this.can('EDIT')) await this.loadCopy(editable.copyId);
  }
  async openCopy(id: number): Promise<void> {
    if (!this.leaveEditor()) return;
    this.busy = true; this.error = '';
    try { await this.loadCopy(id); } catch (e) { this.fail(e); } finally { this.busy = false; }
  }
  private async loadCopy(id: number): Promise<void> {
    this.copy = await firstValueFrom(this.http.get<Copy>(`${this.api}/requests/${this.request!.requestId}/copies/${id}`));
    this.dirty = false;
  }
  async openDocument(doc: DocumentView): Promise<void> {
    if (!this.leaveEditor()) return;
    this.busy = true; this.error = ''; this.message = '';
    try {
      const history = await firstValueFrom(this.http.get<Snapshot[]>(`${this.api}/documents/${doc.documentId}/history`));
      this.document = doc; this.request = undefined; this.copy = undefined; this.creating = false; this.dirty = false;
      this.preparingRevision=false;
      this.setPublicationVersions(history);
      this.history = history; this.left = history[1]; this.right = history[0];
    } catch (e) { this.fail(e); } finally { this.busy = false; }
  }
  private setPublicationVersions(publications: Snapshot[]): void {
    this.publicationVersions = new Map(publications.filter(v=>v.id!==undefined).map((v,index)=>[v.id!,publications.length-index]));
  }
  snapshotLabel(snapshot: Snapshot | undefined): string {
    if (!snapshot) return '';
    const published = snapshot.id === undefined ? undefined : this.publicationVersions.get(snapshot.id);
    const index = this.history.findIndex(v=>v.id===snapshot.id);
    const candidateNumber = index >= 0 ? this.history.length-index : this.history.length || 1;
    let version = published !== undefined ? `v${published}` : `Candidate ${candidateNumber}`;
    if (snapshot.id === this.request?.candidateId) version += published !== undefined ? ' — Published candidate' : ' — Current candidate';
    else if (snapshot.id === this.request?.published?.id || snapshot.id === this.document?.revisionId) version += ' — Current publication';
    const nodeKey = this.document?.nodeKey || `process:${this.request?.processId ?? this.document?.processId}`;
    return formattedSopTitle(this.hierarchy,nodeKey,snapshot.title,version);
  }
  newDraft(): void {
    if (!this.leaveEditor()) return;
    this.request = undefined; this.document = undefined; this.copy = undefined; this.creating = true;
    this.title = ''; this.description = ''; this.details = JSON.stringify(blankTemplate()); this.processId=0;
    this.dirty = false; this.error = ''; this.message = '';
  }
  async create(): Promise<void> {
    await this.command('/documents', {processId: Number(this.processId), title: this.title, description: this.description, details: this.details}, 'Draft created.', true);
  }
  async startRevision(): Promise<void> {
    await this.command(`/documents/${this.document!.documentId}/drafts`, {publishedRevisionId: this.document!.revisionId,suggestionId:this.revisionSuggestion||null,rationale:this.revisionRationale}, 'Revision draft created.', true);
    if(!this.error)this.preparingRevision=false;
  }
  async prepareRevision():Promise<void>{this.busy=true;this.error='';try{this.eligibleSuggestions=await firstValueFrom(this.http.get<{id:string;title:string}[]>(this.api+'/suggestions/eligible?documentId='+this.document!.documentId));this.revisionRationale='';this.revisionSuggestion='';this.preparingRevision=true;}catch(e){this.fail(e);}finally{this.busy=false;}}
  async save(): Promise<void> {
    const c = this.copy!;
    await this.command(`/requests/${this.request!.requestId}/copies/${c.copyId}`, {
      requestVersion: this.request!.version, copyVersion: c.version, title: c.title, description: c.description, details: c.details
    }, 'Private copy saved.', false, true);
  }
  async submit(): Promise<void> {
    await this.command(`/requests/${this.request!.requestId}/submit`, {
      requestVersion: this.request!.version, copyId: this.copy!.copyId, copyVersion: this.copy!.version, reason: this.reason
    }, 'Candidate submitted. Eligible approvers have been notified.');
  }
  async act(action: string): Promise<void> {
    const r = this.request!;
    const body: any = {requestVersion: r.version};
    if (!['cancel', 'revise'].includes(action)) { body.candidateId = r.candidateId; body.reason = this.reason; }
    if (action === 'approve' || action === 'self') body.mode = action === 'self' ? 'SELF_APPROVAL' : 'NORMAL';
    const endpoint = action === 'self' ? 'approve' : action;
    await this.command(`/requests/${r.requestId}/${endpoint}`, body,
      action === 'approve' || action === 'self' ? 'SOP published.' : 'Workflow updated.', ['revise', 'reviewer-copies'].includes(action));
  }
  private async command(path: string, body: object, message: string, opensCopy = false, saving = false): Promise<void> {
    if (this.busy) return;
    this.busy = true; this.error = ''; this.message = '';
    const key = path + JSON.stringify(body);
    const commandId = this.commands.get(key) || crypto.randomUUID();
    this.commands.set(key, commandId);
    try {
      const payload = {...body, commandId};
      const result: any = await firstValueFrom(saving ? this.http.put(this.api + path, payload) : this.http.post(this.api + path, payload));
      this.commands.delete(key); this.dirty = false;
      this.message = message;
      try {
        if (saving) { this.copy!.version = result.version; }
        else if (opensCopy) { await this.loadRequest(result.requestId); await this.loadCopy(result.copyId); }
        else if (this.request) {
          try { await this.loadRequest(this.request.requestId); }
          catch (e: any) {
            if (e.status !== 403) throw e;
            this.request = undefined; this.copy = undefined;
            this.message += ' Your review assignment has ended.';
          }
        }
        await this.loadLists();
      } catch (e) {
        this.request = undefined; this.copy = undefined; this.creating = false;
        this.error = 'The change was saved, but the refreshed view could not be loaded. Select Refresh before continuing.';
      }
    } catch (e) { this.fail(e); } finally { this.busy = false; }
  }
  private fail(e: any): void {
    this.error = e.status === 409 ? 'This work changed since you opened it. Copy any unsaved text, then reopen the request to review the latest version.'
      : e.status === 401 ? 'Your session expired. Log out and log in again.'
      : e.status === 403 ? 'You no longer have permission for this action. Refresh to see your available work.'
      : typeof e.error?.message === 'string' ? e.error.message : 'The request could not be completed. Your unsaved text is still here. Check the connection and try again.';
  }
}

@Injectable({providedIn: 'root'})
export class WorkflowUnsavedGuard implements CanDeactivate<WorkflowComponent> {
  canDeactivate(component: WorkflowComponent): boolean { return component.canLeave(); }
}
