import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Department } from '../models/department.model';
import { DeptSubgroup } from '../models/dept-subgroup.model';
import { DepartmentService } from '../services/department.service';
import { DeptSubgroupService } from '../services/dept-subgroup.service';

@Component({
  selector: 'app-dept-subgroups-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
  <h2>Dept Subgroups</h2>

  <form [formGroup]="form" (ngSubmit)="save()" class="form">
    <label>Department
      <select formControlName="departmentId">
        <option [ngValue]="null">-- select --</option>
        <option *ngFor="let d of depts" [ngValue]="d.departmentId">
          {{ d.departmentName }}
        </option>
      </select>
    </label>

    <label>Subgroup Name
      <input formControlName="deptSubgroupName" />
    </label>

    <button type="submit" [disabled]="form.invalid || saving">Add Subgroup</button>
    <span class="error" *ngIf="error">{{ error }}</span>
  </form>

  <section *ngFor="let bucket of subByDept">
    <h3>{{ bucket.dept.departmentName }}</h3>
    <ul>
      <li *ngFor="let s of bucket.subgroups">{{ s.deptSubgroupName }}</li>
    </ul>
    <div *ngIf="!bucket.subgroups.length"><em>No subgroups yet.</em></div>
  </section>
  `,
  styles: [`
    .form { display:flex; gap:12px; align-items:end; flex-wrap:wrap; margin-bottom:1rem; }
    .form label { display:flex; flex-direction:column; gap:4px; }
    .error { color:#b00020; margin-left:.5rem; }
  `]
})
export class DeptSubgroupsPageComponent implements OnInit {
  depts: Department[] = [];
  subs: DeptSubgroup[] = [];
  subByDept: { dept: Department; subgroups: DeptSubgroup[] }[] = [];
  saving = false;
  error?: string;

  form = this.fb.group({
    departmentId: [null as number | null, [Validators.required]],
    deptSubgroupName: ['', [Validators.required, Validators.maxLength(255)]],
  });

  constructor(
    private fb: FormBuilder,
    private deptSvc: DepartmentService,
    private svc: DeptSubgroupService
  ) {}

  ngOnInit(): void {
    this.loadDepts();
    this.refresh();
  }

  loadDepts() {
    this.deptSvc.list().subscribe({
      next: rows => this.depts = rows,
      error: () => this.error = 'Failed to load departments'
    });
  }

  refresh() {
    this.svc.list().subscribe({
      next: rows => {
        this.subs = rows;
        this.subByDept = this.bucketByDepartment(rows);
      },
      error: () => this.error = 'Failed to load subgroups'
    });
  }

  save() {
    if (this.form.invalid) return;
    this.saving = true;
    const body = {
      departmentId: Number(this.form.value.departmentId),
      deptSubgroupName: this.form.value.deptSubgroupName!
    };
    this.svc.create(body).subscribe({
      next: () => { this.form.reset(); this.saving = false; this.refresh(); },
      error: () => { this.error = 'Failed to create subgroup'; this.saving = false; }
    });
  }

  private bucketByDepartment(rows: DeptSubgroup[]): { dept: Department; subgroups: DeptSubgroup[] }[] {
    const map = new Map<number, { dept: Department; subgroups: DeptSubgroup[] }>();
    for (const s of rows) {
      const d = this.depts.find(x => x.departmentId === s.departmentId);
      if (!d) continue;
      if (!map.has(d.departmentId)) map.set(d.departmentId, { dept: d, subgroups: [] });
      map.get(d.departmentId)!.subgroups.push(s);
    }
    return Array.from(map.values());
  }
}
