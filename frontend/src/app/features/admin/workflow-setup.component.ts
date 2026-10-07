import {AdminParentSelectorComponent} from './admin-parent-selector.component';
import {HierarchyCodesComponent} from './hierarchy-codes.component';
import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {firstValueFrom} from 'rxjs';
import {environment} from '../../../environments/environment';

interface Setup {
  clientOrgId: number|null;
  organizations: {id:number;name:string}[];
  users: {id:number;name:string;username:string;managerId:number|null}[];
  processes: {id:number;name:string;groupName:string;departmentName:string;familyName:string;ownerId:number|null;managerId:number|null;version:number|null}[];
  activity: {id:number;action:string;processId:number|null;actor:string;reason:string;previousValue:string;newValue:string;recordedAt:string}[];
}
@Component({selector:'app-workflow-setup',standalone:true,imports:[CommonModule,FormsModule,RouterModule,HierarchyCodesComponent,AdminParentSelectorComponent],template:`
  <section class="setup"><a routerLink="/admin">← Admin</a><h1>Workflow setup</h1>
    <p>Build the organization hierarchy in Admin, then assign ownership here. Configured processes appear in the SOP template's hierarchy selectors.</p>
    <p role="alert" class="error" *ngIf="error">{{error}}</p><p role="status" *ngIf="message">{{message}}</p>
    <button type="button" (click)="load()" [disabled]="busy">Refresh setup</button>
    <ng-container *ngIf="data">
      <section class="panel"><h2>Deployment organization</h2>
        <p *ngIf="data.clientOrgId">{{organizationName}} · This deployment is assigned to this organization.</p>
        <form *ngIf="!data.clientOrgId" (ngSubmit)="configure()"><label>Organization<select name="org" [(ngModel)]="orgId" [disabled]="busy"><option [ngValue]="0">Choose an organization</option><option *ngFor="let org of data.organizations" [ngValue]="org.id">{{org.name}}</option></select></label><label>Setup reason<input name="clientReason" [(ngModel)]="clientReason" maxlength="2000" [disabled]="busy"></label><button [disabled]="busy || !orgId || !clientReason.trim()">Configure organization</button></form>
      </section>
      <section class="panel" *ngIf="data.clientOrgId"><h2>Process ownership and reporting line</h2>
        <p *ngIf="!data.processes.length">Create an organization group, department, process family, and business process using the Admin screens first.</p>
        <form (ngSubmit)="save()"><app-admin-parent-selector target="process" name="process" [(ngModel)]="processId" (ngModelChange)="selectProcess()" [disabled]="busy"></app-admin-parent-selector>
          <ng-container *ngIf="processId"><label>Process owner<select name="owner" [(ngModel)]="ownerId" (ngModelChange)="selectOwner()" [disabled]="busy"><option [ngValue]="0">Choose an account</option><option *ngFor="let user of data.users" [ngValue]="user.id">{{user.name}} ({{user.username}})</option></select></label>
            <label>Owner's direct manager (optional)<select name="manager" [(ngModel)]="managerId" [disabled]="busy"><option [ngValue]="null">No manager assigned</option><ng-container *ngFor="let user of data.users"><option *ngIf="user.id !== ownerId" [ngValue]="user.id">{{user.name}} ({{user.username}})</option></ng-container></select></label>
            <p>The reporting line belongs to the owner account and applies to every process they own. A change invalidates affected pending review assignments; an administrator must recalculate those assignments in the SOP workspace. With no manager, eligible administrators can review the owner's submission.</p>
            <label>Reason for assignment or change<textarea name="reason" [(ngModel)]="reason" maxlength="2000" [disabled]="busy"></textarea></label><button [disabled]="busy || !ownerId || !reason.trim()">Save ownership</button>
          </ng-container>
        </form>
      </section>
      <app-hierarchy-codes></app-hierarchy-codes><details class="workflow-log"><summary>Configuration log ({{data.activity.length}})</summary><div class="log-entries" tabindex="0" aria-label="Configuration activity log"><p *ngIf="!data.activity.length">No configuration changes recorded through this screen yet.</p><article *ngFor="let event of data.activity"><strong>{{event.action.split('_').join(' ')}}</strong><p>{{event.actor}} · {{event.recordedAt | date:'medium'}}<span *ngIf="event.processId"> · Process #{{event.processId}}</span></p><p>{{event.reason}}</p><details><summary>Recorded assignment values</summary><p>Before: {{event.previousValue || 'Not configured'}}</p><p>After: {{event.newValue}}</p></details></article></div></details>
    </ng-container>
  </section>`,styles:[`.setup{max-width:1000px;margin:auto}p{line-height:1.6}.panel{background:white;border:1px solid #d2dfe4;border-radius:8px;padding:24px;margin:24px 0}label{display:block;font-weight:bold;margin:16px 0}input,select,textarea{display:block;width:100%;box-sizing:border-box;padding:12px;border:1px solid #9fb3bd;border-radius:6px;font:inherit;margin-top:8px}button{background:#176458;color:white;border:0;padding:12px 18px;border-radius:6px;cursor:pointer}button:disabled{opacity:.5}.error{color:#8b2525}article{border-top:1px solid #d2dfe4;padding:16px 0}details{overflow-wrap:anywhere}`]})
export class WorkflowSetupComponent implements OnInit {
  data?: Setup; busy=false; error=''; message=''; orgId=0; clientReason=''; processId=0; ownerId=0; managerId:number|null=null; reason='';
  private api=environment.apiBaseUrl+'/api/lifecycle/admin';
  constructor(private http:HttpClient){}
  ngOnInit():void{void this.load();}
  get organizationName():string{return this.data?.organizations.find(o=>o.id===this.data?.clientOrgId)?.name || '';}
  async load():Promise<void>{this.busy=true;this.error='';try{this.data=await firstValueFrom(this.http.get<Setup>(this.api+'/setup'));this.selectProcess();}catch(e:any){this.error=e.error?.message || 'Setup could not be loaded. Administrator access is required.';}finally{this.busy=false;}}
  selectProcess():void{const p=this.data?.processes.find(p=>p.id===this.processId);this.ownerId=p?.ownerId||0;this.selectOwner();this.reason='';}
  selectOwner():void{this.managerId=this.data?.users.find(u=>u.id===this.ownerId)?.managerId ?? null;}
  async configure():Promise<void>{await this.write('/client',{orgId:this.orgId,reason:this.clientReason});}
  async save():Promise<void>{const p=this.data!.processes.find(p=>p.id===this.processId)!;const u=this.data!.users.find(u=>u.id===this.ownerId)!;await this.write('/processes/'+this.processId+'/ownership',{ownerId:this.ownerId,managerId:this.managerId,expectedVersion:p.version,expectedManagerId:u.managerId,reason:this.reason});}
  private async write(path:string,body:object):Promise<void>{this.busy=true;this.error='';this.message='';try{this.data=await firstValueFrom(this.http.put<Setup>(this.api+path,body));this.selectProcess();this.message='Configuration saved and audit recorded.';}catch(e:any){this.error=e.error?.message || 'Configuration could not be saved. Refresh and try again.';}finally{this.busy=false;}}
}
