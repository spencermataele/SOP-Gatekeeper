import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {firstValueFrom} from 'rxjs';
import {environment} from '../../../environments/environment';
import {LibraryNode} from '../library/sop-library.component';

@Component({selector:'app-hierarchy-codes',standalone:true,imports:[CommonModule,FormsModule],template:`
<section><h2>Library hierarchy codes</h2>
<p>Choose each layer to narrow the next dropdown. Stop at any level to edit its code. Codes stay stable when names change and must be unique among siblings.</p>
<p>Options are ordered by code number. Entries that need a code appear last, alphabetically.</p>
<p role="alert" *ngIf="error">{{error}}</p><p role="status">{{message}}</p>
<form (ngSubmit)="save()">
 <fieldset [disabled]="busy"><legend>Find a hierarchy entry</legend>
  <label *ngFor="let level of levels;let i=index">{{level.label}}
   <select [name]="'codeLevel'+i" [ngModel]="selection[i] || ''" (ngModelChange)="choose(i,$event)" [disabled]="i>0 && !selection[i-1]">
    <option value="">Choose {{level.label.toLowerCase()}}</option>
    <option *ngFor="let node of options(i)" [value]="node.nodeKey">{{node.code || 'Needs code'}} · {{node.name}}</option>
   </select>
   <small *ngIf="!busy && !error && (i===0 || selection[i-1]) && !options(i).length">No entries under this parent.</small>
  </label>
 </fieldset>
 <div *ngIf="selectedNode as node" class="code-editor">
  <p><strong>Editing code for:</strong> {{path(node)}}</p>
  <label>Code<input name="displayCode" [(ngModel)]="code" maxlength="24" pattern="[A-Za-z0-9]+" required [disabled]="busy"></label>
  <label>Reason<input name="codeReason" [(ngModel)]="reason" maxlength="2000" required [disabled]="busy"></label>
  <button [disabled]="busy || !code.trim() || !reason.trim()">Save code</button>
 </div>
 <button type="button" (click)="load()" [disabled]="busy">Refresh codes</button>
</form></section>`,styles:[`section{background:white;border:1px solid #d2dfe4;border-radius:8px;padding:24px;margin:24px 0;min-width:0}fieldset{border:1px solid #d2dfe4;border-radius:6px;padding:16px;min-width:0}label{display:block;margin:16px 0}input,select{display:block;width:100%;min-width:0;box-sizing:border-box;padding:12px;margin-top:8px;font:inherit}button{padding:12px;margin-right:8px;height:auto;white-space:normal}p{overflow-wrap:anywhere}.code-editor{margin:20px 0}`]})
export class HierarchyCodesComponent implements OnInit {
 nodes:LibraryNode[]=[];key='';selection:string[]=[];code='';reason='';error='';message='';busy=false;
 readonly levels=[{key:'org',label:'Organization'},{key:'group',label:'Organization group'},{key:'department',label:'Department'},{key:'subgroup',label:'Subdepartment'},{key:'family',label:'Process family'},{key:'process',label:'Process'}];
 private readonly codeOrder=new Intl.Collator('en',{numeric:true,sensitivity:'base'});
 constructor(private http:HttpClient){}
 ngOnInit():void{void this.load();}
 get selectedNode():LibraryNode|undefined{return this.nodes.find(n=>n.nodeKey===this.key);}
 options(index:number):LibraryNode[]{
  if(index>0&&!this.selection[index-1])return [];
  const parent=index===0?'':this.selection[index-1];
  return this.nodes.filter(n=>n.nodeKey.startsWith(this.levels[index].key+':')&&(n.parentKey||'')===parent).sort((a,b)=>{
   if(!!a.code!==!!b.code)return a.code?-1:1;
   return (a.code&&b.code?this.codeOrder.compare(a.code,b.code):0)||a.name.localeCompare(b.name,'en',{sensitivity:'base'})||a.nodeKey.localeCompare(b.nodeKey);
  });
 }
 choose(index:number,key:string):void{
  this.selection=this.selection.slice(0,index);if(key)this.selection[index]=key;
  this.key=this.selection[this.selection.length-1]||'';this.select();this.message='';this.error='';
 }
 path(node:LibraryNode):string{const names=[node.name];let parent=this.nodes.find(n=>n.nodeKey===node.parentKey);const seen=new Set([node.nodeKey]);while(parent&&!seen.has(parent.nodeKey)){seen.add(parent.nodeKey);names.unshift(parent.name);parent=this.nodes.find(n=>n.nodeKey===parent!.parentKey);}return names.join(' / ');}
 select():void{this.code=this.selectedNode?.code||'';this.reason='';}
 private restore():void{
  this.selection=[];let node=this.selectedNode;const seen=new Set<string>();
  if(!node)this.key='';
  while(node&&!seen.has(node.nodeKey)){seen.add(node.nodeKey);const index=this.levels.findIndex(l=>node!.nodeKey.startsWith(l.key+':'));if(index>=0)this.selection[index]=node.nodeKey;node=this.nodes.find(n=>n.nodeKey===node!.parentKey);}
  this.select();
 }
 async load():Promise<void>{this.busy=true;this.error='';try{this.nodes=await firstValueFrom(this.http.get<LibraryNode[]>(environment.apiBaseUrl+'/api/lifecycle/library/hierarchy'));this.restore();}catch{this.error='Could not load hierarchy codes.';}finally{this.busy=false;}}
 async save():Promise<void>{const node=this.selectedNode;if(this.busy||!node||!this.code.trim()||!this.reason.trim())return;this.busy=true;this.error='';this.message='';try{await firstValueFrom(this.http.put(environment.apiBaseUrl+'/api/lifecycle/admin/hierarchy/'+encodeURIComponent(node.nodeKey)+'/code',{code:this.code,expectedCode:node.code,reason:this.reason}));node.code=this.code;this.reason='';this.message='Code saved and audit recorded. Refresh Workflow setup to see the new activity.';}catch(e:any){this.error=e.error?.message||'Code could not be saved. Refresh and try again.';}finally{this.busy=false;}}
}
