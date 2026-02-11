import {Component, ElementRef, OnInit, ViewChild} from "@angular/core";
import {CommonModule} from "@angular/common";
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors, ValidatorFn} from "@angular/forms";
import {RouterModule} from "@angular/router";
import {UserDto} from "../../sops/models/user.model";
import {UserService} from "../services/user.service";


@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <section class="container">
      <h2>Users</h2>

      <!-- Create -->
      <form [formGroup]="createForm" (ngSubmit)="create()" class="form">
        <label>User Name
          <input formControlName="username" required/>
        </label>
        <label>Email
          <input type="email" formControlName="email" required/>
        </label>
        <label>Password
          <input type="password" formControlName="password" required/>
        </label>
        <label>First & Last Name
          <input formControlName="fullName" required/>
        </label>
        <label>Role
          <select formControlName="role" required>
              <option value="" disabled>Select role</option>
              <option *ngFor="let r of roles" [value]="r"> {{ r }}</option>
          </select>
        </label>
        <hr/>
        <button type="submit" [disabled]="createForm.invalid || savingCreate">Add User</button>
        <span class="error" *ngIf="errorCreate">{{ errorCreate }}</span>
      </form>
    </section>

    <div>
      <hr/>
    </div>

    <section class="container">
      <h3>All Users</h3>
      <table>
        <thead>
        <tr>
          <th>Username</th>
          <th>Email</th>
          <th>Full Name</th>
          <th>Role</th>
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let user of users">
          <td>{{ user.username }}</td>
          <td>{{ user.email }}</td>
          <td>{{ user.fullName }}</td>
          <td>{{ user.role }}</td>
          <td>
            <button (click)="startEdit(user)">Edit</button>
            <button class="danger" (click)="delete(user)">Delete</button>
          </td>
        </tr>
        </tbody>
      </table>
    </section>

    <div>
      <hr/>
    </div>

    <section class="container">
      <form
        #editSection
        *ngIf="editingId !== null"
        [formGroup]="editForm"
        (ngSubmit)="saveEdit(editingId!)"
        class="form">

        <h3>Edit User</h3>

        <label>User Name
          <input formControlName="username"/>
        </label>

        <label>Email
          <input type="email" formControlName="email"/>
        </label>

        <label>Password
          <input type="password" formControlName="password"/>
        </label>

        <label>First & Last Name
          <input formControlName="fullName"/>
        </label>

        <label>Role
          <select formControlName="role">
            <option value="" disabled>Select role</option>
            <option *ngFor="let r of roles" [value]="r">{{ r }}</option>
          </select>
        </label>

        <div class="actions">
          <button type="submit" [disabled]="editForm.invalid || savingEdit">
            Save
          </button>

          <button type="button" class="secondary" (click)="cancelEdit()">
            Cancel
          </button>
        </div>
      </form>
    </section>
  `,
  styleUrls: ['../../../../styles.css']
})
export class UsersPageComponent implements OnInit {
  users: UserDto[] = [];
  readonly roles = ['ADMIN', 'USER'] as const;

  // autoscroll to the edit section
  @ViewChild('editSection')
  editsection?: ElementRef<HTMLFormElement>;

  // create
  createForm = this.fb.group({
    username: ['',  [Validators.required, Validators.maxLength(255)]],
    email: ['',  [Validators.required, Validators.maxLength(255)]],
    password: ['',  [Validators.required, Validators.maxLength(255)]],
    fullName: ['',  [Validators.required, Validators.maxLength(255)]],
    role: [
      '',
      [
        Validators.required,
        oneOf(this.roles)
      ]
    ]

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
    role: [
      '',
      [
        Validators.required,
        oneOf(this.roles)
      ]
    ]

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
    if (user.id == null) {
      throw new Error('User ID is missing')
    }
    this.editingId = user.id;
    this.editForm.reset({
      username: user.username ?? '',
      email: user.email ?? '',
      password: '',
      fullName: user.fullName ?? '',
      role: user.role ?? ''
    });

    // autoscroll
    setTimeout(() => {
      this.editsection?.nativeElement.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
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
    if (user.id == null) {
      console.error(`Cannot delete user without userId`, user);
      return;
    }

    const ok = confirm(`Delete user "${user.id}"?`);
    if (!ok) return;

    this.svc.delete(user.id).subscribe({
      next: () => this.refresh(),
      error: (err) => {
        console.error('Delete user failed', err);
      }
    });
  }
}

function oneOf<T extends readonly string[]>(allowed: T): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    return allowed.includes(control.value)
      ? null
      : { oneOf: { allowed } };
  };
}

