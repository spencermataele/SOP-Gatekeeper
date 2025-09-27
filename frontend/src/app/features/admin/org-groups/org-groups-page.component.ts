import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { OrgService } from '../services/org.service';
import { OrgDto } from '../models/org.model';
import { OrgGroupService } from '../services/org-group.service';
import { OrgGroupDto } from '../models/org-group.model';

@Component({
  selector: 'app-org-groups-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <h2>Org Groups</h2>

    <!-- Create -->
    <form [formGroup]="createForm" (ngSubmit)="create()" class="form">
      <label>Org
        <select formControlName="orgId">
          <option [ngValue]="null">-- select --</option>
          <option *ngFor="let o of orgs" [ngValue]="o.orgId">{{ o.orgName }}</option>
        </select>
      </label>
      <label>Org Group Name
        <input formControlName="orgGroupName" />
      </label>
      <button type="submit" [disabled]="createForm.invalid || savingCreate">Add Org Group</button>
      <span class="error" *ngIf="errorCreate">{{ errorCreate }}</span>
    </form>

    <h3>All Org Groups</h3>
    <ul class="list">
      <li *ngFor="let g of groups">
        <!-- Edit mode -->
        <ng-container *ngIf="editingId === g.orgGroupId; else viewRow">
          <form [formGroup]="editForm" (ngSubmit)="saveEdit(g.orgGroupId)" class="row edit">
            <select formControlName="orgId">
              <option *ngFor="let o of orgs" [ngValue]="o.orgId">{{ o.orgName }}</option>
            </select>
            <input formControlName="orgGroupName" />
            <button type="submit" [disabled]="editForm.invalid || savingEdit">Save</button>
            <button type="button" (click)="cancelEdit()">Cancel</button>
          </form>
        </ng-container>

        <!-- View mode -->
        <ng-template #viewRow>
          <div class="row">
            <div>
              <strong>{{ g.orgGroupName }}</strong>
              <div class="muted">
                Org: {{ displayOrgName(g.orgId) }}
              </div>
              <div *ngIf="(g.departments?.length ?? 0) > 0; else noDept" class="muted">
                {{ (g.departments?.length ?? 0) }} department{{ (g.departments?.length ?? 0) === 1 ? '' : 's' }}
              </div>
            <ng-template #noDept><span class="muted">No departments yet.</span></ng-template>
            </div>
            <div class="actions">
              <button type="button" (click)="startEdit(g)">Edit</button>
              <button type="button" class="danger" (click)="remove(g)">Delete</button>
            </div>
          </div>
        </ng-template>
      </li>
    </ul>

    <p class="footer">
      Manage: <a [routerLink]="['/admin/departments']">Departments</a> →
      <a [routerLink]="['/admin/dept-subgroups']">Dept Subgroups</a>
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
export class OrgGroupsPageComponent implements OnInit {
  orgs: OrgDto[] = [];
  groups: OrgGroupDto[] = [];

  // create
  createForm = this.fb.group({
    orgId: [null as number | null, [Validators.required]],
    orgGroupName: ['', [Validators.required, Validators.maxLength(255)]]
  });
  savingCreate = false;
  errorCreate?: string;

  // edit
  editingId: number | null = null;
  editForm = this.fb.group({
    orgId: [null as number | null, [Validators.required]],
    orgGroupName: ['', [Validators.required, Validators.maxLength(255)]]
  });
  savingEdit = false;

  constructor(
    private fb: FormBuilder,
    private orgSvc: OrgService,
    private svc: OrgGroupService
  ) {}

  ngOnInit(): void {
    this.loadOrgs();
    this.refresh();
  }

  loadOrgs() {
    this.orgSvc.list().subscribe({
      next: rows => this.orgs = rows,
      error: err => console.error('Failed to load orgs', err)
    });
  }

  refresh() {
    this.svc.list().subscribe({
      next: rows => this.groups = rows,
      error: err => console.error('Failed to load org groups', err)
    });
  }

  create() {
    if (this.createForm.invalid) return;
    this.savingCreate = true;
    const body = {
      orgId: Number(this.createForm.value.orgId),
      orgGroupName: this.createForm.value.orgGroupName!
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

  startEdit(g: OrgGroupDto) {
    this.editingId = g.orgGroupId;
    this.editForm.reset({ orgId: g.orgId, orgGroupName: g.orgGroupName });
  }

  cancelEdit() {
    this.editingId = null;
    this.editForm.reset();
  }

  saveEdit(id: number) {
    if (this.editForm.invalid) return;
    this.savingEdit = true;
    const body = {
      orgId: Number(this.editForm.value.orgId),
      orgGroupName: this.editForm.value.orgGroupName!
    };
    this.svc.update(id, body).subscribe({
      next: () => { this.savingEdit = false; this.editingId = null; this.refresh(); },
      error: err => { console.error('Update org group failed', err); this.savingEdit = false; }
    });
  }

  remove(g: OrgGroupDto) {
    const ok = confirm(`Delete org group "${g.orgGroupName}"?`);
    if (!ok) return;
    this.svc.delete(g.orgGroupId).subscribe({
      next: () => this.refresh(),
      error: err => {
        console.error('Delete org group failed', err);
        alert(this.extractMsg(err) ?? 'Failed to delete org group');
      }
    });
  }

  displayOrgName(orgId: number): string {
    return this.orgs.find(o => o.orgId === orgId)?.orgName ?? `#${orgId}`;
  }

  private extractMsg(err: any): string | undefined {
    return err?.error?.message ?? err?.error?.detail ?? err?.message;
  }
}

