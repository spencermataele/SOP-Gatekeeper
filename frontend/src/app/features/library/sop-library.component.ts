import {formattedSopTitle} from '../workflows/sop-title';
import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {forkJoin, firstValueFrom} from 'rxjs';
import {environment} from '../../../environments/environment';
import {SopContentComponent} from '../workflows/sop-content.component';
import {parseTemplate} from '../workflows/sop-template';

export interface LibraryNode {nodeKey:string;parentKey:string|null;name:string;kind:string;code:string|null;}
export interface LibraryDocument {documentId:number;nodeKey:string;title:string;description:string;details:string;publishedVersion:number;revisionId:number;kind:string;sourceLabel?:string;}
@Component({selector:'app-sop-library',standalone:true,imports:[CommonModule,FormsModule,SopContentComponent],templateUrl:'./sop-library.component.html',styleUrls:['./sop-library.component.css']})
export class SopLibraryComponent implements OnInit {
  @ViewChild('reader') reader?:ElementRef<HTMLElement>;
  nodes:LibraryNode[]=[]; documents:LibraryDocument[]=[]; selected:LibraryDocument|undefined;
  branch=''; search=''; error=''; loading=true; historyBusy=false; showHistory=false;
  history:any[]=[]; older:any; newer:any;
  private api=environment.apiBaseUrl+'/api/lifecycle';
  constructor(private http:HttpClient){}
  ngOnInit():void{void this.load();}
  async load():Promise<void>{this.loading=true;this.error='';try{
    const data=await firstValueFrom(forkJoin({nodes:this.http.get<LibraryNode[]>(this.api+'/library/hierarchy'),documents:this.http.get<LibraryDocument[]>(this.api+'/documents')}));
    this.nodes=data.nodes;this.documents=data.documents;
  }catch{this.error='The SOP library could not be loaded. Please retry.';}finally{this.loading=false;}}
  path(key:string):LibraryNode[]{const result:LibraryNode[]=[];const visited=new Set<string>();let node=this.nodes.find(n=>n.nodeKey===key);while(node&&!visited.has(node.nodeKey)){visited.add(node.nodeKey);result.unshift(node);node=this.nodes.find(n=>n.nodeKey===node!.parentKey);}return result;}
  get breadcrumbs():LibraryNode[]{return this.path(this.branch);}
  get children():LibraryNode[]{return this.nodes.filter(n=>(n.parentKey||'')===this.branch);}
  within(doc:LibraryDocument,key:string):boolean{
    if(!key)return true;
    return this.path(doc.nodeKey).some(n=>n.nodeKey===key);
  }
  get filtered():LibraryDocument[]{const q=this.search.trim().toLocaleLowerCase();return this.documents.filter(d=>this.within(d,this.branch)&&(!q||(this.label(d)+' '+this.location(d)+' '+d.description).toLocaleLowerCase().includes(q))).sort((a,b)=>this.label(a).localeCompare(this.label(b)));}
  location(doc:LibraryDocument):string{return this.path(doc.nodeKey).map(n=>n.name).join(' / ');}
  label(doc:LibraryDocument):string{return formattedSopTitle(this.nodes,doc.nodeKey,doc.title,`v${doc.publishedVersion}`);}
  choose(key:string):void{this.branch=key;this.selected=undefined;this.showHistory=false;}
  open(doc:LibraryDocument):void{this.selected=doc;this.showHistory=false;this.history=[];this.error='';setTimeout(()=>{this.reader?.nativeElement.focus();this.reader?.nativeElement.scrollIntoView({block:'start',behavior:'smooth'});});}
  async compare():Promise<void>{if(!this.selected||this.historyBusy)return;const id=this.selected.documentId;this.historyBusy=true;this.error='';try{
    const rows=await firstValueFrom(this.http.get<any[]>(`${this.api}/documents/${id}/history`));
    if(this.selected?.documentId!==id)return;
    this.history=rows.map((r,index)=>({...r,publishedVersion:rows.length-index}));
    this.newer=this.history.find(r=>r.id===this.selected!.revisionId)||this.history[0];this.older=this.history.find(r=>r.id!==this.newer?.id);this.showHistory=true;
  }catch{this.error='Version history could not be loaded. Please retry.';}finally{this.historyBusy=false;}}
}
