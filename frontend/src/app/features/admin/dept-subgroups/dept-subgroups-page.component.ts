import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { DeptSubgroupService } from '../services/dept-subgroup.service';
import { DeptSubgroupDto } from '../models/dept-subgroup.model';

import { DepartmentService } from '../services/department.service';
import { DepartmentDto } from '../models/department.model';

@Component({
  selector: 'app-dept-subgroups-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <h2>Department Subgroups</h2>

    <!-- Create -->
    <form [formGroup]="createForm" (ngSubmit)="create()" class="form">
      <label>Department
        <select formControlName="departmentId">
          <option [ngValue]="null">-- select --</option>
          <option *ngFor="let d of departments" [ngValue]="d.departmentId">
            {{ d.departmentName }}
          </option>
        </select>
      </label>
      <label>Subgroup Name
        <input formControlName="deptSubgroupName" />
      </label>
      <button type="submit" [disabled]="createForm.invalid || savingCreate">Add Subgroup</button>
      <span class="error" *ngIf="errorCreate">{{ errorCreate }}</span>
    </form>

    <h3>All Subgroups</h3>
    <ul class="list">
      <li *ngFor="let s of subs">
        <!-- Edit mode -->
        <ng-container *ngIf="editingId === s.deptSubgroupId; else viewRow">
          <form [formGroup]="editForm" (ngSubmit)="saveEdit(s.deptSubgroupId)" class="row edit">
            <select formControlName="departmentId">
              <option *ngFor="let d of departments" [ngValue]="d.departmentId">{{ d.departmentName }}</option>
            </select>
            <input formControlName="deptSubgroupName" />
            <button type="submit" [disabled]="editForm.invalid || savingEdit">Save</button>
            <button type="button" (click)="cancelEdit()">Cancel</button>
          </form>
        </ng-container>

        <!-- View mode -->
        <ng-template #viewRow>
          <div class="row">
            <div>
              <strong>{{ s.deptSubgroupName }}</strong>
              <div class="muted">Department: {{ displayDepartmentName(s.departmentId) }}</div>
            </div>
            <div class="actions">
              <button type="button" (click)="startEdit(s)">Edit</button>
              <button type="button" class="danger" (click)="remove(s)">Delete</button>
            </div>
          </div>
        </ng-template>
      </li>
    </ul>

    <p class="footer">
      Manage: <a [routerLink]="['/admin/departments']">Departments</a>
    </p>
  `,
  styleUrls: ['../../../../styles.css']
})
export class DeptSubgroupsPageComponent implements OnInit {
  subs: DeptSubgroupDto[] = [];
  departments: DepartmentDto[] = [];

  // create
  createForm = this.fb.group({
    departmentId: [null as number | null, [Validators.required]],
    deptSubgroupName: ['', [Validators.required, Validators.maxLength(255)]]
  });
  savingCreate = false;
  errorCreate?: string;

  // edit
  editingId: number | null = null;
  editForm = this.fb.group({
    departmentId: [null as number | null, [Validators.required]],
    deptSubgroupName: ['', [Validators.required, Validators.maxLength(255)]]
  });
  savingEdit = false;

  constructor(
    private fb: FormBuilder,
    private svc: DeptSubgroupService,
    private deptSvc: DepartmentService
  ) {}

  ngOnInit(): void {
    this.loadDepartments();
    this.refresh();
  }

  loadDepartments() {
    this.deptSvc.list().subscribe({
      next: rows => this.departments = rows,
      error: err => console.error('Failed to load departments', err)
    });
  }

  refresh() {
    this.svc.list().subscribe({
      next: rows => this.subs = rows,
      error: err => console.error('Failed to load subgroups', err)
    });
  }

  create() {
    if (this.createForm.invalid) return;
    this.savingCreate = true;
    // @ts-ignore
    const body = {
      departmentId: Number(this.createForm.value.departmentId),
      deptSubgroupName: this.createForm.value.deptSubgroupName!
    };
    this.svc.create(body).subscribe({
      next: () => {
        this.createForm.reset();
        this.savingCreate = false;
        this.refresh();
      },
      // to validate functionality, matches GlobalExceptionHandler
      error: err => {
        this.savingCreate = false;
        const apiErrors = err?.error?.errors;
        if (apiErrors) {
          Object.entries(apiErrors).forEach(([field, messages]) => {
            const ctrl = this.createForm.get(field as string);
            if (ctrl) ctrl.setErrors({api: (messages as string[]).join(' ')});
          });
        }
      }
    });
  }

  startEdit(s: DeptSubgroupDto) {
    this.editingId = s.deptSubgroupId;
    this.editForm.reset({ departmentId: s.departmentId, deptSubgroupName: s.deptSubgroupName });
  }

  cancelEdit() {
    this.editingId = null;
    this.editForm.reset();
  }

  saveEdit(id: number) {
    if (this.editForm.invalid) return;
    this.savingEdit = true;
    const body = {
      departmentId: Number(this.editForm.value.departmentId),
      deptSubgroupName: this.editForm.value.deptSubgroupName!
    };
    this.svc.update(id, body).subscribe({
      next: () => { this.savingEdit = false; this.editingId = null; this.refresh(); },
      error: err => { console.error('Update subgroup failed', err); this.savingEdit = false; }
    });
  }

  remove(s: DeptSubgroupDto) {
    const ok = confirm(`Delete subgroup "${s.deptSubgroupName}"?`);
    if (!ok) return;
    this.svc.delete(s.deptSubgroupId).subscribe({
      next: () => this.refresh(),
      error: err => {
        console.error('Delete subgroup failed', err);
        alert(this.extractMsg(err) ?? 'Failed to delete subgroup');
      }
    });
  }

  displayDepartmentName(deptId: number): string {
    return this.departments.find(d => d.departmentId === deptId)?.departmentName ?? `#${deptId}`;
  }

  private extractMsg(err: any): string | undefined {
    return err?.error?.message ?? err?.error?.detail ?? err?.message;
  }
}

