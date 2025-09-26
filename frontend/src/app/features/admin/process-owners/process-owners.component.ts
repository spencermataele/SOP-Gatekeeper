import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProcessOwnerService } from '../services/process-owner.service';
import { ProcessOwner } from '../../sops/models/process-owner.model';

@Component({
  selector: 'app-process-owners',
  templateUrl: './process-owners.component.html',
  styleUrls: ['./process-owners.component.scss']
})
export class ProcessOwnersComponent implements OnInit {
  owners: ProcessOwner[] = [];
  loading = false;
  error?: string;

  // form state
  form!: FormGroup;
  editingId?: number;

  // filters
  filterName = '';
  filterId?: number | null;

  constructor(private fb: FormBuilder, private svc: ProcessOwnerService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      positionId: [null, [Validators.required]]
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
    this.form.reset({ name: '', positionId: null });
  }

  startEdit(row: ProcessOwner): void {
    this.editingId = row.processOwnerId;
    this.form.reset({ name: row.name, positionId: row.positionId });
  }

  cancel(): void {
    this.editingId = undefined;
    this.form.reset({ name: '', positionId: null });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const payload = {
      name: String(this.form.value.name).trim(),
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
      // update
      this.svc.update(this.editingId, payload).subscribe({
        next: updated => {
          this.owners = this.owners.map(o => o.processOwnerId === updated.processOwnerId ? updated : o);
          this.cancel();
        },
        error: () => this.error = 'Update failed'
      });
    }
  }

  remove(row: ProcessOwner): void {
    if (!confirm(`Delete ${row.name}?`)) return;
    this.svc.delete(row.processOwnerId).subscribe({
      next: () => this.owners = this.owners.filter(o => o.processOwnerId !== row.processOwnerId),
      error: () => this.error = 'Delete failed (possibly in use by an SOP)'
    });
  }

  filtered(): ProcessOwner[] {
    const name = this.filterName.trim().toLowerCase();
    const id = this.filterId && Number.isFinite(this.filterId) ? Number(this.filterId) : undefined;
    return this.owners.filter(o => {
      const nameOk = !name || o.name.toLowerCase().includes(name);
      const idOk = id === undefined || o.processOwnerId === id;
      return nameOk && idOk;
    });
  }

  trackById(_: number, o: ProcessOwner) { return o.processOwnerId; }
}
