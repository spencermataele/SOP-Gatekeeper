import {Component,OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder,ReactiveFormsModule,Validators} from '@angular/forms';
import {ActivatedRoute,Router} from '@angular/router';
import {forkJoin} from 'rxjs';
import {BusinessProcessService} from '../services/business-process.service';
import {BusinessProcessFamilyService} from '../services/business-process-family.service';
import {BusinessProcessFamily} from '../models/business-process-family.model';
import {BusinessProcess} from '../models/business-process.model';
import {AdminParentSelectorComponent} from '../admin-parent-selector.component';
@Component({selector:'app-business-process-form',standalone:true,imports:[CommonModule,ReactiveFormsModule,AdminParentSelectorComponent],templateUrl:'./business-process-form.component.html'})
export class BusinessProcessFormComponent implements OnInit {
 id?:number;loading=true;saving=false;error='';families:BusinessProcessFamily[]=[];processes:BusinessProcess[]=[];
 form=this.fb.group({businessProcessName:['',[Validators.required,Validators.maxLength(255)]],businessProcessFamilyId:[null as number|null,Validators.required],parentBusinessProcessId:[null as number|null]});
 constructor(private fb:FormBuilder,private svc:BusinessProcessService,private familySvc:BusinessProcessFamilyService,private route:ActivatedRoute,private router:Router){}
 ngOnInit():void{
  const id=this.route.snapshot.paramMap.get('id');if(id&&id!=='new')this.id=Number(id);
  this.form.controls.businessProcessFamilyId.valueChanges.subscribe(()=>this.form.controls.parentBusinessProcessId.setValue(null));
  forkJoin({families:this.familySvc.list(),processes:this.svc.list()}).subscribe({next:data=>{
   this.families=data.families;this.processes=data.processes;
   if(this.id)this.svc.get(this.id).subscribe({next:p=>{this.form.patchValue({businessProcessName:p.businessProcessName,businessProcessFamilyId:p.businessProcessFamily,parentBusinessProcessId:p.parentBusinessProcess??null},{emitEvent:false});this.loading=false;},error:()=>{this.error='Process could not be loaded.';this.loading=false;}});
   else this.loading=false;
  },error:()=>{this.error='Hierarchy could not be loaded. Reload this page to retry.';this.loading=false;}});
 }
 get parents():BusinessProcess[]{return this.processes.filter(p=>p.businessProcessFamily===this.form.value.businessProcessFamilyId&&p.businessProcessId!==this.id);}
 save():void{if(this.form.invalid||this.saving)return;const value=this.form.getRawValue();const family=this.families.find(f=>f.businessProcessFamilyId===value.businessProcessFamilyId);if(!family){this.error='Choose a valid process family.';return;}
  this.saving=true;const body={businessProcessName:value.businessProcessName!,businessProcessFamilyId:family.businessProcessFamilyId,parentBusinessProcessId:value.parentBusinessProcessId,departmentId:family.departmentId};
  (this.id?this.svc.update(this.id,body):this.svc.create(body)).subscribe({next:()=>{this.saving=false;void this.router.navigate(['/admin/business-processes']);},error:e=>{this.saving=false;this.error=e.error?.message||'Process could not be saved.';}});
 }
 cancel():void{void this.router.navigate(['/admin/business-processes']);}
}
