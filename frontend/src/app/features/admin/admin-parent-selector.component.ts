import {Component,forwardRef,Input,OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule,ControlValueAccessor,NG_VALUE_ACCESSOR} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
interface HierarchyEntry {nodeKey:string;parentKey:string|null;name:string;kind:string;}
@Component({selector:'app-admin-parent-selector',standalone:true,imports:[CommonModule,FormsModule],providers:[{provide:NG_VALUE_ACCESSOR,useExisting:forwardRef(()=>AdminParentSelectorComponent),multi:true}],template:`
<fieldset [disabled]="disabled || loading"><legend>Choose the parent hierarchy</legend>
 <p role="alert" *ngIf="error">{{error}} <button type="button" (click)="load()">Retry</button></p>
 <label *ngFor="let level of levels;let i=index">{{level.label}}
  <select [ngModel]="selection[i] || ''" [ngModelOptions]="{standalone:true}" (ngModelChange)="choose(i,$event)" (blur)="touched()" [disabled]="i>0 && !selection[i-1]">
   <option value="">Choose {{level.label.toLowerCase()}}</option><option *ngFor="let node of options(i)" [value]="node.nodeKey">{{node.name}}</option>
  </select>
  <small *ngIf="(i===0 || selection[i-1]) && !loading && !error && !options(i).length">No entries at this level. Create the parent hierarchy first.</small>
 </label>
</fieldset>`,styles:[`fieldset{border:1px solid #c9d9dc;padding:16px;border-radius:8px;min-width:0}label{display:block;margin:12px 0}select{display:block;width:100%;box-sizing:border-box;padding:10px;font:inherit;margin-top:6px}small{display:block}legend{font-weight:bold}`]})
export class AdminParentSelectorComponent implements OnInit,ControlValueAccessor {
 @Input() target:'org'|'group'|'department'|'subgroup'|'family'|'process'='department';
 nodes:HierarchyEntry[]=[];selection:string[]=[];disabled=false;loading=false;error='';private value:number|null=null;
 private allLevels=[{key:'org',label:'Organization'},{key:'group',label:'Organization group'},{key:'department',label:'Department'},{key:'subgroup',label:'Subdepartment'},{key:'family',label:'Process family'},{key:'process',label:'Process'}];
 get levels(){return this.allLevels.slice(0,this.allLevels.findIndex(l=>l.key===this.target)+1);}
 changed:(value:number|null)=>void=()=>{};touched:()=>void=()=>{};
 constructor(private http:HttpClient){}
 ngOnInit():void{this.load();}
 load():void{this.loading=true;this.error='';this.http.get<HierarchyEntry[]>(environment.apiBaseUrl+'/admin/hierarchy').subscribe({next:nodes=>{this.nodes=nodes;this.loading=false;this.restore();},error:()=>{this.loading=false;this.error='Hierarchy could not be loaded.';}});}
 options(i:number):HierarchyEntry[]{return this.nodes.filter(n=>n.nodeKey.startsWith(this.levels[i].key+':')&&(n.parentKey||'')===(i===0?'':this.selection[i-1]));}
 choose(index:number,key:string):void{this.selection=this.selection.slice(0,index);this.selection[index]=key;this.value=index===this.levels.length-1&&key?Number(key.split(':')[1]):null;this.changed(this.value);this.touched();}
 writeValue(value:number|null):void{this.value=value;this.restore();}
 private restore():void{this.selection=[];if(this.value==null)return;let node=this.nodes.find(n=>n.nodeKey===this.target+':'+this.value);const seen=new Set<string>();while(node&&!seen.has(node.nodeKey)){seen.add(node.nodeKey);const index=this.levels.findIndex(l=>node!.nodeKey.startsWith(l.key+':'));if(index>=0)this.selection[index]=node.nodeKey;node=this.nodes.find(n=>n.nodeKey===node!.parentKey);}}
 registerOnChange(fn:(value:number|null)=>void):void{this.changed=fn;}
 registerOnTouched(fn:()=>void):void{this.touched=fn;}
 setDisabledState(disabled:boolean):void{this.disabled=disabled;}
}
