import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {ActivatedRoute} from '@angular/router';
import {firstValueFrom,forkJoin} from 'rxjs';
import {environment} from '../../../environments/environment';
import {GovernedProcess,ProcessSelectorComponent} from '../workflows/process-selector.component';

@Component({selector:'app-suggestions',standalone:true,imports:[CommonModule,FormsModule,ProcessSelectorComponent],
 templateUrl:'./suggestions.component.html',styleUrls:['./suggestions.component.css']})
export class SuggestionsComponent implements OnInit {
 readonly api=environment.apiBaseUrl+'/api/lifecycle';
 processes:GovernedProcess[]=[];documents:any[]=[];items:any[]=[];selected:any;
 busy=false;error='';message='';creating=false;queue='all';
 processId=0;documentId:number|null=null;title='';problem='';proposal='';benefit='';costEstimate='';roiEstimate='';
 response='';concern='UNDETERMINED';declineReason='';plan='';evidence='';reviewDate='';
 readonly concerns=['UNDETERMINED','NONE','TRAINING','DEFECT','BOTH'];
 readonly declines=[['EXISTING_STANDARD','Existing standard addresses the concern'],['ALTERNATIVE_SELECTED','Alternative solution selected'],['DUPLICATE','Duplicate improvement'],['INSUFFICIENT_EVIDENCE','Insufficient evidence'],['IMPLEMENTATION_CONSTRAINTS','Constraints prevent implementation']];
 private commands=new Map<string,string>();
 constructor(private http:HttpClient,private route:ActivatedRoute){}
 ngOnInit():void{void this.load();}
 label(value:string):string{return value==='NONE'?'N/A':value?.split('_').join(' ').toLowerCase()||'';}
 ticketUrl(url:string):string|null{try{const parsed=new URL(url);return ['http:','https:'].includes(parsed.protocol)&&!parsed.username&&!parsed.password?parsed.href:null;}catch{return null;}}
 get relatedDocuments():any[]{return this.documents.filter(d=>d.processId===this.processId);}
 get visible():any[]{return this.items.filter(s=>this.queue==='all'||(this.queue==='defects'?['DEFECT','BOTH'].includes(s.concern)&&s.state!=='CLOSED':this.queue==='alignment'?s.state==='ALIGNMENT_UNRESOLVED':this.queue==='closed'?s.state==='CLOSED':!['DEFECT','BOTH'].includes(s.concern)&&s.state!=='CLOSED'));}
 processChanged(id:number):void{this.processId=id;this.documentId=null;}
 async load():Promise<void>{
  this.busy=true;this.error='';
  try{const data=await firstValueFrom(forkJoin({processes:this.http.get<GovernedProcess[]>(this.api+'/processes'),documents:this.http.get<any[]>(this.api+'/documents'),items:this.http.get<any[]>(this.api+'/suggestions')}));Object.assign(this,data);
   const params=this.route.snapshot.queryParamMap;const process=Number(params.get('process'));const document=Number(params.get('document'));
   if(process&&this.processes.some(p=>p.id===process)){this.processId=process;this.documentId=this.relatedDocuments.some(d=>d.documentId===document)?document:null;this.creating=true;}
   const suggestion=params.get('suggestion');if(suggestion)this.setSelected(await firstValueFrom(this.http.get(this.api+'/suggestions/'+encodeURIComponent(suggestion))));
  }catch(e:any){this.error=e.error?.message||'Could not load suggestions.';}finally{this.busy=false;}
 }
 async open(id:string):Promise<void>{this.busy=true;this.error='';try{this.setSelected(await firstValueFrom(this.http.get(this.api+'/suggestions/'+id)));}catch(e:any){this.error=e.error?.message||'Could not open suggestion.';}finally{this.busy=false;}}
 private setSelected(value:any):void{this.selected=value;this.response='';this.concern=value.concern;this.declineReason=value.decline_reason||'';this.plan=value.action_plan||'';this.evidence=value.evidence||'';this.reviewDate=value.review_date||'';}
 private command(body:any):any{const key=JSON.stringify(body);if(!this.commands.has(key))this.commands.set(key,crypto.randomUUID());return {...body,commandId:this.commands.get(key)};}
 async submit():Promise<void>{
  if(this.busy||!this.processId||![this.title,this.problem,this.proposal,this.benefit].every(s=>s.trim()))return;
  await this.send('/suggestions',{processId:this.processId,documentId:this.documentId,title:this.title,problem:this.problem,proposal:this.proposal,benefit:this.benefit,costEstimate:this.costEstimate,roiEstimate:this.roiEstimate});
  if(!this.error){this.creating=false;this.title=this.problem=this.proposal=this.benefit=this.costEstimate=this.roiEstimate='';}
 }
 async act(action:string):Promise<void>{if(this.busy||!this.response.trim())return;await this.send('/suggestions/'+this.selected.suggestion_id+'/actions',{version:this.selected.lock_version,action,message:this.response,concern:this.concern,declineReason:this.declineReason,plan:this.plan,evidence:this.evidence,reviewDate:this.reviewDate||null});}
 private async send(path:string,body:any):Promise<void>{this.busy=true;this.error='';this.message='';try{const saved=await firstValueFrom(this.http.post(this.api+path,this.command(body)));this.setSelected(saved);this.message='Saved. Communication is recorded and available to participants in the app.';this.items=await firstValueFrom(this.http.get<any[]>(this.api+'/suggestions'));}catch(e:any){this.error=e.error?.message||'Could not save. Your entries are retained; refresh if the record changed.';}finally{this.busy=false;}}
 get reviewable():boolean{return this.selected?.canReview&&['SUBMITTED','NEEDS_CLARIFICATION','DECLINED_AWAITING_RESPONSE','ALIGNMENT_UNRESOLVED'].includes(this.selected.state);}
 get actionable():boolean{return this.selected?.canReview&&['ACCEPTED','FOLLOW_UP_REQUIRED','AWAITING_AGREEMENT'].includes(this.selected.state);}
 recipients(id:string):string{return (this.selected?.recipients||[]).filter((r:any)=>r.communication_id===id).map((r:any)=>r.recipient+' ('+this.label(r.delivery_status)+')').join(', ');}
 communicationDetails(message:any):{label:string;value:string}[]{
  let details:any;try{details=typeof message.details==='string'?JSON.parse(message.details):message.details;}catch{return [];}
  return Object.entries({title:'Title',problem:'Problem',proposal:'Proposed change',benefit:'Expected benefit',costEstimate:'Cost estimate',roiEstimate:'ROI estimate',concern:'Concern assessment',declineReason:'Decline reason',plan:'Action or training plan',evidence:'Evidence',reviewDate:'Effectiveness review date',requestId:'Linked change request'})
   .filter(([key])=>details?.[key]!==undefined&&details[key]!==null&&details[key]!=='')
   .map(([key,label])=>({label,value:['concern','declineReason'].includes(key)?this.label(details[key]):String(details[key])}));
 }
}
