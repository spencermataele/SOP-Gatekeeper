import {ComponentFixture, TestBed, fakeAsync, tick} from '@angular/core/testing';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {blankTemplate} from './sop-template';
import {WorkflowComponent} from './workflow.component';
import {AuthService} from '../authorization/auth.service';

function structured(what: string): string { const value = blankTemplate(); value.steps[0] = {id:'00000000-0000-0000-0000-000000000001', who:'Operator',what,where:'Workstation',notes:''}; return JSON.stringify(value); }

describe('Governed workflow workspace', () => {
  let fixture: ComponentFixture<WorkflowComponent>;
  let component: WorkflowComponent;
  let http: HttpTestingController;
  beforeEach(async () => {
    await TestBed.configureTestingModule({imports: [WorkflowComponent, HttpClientTestingModule], providers: [
      {provide: AuthService, useValue: {currentUser: () => ({fullName: 'Practice author', username: 'demo.author'})}}
    ]}).compileComponents();
    fixture = TestBed.createComponent(WorkflowComponent); component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function lists(): void {
    for (const name of ['documents', 'requests', 'processes', 'notifications', 'subgroups', 'library/hierarchy']) http.expectOne(component.api + '/' + name).flush([]);
    tick(); fixture.detectChanges();
  }
  function start(): void { fixture.detectChanges(); lists(); }
  function request(actions: string[] = ['EDIT', 'SUBMIT']): void {
    component.request = {requestId: 9, documentId: 10, version: 3, state: 'IN_REVIEW', title: 'Procedure', author: 'Author', candidateId: 20,
      actions, ownCopies: [], versions: [], activity: [], candidate: {id: 20, title: 'Procedure', description: 'Purpose', details: 'Steps'}};
    component.right = component.request.candidate;
    component.copy = {copyId: 7, version: 4, state: 'EDITABLE', title: 'Procedure', description: 'Purpose', details: structured('My unsaved steps')};
  }
  function button(label: string): HTMLButtonElement | undefined {
    return Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).find(b => b.textContent?.trim() === label);
  }
  it('filters published tiles through the hierarchy, clears descendants, and orders numeric codes', fakeAsync(() => {
    start();
    component.hierarchy=[{nodeKey:'org:1',parentKey:null,name:'Organization',code:'01'},
      {nodeKey:'group:10',parentKey:'org:1',name:'Ten',code:'10'},
      {nodeKey:'group:2',parentKey:'org:1',name:'Two',code:'2'},
      {nodeKey:'process:10',parentKey:'group:10',name:'Process ten',code:'001'},
      {nodeKey:'process:2',parentKey:'group:2',name:'Process two',code:'001'}];
    component.documents=[10,2].map(id=>({documentId:id,revisionId:900+id,nodeKey:`process:${id}`,publishedVersion:3,kind:'CLIENT_SOP',title:'Initial Setup',description:'Purpose',details:''}));
    fixture.detectChanges();tick();
    const titles=()=>Array.from(fixture.nativeElement.querySelectorAll('.document-card strong') as NodeListOf<HTMLElement>).map(n=>n.textContent);
    expect(titles()[0]).toBe('01-2-001 · Two | Process two | Initial Setup · v3');
    component.chooseLibraryLevel(0,'org:1');component.chooseLibraryLevel(1,'group:10');fixture.detectChanges();
    expect(titles().length).toBe(1);expect(titles()[0]).toContain('01-10-001');
    const org=fixture.nativeElement.querySelector('.library-filters select') as HTMLSelectElement;
    org.value='';org.dispatchEvent(new Event('change'));tick();fixture.detectChanges();
    expect(component.librarySelection).toEqual([]);expect(titles().length).toBe(2);
    expect(component.libraryOptions(2)).toEqual([]);
  }));
  it('limits the published tile region to four rows and scrolls additional tiles', fakeAsync(() => {
    start();
    component.documents=Array.from({length:30},(_,id)=>({documentId:id,revisionId:id,publishedVersion:1,kind:'CLIENT_SOP',title:'Long procedure title '.repeat(15),description:'Purpose '.repeat(40),details:''}));
    fixture.detectChanges();
    const region=fixture.nativeElement.querySelector('.published-cards') as HTMLElement;
    region.style.width='600px';
    const style=getComputedStyle(region);const card=region.querySelector('button') as HTMLElement;
    expect(style.overflowY).toBe('auto');
    expect(parseFloat(style.maxHeight)).toBeCloseTo(4*card.getBoundingClientRect().height+3*parseFloat(style.rowGap),0);
    expect(region.scrollHeight).toBeGreaterThan(region.clientHeight);
    expect(card.scrollWidth).toBeLessThanOrEqual(card.clientWidth+1);
  }));
  it('filters published tiles through the hierarchy, clears descendants, and orders numeric codes', fakeAsync(() => {
    start();
    component.hierarchy=[{nodeKey:'org:1',parentKey:null,name:'Organization',code:'01'},
      {nodeKey:'group:10',parentKey:'org:1',name:'Ten',code:'10'},
      {nodeKey:'group:2',parentKey:'org:1',name:'Two',code:'2'},
      {nodeKey:'process:10',parentKey:'group:10',name:'Process ten',code:'001'},
      {nodeKey:'process:2',parentKey:'group:2',name:'Process two',code:'001'}];
    component.documents=[10,2].map(id=>({documentId:id,revisionId:900+id,nodeKey:`process:${id}`,publishedVersion:3,kind:'CLIENT_SOP',title:'Initial Setup',description:'Purpose',details:''}));
    fixture.detectChanges();tick();
    const titles=()=>Array.from(fixture.nativeElement.querySelectorAll('.document-card strong') as NodeListOf<HTMLElement>).map(n=>n.textContent);
    expect(titles()[0]).toBe('01-2-001 · Two | Process two | Initial Setup · v3');
    component.chooseLibraryLevel(0,'org:1');component.chooseLibraryLevel(1,'group:10');fixture.detectChanges();
    expect(titles().length).toBe(1);expect(titles()[0]).toContain('01-10-001');
    const org=fixture.nativeElement.querySelector('.library-filters select') as HTMLSelectElement;
    org.value='';org.dispatchEvent(new Event('change'));tick();fixture.detectChanges();
    expect(component.librarySelection).toEqual([]);expect(titles().length).toBe(2);
    expect(component.libraryOptions(2)).toEqual([]);
  }));
  it('limits the published tile region to four rows and scrolls additional tiles', fakeAsync(() => {
    start();
    component.documents=Array.from({length:30},(_,id)=>({documentId:id,revisionId:id,publishedVersion:1,kind:'CLIENT_SOP',title:'Long procedure title '.repeat(15),description:'Purpose '.repeat(40),details:''}));
    fixture.detectChanges();
    const region=fixture.nativeElement.querySelector('.published-cards') as HTMLElement;
    region.style.width='600px';
    const style=getComputedStyle(region);const card=region.querySelector('button') as HTMLElement;
    expect(style.overflowY).toBe('auto');
    expect(parseFloat(style.maxHeight)).toBeCloseTo(4*card.getBoundingClientRect().height+3*parseFloat(style.rowGap),0);
    expect(region.scrollHeight).toBeGreaterThan(region.clientHeight);
    expect(card.scrollWidth).toBeLessThanOrEqual(card.clientWidth+1);
  }));
  it('labels both comparison menus with hierarchy, title, and publication version instead of revision IDs', fakeAsync(() => {
    start();
    component.hierarchy = [{nodeKey:'family:5',parentKey:null,name:'Governance',code:'01'},
      {nodeKey:'process:7',parentKey:'family:5',name:'Admin How Tos',code:'002'}];
    void component.openDocument({documentId:10,revisionId:902,processId:7,nodeKey:'process:7',kind:'CLIENT_SOP',title:'Initial Setup',description:'Purpose',details:'Latest'});
    http.expectOne(component.api+'/documents/10/history').flush([
      {id:902,title:'Initial Setup',description:'Purpose',details:'Latest'},
      {id:57,title:'Initial Setup',description:'Purpose',details:'Earlier'}]);
    tick();fixture.detectChanges();
    const menus=fixture.nativeElement.querySelectorAll('.comparison select') as NodeListOf<HTMLSelectElement>;
    expect(menus.length).toBe(2);
    menus.forEach(menu=>{
      expect(menu.textContent).toContain('01-002 · Governance | Admin How Tos | Initial Setup · v2');
      expect(menu.textContent).toContain('Initial Setup · v1');
      expect(menu.textContent).not.toContain('Revision #');
      expect(menu.textContent).not.toContain('902');
    });
  }));
  it('identifies unpublished candidates separately from published versions', fakeAsync(() => {
    start();request();component.history=[component.request!.candidate!,{id:19,title:'Earlier candidate',description:'',details:''}];
    expect(component.snapshotLabel(component.history[0])).toContain('Candidate 2 — Current candidate');
    expect(component.snapshotLabel(component.history[1])).toContain('Candidate 1');
    expect(component.snapshotLabel(component.history[0])).not.toContain('v2');
  }));
  it('offers self-approval only when the server grants it and a reason is present', fakeAsync(() => {
    start(); request([]); fixture.detectChanges(); expect(button('Self-approve & publish')).toBeUndefined();
    component.request!.actions = ['SELF_APPROVE']; fixture.detectChanges(); expect(button('Self-approve & publish')!.disabled).toBeTrue();
    component.reason = 'Owner verified the emergency change'; fixture.detectChanges(); expect(button('Self-approve & publish')!.disabled).toBeFalse();
  }));
  it('blocks approval while an older candidate is selected for comparison', fakeAsync(() => {
    start(); request(['APPROVE']); fixture.detectChanges(); expect(button('Approve & publish')!.disabled).toBeFalse();
    component.right = {id: 19, title: 'Older', description: 'Purpose', details: 'Old steps'};
    fixture.detectChanges(); expect(button('Approve & publish')!.disabled).toBeTrue();
  }));
  it('keeps unsaved text after a stale-save conflict and sends both versions', fakeAsync(() => {
    start(); request(); component.dirty = true; void component.save();
    const save = http.expectOne(component.api + '/requests/9/copies/7');
    expect(save.request.method).toBe('PUT'); expect(save.request.body.requestVersion).toBe(3); expect(save.request.body.copyVersion).toBe(4);
    save.flush({}, {status: 409, statusText: 'Conflict'}); tick();
    expect(component.copy!.details).toBe(structured('My unsaved steps')); expect(component.dirty).toBeTrue(); expect(component.error).toContain('reopen');
  }));
  it('reuses a command ID when retrying an uncertain save', fakeAsync(() => {
    start(); request(); component.dirty = true; void component.save();
    const first = http.expectOne(component.api + '/requests/9/copies/7'); const id = first.request.body.commandId;
    first.error(new ProgressEvent('error')); tick(); void component.save();
    const retry = http.expectOne(component.api + '/requests/9/copies/7'); expect(retry.request.body.commandId).toBe(id);
    retry.flush({requestId: 9, copyId: 7, version: 5}); tick(); lists();
    expect(component.copy!.version).toBe(5); expect(component.dirty).toBeFalse();
  }));
  it('requires saving content and a reason before replacing a candidate', fakeAsync(() => {
    start(); request(['SUGGEST_EDITS']); component.dirty = true; component.reason = 'Corrected a step'; fixture.detectChanges();
    expect(button('Submit revised candidate')!.disabled).toBeTrue();
    component.dirty = false; component.reason = ''; fixture.detectChanges(); expect(button('Submit revised candidate')!.disabled).toBeTrue();
    component.reason = 'Corrected a step'; fixture.detectChanges(); expect(button('Submit revised candidate')!.disabled).toBeFalse();
  }));
  it('protects unsaved work when navigating away', fakeAsync(() => {
    start(); component.dirty = true; spyOn(window, 'confirm').and.returnValue(false);
    expect(component.canLeave()).toBeFalse(); component.dirty = false; expect(component.canLeave()).toBeTrue();
  }));
  it('reports a successful submission when recalculated routing ends the editors access', fakeAsync(() => {
    start(); request(['SUGGEST_EDITS']); component.reason = 'Corrected the step'; void component.submit();
    http.expectOne(component.api + '/requests/9/submit').flush({candidateId: 21}); tick();
    http.expectOne(component.api + '/requests/9').flush({}, {status: 403, statusText: 'Forbidden'}); tick(); lists();
    expect(component.message).toContain('Candidate submitted'); expect(component.message).toContain('assignment has ended');
    expect(component.request).toBeUndefined(); expect(component.error).toBe('');
  }));
});
