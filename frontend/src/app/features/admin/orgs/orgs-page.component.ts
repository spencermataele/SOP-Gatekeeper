import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { OrgService } from '../services/org.service';
import { OrgDto } from '../models/org.model';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-orgs-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <h2>Organizations</h2>

    <!-- Create -->
    <form [formGroup]="createForm" (ngSubmit)="create()" class="form">
      <label>Org Name
        <input formControlName="orgName" />
      </label>
      <button type="submit" [disabled]="createForm.invalid || savingCreate">Add Org</button>
      <span class="error" *ngIf="errorCreate">{{ errorCreate }}</span>
    </form>

    <h3>All Orgs</h3>
    <ul class="list">
      <li *ngFor="let org of orgs">
        <!-- Inline edit mode -->
        <ng-container *ngIf="editingId === org.orgId; else viewRow">
          <form [formGroup]="editForm" (ngSubmit)="saveEdit(org.orgId)" class="row edit">
            <input formControlName="orgName" />
            <button type="submit" [disabled]="editForm.invalid || savingEdit">Save</button>
            <button type="button" (click)="cancelEdit()">Cancel</button>
          </form>
        </ng-container>

        <!-- View mode -->
        <ng-template #viewRow>
          <div class="row">
            <div>
              <strong>{{ org.orgName }}</strong>
              <div class="muted" *ngIf="org.orgGroups?.length">
                {{ org.orgGroups.length }} group{{ org.orgGroups.length === 1 ? '' : 's' }}
              </div>
              <div class="muted" *ngIf="!org.orgGroups?.length">No groups yet.</div>
            </div>
            <div class="actions">
              <button type="button" (click)="startEdit(org)">Edit</button>
              <button type="button" class="danger" (click)="delete(org)">Delete</button>
            </div>
          </div>
        </ng-template>
      </li>
    </ul>

    <p class="footer">
      Manage: <a [routerLink]="['/admin/org-groups']">Org Groups</a> →
      <a [routerLink]="['/admin/departments']">Departments</a> →
      <a [routerLink]="['/admin/dept-subgroups']">Dept Subgroups</a>
    </p>
  `,

  styleUrls: ['../../../../styles.css']
})
export class OrgsPageComponent implements OnInit {
  orgs: OrgDto[] = [];

  // create
  createForm = this.fb.group({
    orgName: ['', [Validators.required, Validators.maxLength(255)]]
  });
  savingCreate = false;
  errorCreate?: string;

  // edit
  editingId: number | null = null;
  editForm = this.fb.group({
    orgName: ['', [Validators.required, Validators.maxLength(255)]]
  });
  savingEdit = false;

  constructor(private fb: FormBuilder, private svc: OrgService) {}

  ngOnInit(): void { this.refresh(); }

  refresh() {
    this.svc.list().subscribe({
      next: rows => this.orgs = rows,
      error: () => this.errorCreate = 'Failed to load Orgs'
    });
  }

  create() {
    if (this.createForm.invalid) return;
    this.savingCreate = true;
    this.svc.create({ orgName: this.createForm.value.orgName! }).subscribe({
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

  startEdit(org: OrgDto) {
    this.editingId = org.orgId;
    this.editForm.reset({ orgName: org.orgName });
  }

  cancelEdit() {
    this.editingId = null;
    this.editForm.reset();
  }

  saveEdit(id: number) {
    if (this.editForm.invalid) return;
    this.savingEdit = true;
    this.svc.update(id, { orgName: this.editForm.value.orgName! }).subscribe({
      next: () => { this.savingEdit = false; this.editingId = null; this.refresh(); },
      error: (err) => {
        console.error('Update org failed', err);
        this.savingEdit = false;
      }
    });
  }

  delete(org: OrgDto) {
    const ok = confirm(`Delete org "${org.orgName}"?`);
    if (!ok) return;
    this.svc.delete(org.orgId).subscribe({
      next: () => this.refresh(),
      error: (err) => {
        console.error('Delete org failed', err);
        // @ts-ignore
        alert(this.extractMsg(err) ?? 'Failed to delete org');
      }
    });
  }

  private extractMsg(err: any): string | undefined {
    return err?.error?.message ?? err?.error?.detail ?? err?.message;
  }
}

