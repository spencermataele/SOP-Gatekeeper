import {Component, OnInit} from "@angular/core";
import {CommonModule} from "@angular/common";
import { FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {RouterModule} from "@angular/router";
import {UserDto} from "../../sops/models/user.model";
import {UserService} from "../services/user.service";


@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <h2>Users</h2>

    <!-- Create -->
    <form [formGroup]="createForm" (ngSubmit)="create()" class="form">
      <label>User Name
        <input formControlName="username"/>
      </label>
      <!-- TODO: Add form email validation -->
      <label type="email">email
        <input formControlName="email"/>
      </label>
      <!-- TODO: Add form password cover up -->
      <label type="password">Password
        <input formControlName="password"/>
      </label>
      <label>First & Last Name
        <input formControlName="fullName"/>
      </label>
      <!-- TODO: Add form static drop menu -->
      <label>Role
        <input formControlName="role"/>
      </label>
      <button type="submit" [disabled]="createForm.invalid || savingCreate">Add User</button>
      <span class="error" *ngIf="errorCreate">{{ errorCreate }}</span>
    </form>

    <h3>All Users</h3>
    <ul class="list">
      <li *ngFor="let user of users">
        <!-- Inline edit mode -->
        <ng-container *ngIf="editingId === user.userId; else viewRow">
          <form [formGroup]="editForm" (ngSubmit)="saveEdit(user.userId)" class="row edit">
            <input formControlName="username"/>
            <button type="submit" [disabled]="editForm.invalid || savingEdit">Save</button>
            <button type="button" (click)="cancelEdit()">Cancel</button>
          </form>
        </ng-container>

        <!-- View mode -->
        <ng-template #viewRow>
          <div class="row">
            <div>
              <strong>{{ user.username }}</strong>
              <strong>{{ user.fullName }}</strong>
              <strong>{{ user.roles }}</strong>
            </div>
            <div class="actions">
              <button type="button" (click)="startEdit(user)">Edit</button>
              <button type="button" class="danger" (click)="delete(user)">Delete</button>
            </div>
          </div>
        </ng-template>
      </li>
    </ul>
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
export class UsersPageComponent implements OnInit {
  users: UserDto[] = [];

  // create
  createForm = this.fb.group({
    username: ['',  [Validators.required, Validators.maxLength(255)]],
    email: ['',  [Validators.required, Validators.maxLength(255)]],
    password: ['',  [Validators.required, Validators.maxLength(255)]],
    fullName: ['',  [Validators.required, Validators.maxLength(255)]],
    role: ['',  [Validators.required, Validators.maxLength(255)]]
  })
  savingCreate = false;
  errorCreate?: string;

  // edit
  editingId: number | null = null;
  editForm = this.fb.group({
    username: ['',  [Validators.required, Validators.maxLength(255)]],
    email: ['',  [Validators.required, Validators.maxLength(255)]],
    password: ['',  [Validators.required, Validators.maxLength(255)]],
    fullName: ['',  [Validators.required, Validators.maxLength(255)]],
    role: ['',  [Validators.required, Validators.maxLength(255)]]
  })
  savingEdit = false;

  constructor(private  fb: FormBuilder, private  svc: UserService) {}

  ngOnInit(): void { this.refresh(); }

  refresh() {
    this.svc.list().subscribe( {
      next: rows => this.users = rows,
      error: () => this.errorCreate = 'Failed to load Users.'
    })
  }

  create() {
    if (this.createForm.invalid) return;
    this.savingCreate = true;
    this.svc.create({
      username: this.createForm.value.username!,
      email: this.createForm.value.email!,
      password: this.createForm.value.password!,
      fullName: this.createForm.value.fullName!,
      role: this.createForm.value.role!
    }).subscribe({
      next: () => {
        this.createForm.reset();
        this.savingCreate = false;
        this.refresh();
      },
      // to validate functionality
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

  startEdit(user: UserDto) {
    // userId cannot be null
    if (user.userId == null) {
      throw new Error('User ID is missing')
    }
    this.editingId = user.userId;
    this.editForm.reset({
      username: user.username ?? '',
      email: user.email ?? '',
      password: '',
      fullName: user.fullName ?? '',
      role: user.roles ?? ''
    });
  }

  cancelEdit() {
    this.editingId = null;
    this.editForm.reset();
  }

  saveEdit(id: number) {
    if (this.editForm.invalid) return;
    this.savingEdit = true;
    this.svc.update(id, {
      username: this.editForm.value.username!,
      email: this.editForm.value.email!,
      password: this.editForm.value.password!,
      fullName: this.editForm.value.fullName!,
      role: this.editForm.value.role!
    }).subscribe({
      next: () => {
        this.savingEdit = false;
        this.editingId = null;
        this.refresh();
      },
      error: (err) => {
        console.error('Update user failed', err);
        this.savingEdit = false;
      }
    });
  }

  delete(user: UserDto) {
    // userId cannot be null
    if (user.userId == null) {
      console.error(`Cannot delete user without userId`, user);
      return;
    }

    const ok = confirm(`Delete user "${user.userId}"?`);
    if (!ok) return;

    this.svc.delete(user.userId).subscribe({
      next: () => this.refresh(),
      error: (err) => {
        console.error('Delete user failed', err);
      }
    });
  }


}
