import {Component,OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {ActivatedRoute} from '@angular/router';
import {firstValueFrom} from 'rxjs';
import {environment} from '../../../environments/environment';
interface Ticket {system:string;number:string;url:string;}
@Component({selector:'app-change-tracking',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./change-tracking.component.html',styleUrls:['./change-tracking.component.css']})
export class ChangeTrackingComponent implements OnInit {
 readonly api=environment.apiBaseUrl+'/api/lifecycle/changes';
 items:any[]=[];selected:any;busy=false;error='';message='';filter='ACTIVE';
 title='';ownerId=0;state='PLANNED';plan='';evidence='';plannedStart='';plannedFinish='';reason='';tickets:Ticket[]=[];suggestionIds:string[]=[];
 readonly states=['PLANNED','IN_PROGRESS','READY_FOR_VALIDATION','IMPLEMENTED','EFFECTIVENESS_VERIFIED','BLOCKED','CANCELLED'];
 private commands=new Map<string,string>();
 constructor(private http:HttpClient,private route:ActivatedRoute){}
 ngOnInit():void{void this.load();}
 label(s:string):string{return s?.split('_').join(' ').toLowerCase()||'';}
 get visible():any[]{return this.items.filter(i=>this.filter==='ALL'||(this.filter==='ACTIVE'?!['EFFECTIVENESS_VERIFIED','CANCELLED'].includes(i.state):i.state===this.filter));}
 async load():Promise<void>{this.busy=true;this.error='';try{this.items=await firstValueFrom(this.http.get<any[]>(this.api));const id=this.route.snapshot.queryParamMap.get('change');if(id)this.setSelected(await firstValueFrom(this.http.get(this.api+'/'+encodeURIComponent(id))));}catch(e:any){this.error=e.error?.message||'Could not load changes.';}finally{this.busy=false;}}
 async open(id:string):Promise<void>{this.busy=true;this.error='';try{this.setSelected(await firstValueFrom(this.http.get(this.api+'/'+id)));}catch(e:any){this.error=e.error?.message||'Could not load change.';}finally{this.busy=false;}}
 private setSelected(s:any):void{this.selected=s;this.title=s.title;this.ownerId=s.implementation_owner_id;this.state=s.state;this.plan=s.plan||'';this.evidence=s.evidence||'';this.plannedStart=s.planned_start||'';this.plannedFinish=s.planned_finish||'';this.reason='';this.tickets=(s.tickets||[]).map((t:Ticket)=>({...t}));this.suggestionIds=(s.suggestions||[]).map((v:any)=>v.id);}
 toggleSuggestion(id:string,checked:boolean):void{this.suggestionIds=checked?[...this.suggestionIds,id]:this.suggestionIds.filter(s=>s!==id);}
 async save():Promise<void>{
  if(this.busy||!this.selected?.canEdit||!this.reason.trim())return;
  const body={version:this.selected.lock_version,title:this.title,ownerId:this.ownerId,state:this.state,plan:this.plan,evidence:this.evidence,plannedStart:this.plannedStart||null,plannedFinish:this.plannedFinish||null,reason:this.reason,tickets:this.tickets,suggestionIds:this.suggestionIds};
  const key=JSON.stringify(body);if(!this.commands.has(key))this.commands.set(key,crypto.randomUUID());
  this.busy=true;this.error='';this.message='';
  try{const saved=await firstValueFrom(this.http.put(this.api+'/'+this.selected.change_id,{...body,commandId:this.commands.get(key)}));this.setSelected(saved);this.commands.delete(key);this.message='Change saved. Linked suggestions and SOP approval remain separate.';try{this.items=await firstValueFrom(this.http.get<any[]>(this.api));}catch{this.message+=' Refresh the list to see the latest summary.';}}
  catch(e:any){this.error=e.error?.message||'Could not save. Your entries are retained.';}finally{this.busy=false;}
 }
 safeUrl(url:string):string|null{try{const value=new URL(url);return ['https:','http:'].includes(value.protocol)&&!value.username&&!value.password?value.href:null;}catch{return null;}}
}
