import {Component, EventEmitter, Input, OnChanges, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
export interface GovernedProcess { id: number; name: string; owner: string; orgId: number; orgName: string; groupId: number; groupName: string; departmentId: number; departmentName: string; familyId: number; familyName: string; }
@Component({selector:'app-process-selector',standalone:true,imports:[CommonModule,FormsModule],template:`
  <fieldset [disabled]="disabled"><legend>Process and organization</legend>
    <p *ngIf="processes.length">Organization: <strong>{{processes[0].orgName}}</strong></p>
    <p *ngIf="!processes.length">No processes are ready for authoring. An administrator must configure the client organization and assign a process owner in Admin → Workflow setup.</p>
    <label>Organization group<select [(ngModel)]="group" [ngModelOptions]="{standalone:true}" (ngModelChange)="reset('group')"><option [ngValue]="0">Choose a group</option><option *ngFor="let p of unique(processes,'groupId')" [ngValue]="p.groupId">{{p.groupName}}</option></select></label>
    <label>Department<select [(ngModel)]="department" [ngModelOptions]="{standalone:true}" (ngModelChange)="reset('department')" [disabled]="!group"><option [ngValue]="0">Choose a department</option><option *ngFor="let p of unique(inGroup,'departmentId')" [ngValue]="p.departmentId">{{p.departmentName}}</option></select></label>
    <label>Process family<select [(ngModel)]="family" [ngModelOptions]="{standalone:true}" (ngModelChange)="reset('family')" [disabled]="!department"><option [ngValue]="0">Choose a family</option><option *ngFor="let p of unique(inDepartment,'familyId')" [ngValue]="p.familyId">{{p.familyName}}</option></select></label>
    <label>Process<select [ngModel]="value" [ngModelOptions]="{standalone:true}" (ngModelChange)="valueChange.emit($event)" [disabled]="!family"><option [ngValue]="0">Choose a process</option><option *ngFor="let p of inFamily" [ngValue]="p.id">{{p.name}} · Owner: {{p.owner}}</option></select></label>
  </fieldset>`,styles:[`fieldset{border:1px solid #d0dee3;padding:16px;border-radius:8px}legend{font-weight:bold}label{display:block;margin:12px 0;font-weight:600}select{display:block;width:100%;padding:12px;border:1px solid #a8b8c1;border-radius:6px;margin-top:8px;font:inherit}`]})
export class ProcessSelectorComponent implements OnChanges {
  @Input() processes: GovernedProcess[]=[]; @Input() value=0; @Input() disabled=false;
  @Output() valueChange=new EventEmitter<number>();
  group=0; department=0; family=0;
  ngOnChanges(): void { const p=this.processes.find(p=>p.id===this.value); if(p){this.group=p.groupId;this.department=p.departmentId;this.family=p.familyId;} }
  get inGroup(): GovernedProcess[]{return this.processes.filter(p=>p.groupId===this.group);}
  get inDepartment(): GovernedProcess[]{return this.inGroup.filter(p=>p.departmentId===this.department);}
  get inFamily(): GovernedProcess[]{return this.inDepartment.filter(p=>p.familyId===this.family);}
  unique(items: GovernedProcess[], key: 'groupId'|'departmentId'|'familyId'): GovernedProcess[]{return items.filter((p,i)=>items.findIndex(v=>v[key]===p[key])===i);}
  reset(level:string):void{if(level==='group')this.department=0;if(level!=='family')this.family=0;this.valueChange.emit(0);}
}
