import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from "@angular/common";
import { BusinessProcessService } from '../services/business-process.service';
import { BusinessProcessFamilyService } from '../services/business-process-family.service';
import { ActivatedRoute, Router } from '@angular/router';
import { BusinessProcess } from '../models/business-process.model';
import { BusinessProcessFamily } from '../models/business-process-family.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface SubgroupLite { departmentId: number; deptSubgroupName: string; }

@Component({
  selector: 'app-business-process-form',
  templateUrl: './business-process-form.component.html',
  imports: [
    ReactiveFormsModule,
    CommonModule
  ],
  standalone: true
})
export class BusinessProcessFormComponent implements OnInit {
  id?: number;
  loading = false;
  saving = false;
  error?: string;

  families: BusinessProcessFamily[] = [];
  parents: BusinessProcess[] = [];
  subgroups: SubgroupLite[] = [];

  form = this.fb.group({
    businessProcessName: ['', [Validators.required, Validators.maxLength(255)]],
    businessProcessFamilyId: [null as number | null, Validators.required],
    parentProcessId: [null as number | null],
    departmentId: [{ value: null as number | null, disabled: true }], // derived from family
    deptSubgroupIds: [[] as number[]],
  });

  constructor(
    private fb: FormBuilder,
    private svc: BusinessProcessService,
    private familySvc: BusinessProcessFamilyService,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadFamilies();
    this.loadSubgroups();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.id = +idParam;
      this.loading = true;
      this.svc.get(this.id).subscribe({
        next: p => {
          this.form.patchValue({
            businessProcessName: p.businessProcessName,
            businessProcessFamilyId: p.businessProcessFamilyId,
            parentProcessId: p.parentProcessId ?? null,
            departmentId: p.departmentId,
            deptSubgroupIds: p.deptSubgroupIds || []
          });
          this.onFamilyChanged(p.businessProcessFamilyId);
          this.loading = false;
        },
        error: () => { this.error = 'Failed to load.'; this.loading = false; }
      });
    }

    this.form.get('businessProcessFamilyId')!.valueChanges.subscribe(val => {
      if (val != null) this.onFamilyChanged(val);
    });
  }

  loadFamilies() {
    this.familySvc.list().subscribe({ next: fs => this.families = fs });
  }

  loadSubgroups() {
    this.http.get<SubgroupLite[]>(`${environment.apiBaseUrl}/admin/dept-subgroups`)
      .subscribe({ next: sgs => this.subgroups = sgs });
  }

  onFamilyChanged(familyId: number) {
    const fam = this.families.find(f => f.businessProcessFamilyId === familyId);
    // derive department from family
    if (fam?.departmentId != null) {
      this.form.get('departmentId')!.setValue(fam.departmentId);
    } else {
      this.form.get('departmentId')!.setValue(null);
    }
    // reload candidate parents limited to same family
    this.svc.list({ familyId }).subscribe({ next: ps => {
        // exclude self (when editing)
        this.parents = (this.id ? ps.filter(p => p.businessProcessId !== this.id) : ps);
      }});
  }

  filteredSubgroups() {
    const familyId = this.form.value.businessProcessFamilyId ?? null;

    // No family selected: show all alphabetically
    if (!familyId) {
      return [...this.subgroups].sort((a, b) =>
        a.deptSubgroupName.localeCompare(b.deptSubgroupName)
      );
    }

    // Filter by matching familyId
    const fam = this.families.find(f => f.businessProcessFamilyId === familyId);
    if (!fam || !fam.departmentId) {
      return [];
    }

    return this.subgroups
      .filter(s => s.departmentId === fam.departmentId)
      .sort((a, b) => a.deptSubgroupName.localeCompare(b.deptSubgroupName));
  }



  save() {
    if (this.form.invalid) return;
    this.saving = true;

    const v = this.form.getRawValue(); // includes disabled departmentId
    const payload = {
      businessProcessName: v.businessProcessName!,
      businessProcessFamilyId: v.businessProcessFamilyId!,
      parentProcessId: v.parentProcessId ?? null,
      departmentId: v.departmentId!,             // server validates it matches family's department
      deptSubgroupIds: v.deptSubgroupIds || [],
    };

    const req$ = this.id ? this.svc.update(this.id, payload) : this.svc.create(payload);

    req$.subscribe({
      next: () => { this.saving = false; this.router.navigate(['/admin/business-processes']); },
      error: () => { this.saving = false; this.error = 'Save failed.'; }
    });
  }

  cancel() { this.router.navigate(['/admin/business-processes']); }

}
