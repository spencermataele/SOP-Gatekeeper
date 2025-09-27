import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Department } from '../models/department.model';
import { OrgGroupDto } from '../models/org-group.model';
import { OrgGroupService } from '../services/org-group.service';
import { DepartmentService } from '../services/department.service';

@Component({
  selector: 'app-departments-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
  <h2>Departments</h2>

  <form [formGroup]="form" (ngSubmit)="save()" class="form">
    <label>Org Group
      <select formControlName="orgGroupId">
        <option [ngValue]="null">-- select --</option>
        <option *ngFor="let g of groups" [ngValue]="g.orgGroupId">
          {{ g.orgGroupName }}
        </option>
      </select>
    </label>

    <label>Department Name
      <input formControlName="departmentName" />
    </label>

    <button type="submit" [disabled]="form.invalid || saving">Add Department</button>
    <span class="error" *ngIf="error">{{ error }}</span>
  </form>

  <section *ngFor="let bucket of deptByGroup">
    <h3>{{ bucket.group.orgGroupName }}</h3>
    <ul>
      <li *ngFor="let d of bucket.depts">
        <strong>{{ d.departmentName }}</strong>
        <div *ngIf="d.subgroups?.length; else noSub">
          <em>Subgroups:</em>
          <ul>
            <li *ngFor="let s of d.subgroups">{{ s.deptSubgroupName }}</li>
          </ul>
        </div>
        <ng-template #noSub><em>No subgroups yet.</em></ng-template>
      </li>
    </ul>
  </section>
  `,
  styles: [`
    .form { display:flex; gap:12px; align-items:end; flex-wrap:wrap; margin-bottom:1rem; }
    .form label { display:flex; flex-direction:column; gap:4px; }
    .error { color:#b00020; margin-left:.5rem; }
  `]
})
export class DepartmentsPageComponent implements OnInit {
  groups: OrgGroupDto[] = [];
  depts: Department[] = [];
  deptByGroup: { group: OrgGroupDto; depts: Department[] }[] = [];
  saving = false;
  error?: string;

  form = this.fb.group({
    orgGroupId: [null as number | null, [Validators.required]],
    departmentName: ['', [Validators.required, Validators.maxLength(255)]],
  });

  constructor(
    private fb: FormBuilder,
    private groupSvc: OrgGroupService,
    private svc: DepartmentService
  ) {}

  ngOnInit(): void {
    this.loadGroups();
    this.refresh();
  }

  loadGroups() {
    this.groupSvc.list().subscribe({
      next: rows => this.groups = rows,
      error: () => this.error = 'Failed to load org groups'
    });
  }

  refresh() {
    this.svc.list().subscribe({
      next: rows => {
        this.depts = rows;
        this.deptByGroup = this.bucketByGroup(rows);
      },
      error: () => this.error = 'Failed to load departments'
    });
  }

  save() {
    if (this.form.invalid) return;
    this.saving = true;
    const body = {
      orgGroupId: Number(this.form.value.orgGroupId),
      departmentName: this.form.value.departmentName!
    };
    this.svc.create(body).subscribe({
      next: () => { this.form.reset(); this.saving = false; this.refresh(); },
      error: () => { this.error = 'Failed to create department'; this.saving = false; }
    });
  }

  private bucketByGroup(rows: Department[]): { group: OrgGroupDto; depts: Department[] }[] {
    const map = new Map<number, { group: OrgGroupDto; depts: Department[] }>();
    for (const d of rows) {
      const g = this.groups.find(x => x.orgGroupId === d.orgGroupId);
      if (!g) continue;
      if (!map.has(g.orgGroupId)) map.set(g.orgGroupId, { group: g, depts: [] });
      map.get(g.orgGroupId)!.depts.push(d);
    }
    return Array.from(map.values());
  }
}
