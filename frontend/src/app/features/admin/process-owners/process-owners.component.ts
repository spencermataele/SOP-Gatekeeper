import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ProcessOwnerService } from '../services/process-owner.service';
import { ProcessOwner } from '../../sops/models/process-owner.model';
import {UserDto} from "../../sops/models/user.model";
import {UserService} from "../services/user.service";

@Component({
  selector: 'app-process-owners',
  templateUrl: './process-owners.component.html',
  styleUrls: ['./process-owners.component.scss']
})
export class ProcessOwnersComponent implements OnInit {
  owners: ProcessOwner[] = [];
  loading = false;
  error?: string;
  users: UserDto[] = [];

  // form state
  form!: FormGroup;
  editingId?: number;

  // filters
  filterName = '';
  filterId?: number | null;

  constructor(
    private fb: FormBuilder,
    private svc: ProcessOwnerService,
    private userSvc: UserService
  ) {}

  ngOnInit(): void {

    this.form = this.fb.group({
      userId: ['', [Validators.required]],
      positionId: [null, [Validators.required]]
    });

    this.userSvc.list().subscribe({
      next: users =>
        this.users = users,
        error: () =>
          this.error = 'Failed to load users.'
    });

    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    this.svc.list().subscribe({
      next: data => { this.owners = data; this.loading = false; },
      error: () => { this.error = 'Failed to load process owners'; this.loading = false; }
    });
  }

  startCreate(): void {
    this.editingId = undefined;
    this.form.reset({ userId: null, positionId: null });
  }

  startEdit(row: ProcessOwner): void {
    this.editingId = row.businessProcessOwnerId;
    this.form.reset({ userId: row.businessProcessOwnerId, positionId: row.positionId });
  }

  cancel(): void {
    this.editingId = undefined;
    this.form.reset({ userId: null, positionId: null });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const selectedUserId = Number(this.form.value.userId);

    const selectedUser = this.users.find(user =>
      user.id === selectedUserId);

    if (!selectedUser) {
      this.error = "Invalid user selection";
      return;
    }

    const payload = {
      businessProcessOwnerId: selectedUserId,
      name: selectedUser.fullName,
      positionId: Number(this.form.value.positionId)
    };

    if (this.editingId == null) {
      // create
      this.svc.create(payload).subscribe({
        next: created => {
          this.owners = [...this.owners, created];
          this.cancel();
        },
        error: () => this.error = 'Create failed'
      });
    } else {
      // updateDraft
      this.svc.update(this.editingId, payload).subscribe({
        next: updated => {
          this.owners = this.owners.map(o =>
            o.businessProcessOwnerId === updated.businessProcessOwnerId ? updated : o);
          this.cancel();
        },
        error: () => this.error = 'Update failed'
      });
    }
  }

  remove(row: ProcessOwner): void {
    if (!confirm(`Delete ${row.name}?`)) return;
    this.svc.delete(row.businessProcessOwnerId).subscribe({
      next: () => this.owners = this.owners.filter(o => o.businessProcessOwnerId !== row.businessProcessOwnerId),
      error: () => this.error = 'Delete failed (possibly in use by an SOP)'
    });
  }

  filtered(): ProcessOwner[] {
    const name = this.filterName.trim().toLowerCase();
    const id = this.filterId && Number.isFinite(this.filterId) ? Number(this.filterId) : undefined;
    return this.owners.filter(o => {
      const nameOk = !name || o.name.toLowerCase().includes(name);
      const idOk = id === undefined || o.businessProcessOwnerId === id;
      return nameOk && idOk;
    });
  }

  trackById(_: number, o: ProcessOwner) { return o.businessProcessOwnerId; }
}
