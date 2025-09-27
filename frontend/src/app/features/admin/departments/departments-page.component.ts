import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { DepartmentService } from '../services/department.service';
import { DepartmentDto } from '../models/department.model';

import { OrgGroupService } from '../services/org-group.service';
import { OrgGroupDto } from '../models/org-group.model';

@Component({
  selector: 'app-departments-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <h2>Departments</h2>

    <!-- Create -->
    <form [formGroup]="createForm" (ngSubmit)="create()" class="form">
      <label>Org Group
        <select formControlName="orgGroupId">
          <option [ngValue]="null">-- select --</option>
          <option *ngFor="let g of orgGroups" [ngValue]="g.orgGroupId">
            {{ g.orgGroupName }} (Org #{{ g.orgId }})
          </option>
        </select>
      </label>
      <label>Department Name
        <input formControlName="departmentName" />
      </label>
      <div class="field-error" *ngIf="createForm.get('departmentName')?.touched && createForm.get('departmentName')?.errors">
        <span *ngIf="createForm.get('departmentName')?.hasError('required')">Required.</span>
        <span *ngIf="createForm.get('departmentName')?.hasError('maxlength')">Max 255 characters.</span>
      </div>
      <button type="submit" [disabled]="createForm.invalid || savingCreate">Add Department</button>
      <span class="error" *ngIf="errorCreate">{{ errorCreate }}</span>
    </form>

    <h3>All Departments</h3>
    <ul class="list">
      <li *ngFor="let d of depts">
        <!-- Edit mode -->
        <ng-container *ngIf="editingId === d.departmentId; else viewRow">
          <form [formGroup]="editForm" (ngSubmit)="saveEdit(d.departmentId)" class="row edit">
            <select formControlName="orgGroupId">
              <option *ngFor="let g of orgGroups" [ngValue]="g.orgGroupId">{{ g.orgGroupName }}</option>
            </select>
            <input formControlName="departmentName" />
            <button type="submit" [disabled]="editForm.invalid || savingEdit">Save</button>
            <button type="button" (click)="cancelEdit()">Cancel</button>
          </form>
        </ng-container>

        <!-- View mode -->
        <ng-template #viewRow>
          <div class="row">
            <div>
              <strong>{{ d.departmentName }}</strong>
              <div class="muted">
                Org Group: {{ displayOrgGroupName(d.orgGroupId) }}
              </div>
              <div class="muted">
                {{ (d.subgroups?.length ?? 0) }} subgroup{{ (d.subgroups?.length ?? 0) === 1 ? '' : 's' }}
              </div>
            </div>
            <div class="actions">
              <button type="button" (click)="startEdit(d)">Edit</button>
              <button type="button" class="danger" (click)="remove(d)">Delete</button>
            </div>
          </div>
        </ng-template>
      </li>
    </ul>

    <p class="footer">
      Manage: <a [routerLink]="['/admin/dept-subgroups']">Dept Subgroups</a>
    </p>
  `,
  styles: [`
    .form { display:flex; gap:12px; align-items:end; flex-wrap:wrap; margin-bottom:1rem; }
    .form label { display:flex; flex-direction:column; gap:4px; }
    .error { color:#b00020; margin-left:.5rem; }

    .list { list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:.5rem; }
    .row { display:flex; justify-content:space-between; align-items:center; gap:12px; padding:.75rem 1rem; border:1px solid #e5e7eb; border-radius:10px; background:#fff; }
    .row.edit { gap:8px; }
    .muted { color:#6b7280; font-size:.9rem; }
    .actions { display:flex; gap:.5rem; }
    .danger { color:#b00020; }
    .footer { margin-top:1rem; color:#6b7280; }
  `]
})
export class DepartmentsPageComponent implements OnInit {
  depts: DepartmentDto[] = [];
  orgGroups: OrgGroupDto[] = [];

  // create
  createForm = this.fb.group({
    orgGroupId: [null as number | null, [Validators.required]],
    departmentName: ['', [Validators.required, Validators.maxLength(255)]]
  });
  savingCreate = false;
  errorCreate?: string;

  // edit
  editingId: number | null = null;
  editForm = this.fb.group({
    orgGroupId: [null as number | null, [Validators.required]],
    departmentName: ['', [Validators.required, Validators.maxLength(255)]]
  });
  savingEdit = false;

  constructor(
    private fb: FormBuilder,
    private deptSvc: DepartmentService,
    private groupSvc: OrgGroupService
  ) {}

  ngOnInit(): void {
    this.loadOrgGroups();
    this.refresh();
  }

  loadOrgGroups() {
    this.groupSvc.list().subscribe({
      next: rows => this.orgGroups = rows,
      error: err => console.error('Failed to load org groups', err)
    });
  }

  refresh() {
    this.deptSvc.list().subscribe({
      next: rows => this.depts = rows.map(d => ({ ...d, subgroups: d.subgroups ?? [] })),
      error: err => console.error('Failed to load departments', err)
    });
  }

  create() {
    if (this.createForm.invalid) return;
    this.savingCreate = true;
    const body = {
      orgGroupId: Number(this.createForm.value.orgGroupId),
      departmentName: this.createForm.value.departmentName!
    };
    this.deptSvc.create(body).subscribe({
      next: () => {
        this.createForm.reset();
        this.savingCreate =
          false; this.refresh();
          },
      // to validate functionality, matches GlobalExceptionHandler
      error: err => {
        this.savingCreate = false;
        const apiErrors = err?.error?.errors;
        if (apiErrors) {
          Object.entries(apiErrors).forEach(([field, messages]) => {
            const ctrl = this.createForm.get(field as string);
            if (ctrl) ctrl.setErrors({ api: (messages as string[]).join(' ') });
          });
        }
      }
    });
  }

  startEdit(d: DepartmentDto) {
    this.editingId = d.departmentId;
    this.editForm.reset({ orgGroupId: d.orgGroupId, departmentName: d.departmentName });
  }

  cancelEdit() {
    this.editingId = null;
    this.editForm.reset();
  }

  saveEdit(id: number) {
    if (this.editForm.invalid) return;
    this.savingEdit = true;
    const body = {
      orgGroupId: Number(this.editForm.value.orgGroupId),
      departmentName: this.editForm.value.departmentName!
    };
    this.deptSvc.update(id, body).subscribe({
      next: () => { this.savingEdit = false; this.editingId = null; this.refresh(); },
      error: err => { console.error('Update department failed', err); this.savingEdit = false; }
    });
  }

  remove(d: DepartmentDto) {
    const ok = confirm(`Delete department "${d.departmentName}"?`);
    if (!ok) return;
    this.deptSvc.delete(d.departmentId).subscribe({
      next: () => this.refresh(),
      error: err => {
        console.error('Delete department failed', err);
        alert(this.extractMsg(err) ?? 'Failed to delete department');
      }
    });
  }

  displayOrgGroupName(orgGroupId: number): string {
    return this.orgGroups.find(g => g.orgGroupId === orgGroupId)?.orgGroupName ?? `#${orgGroupId}`;
  }

  private extractMsg(err: any): string | undefined {
    return err?.error?.message ?? err?.error?.detail ?? err?.message;
  }
}

