import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ProcessOwnerService } from '../../admin/services/process-owner.service';

@Component({
  selector: 'app-process-owner-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
  <div class="container">
    <h2>Create Process Owner</h2>

    <form [formGroup]="form" (ngSubmit)="save()">
      <label>Name
        <input type="text" formControlName="name" />
      </label>

      <label>Position ID
        <input type="number" formControlName="positionId" />
      </label>

      <div class="actions">
        <button type="submit" [disabled]="form.invalid || saving">Save</button>
        <button type="button" (click)="cancel()">Cancel</button>
      </div>

      <div *ngIf="error" class="error">{{ error }}</div>
    </form>
  </div>
  `,
  styles: [`.container{max-width:720px;margin:1rem auto;display:block} .actions{margin-top:1rem;display:flex;gap:.5rem}`]
})
export class ProcessOwnerCreateComponent {
  saving = false;
  error?: string;
  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    positionId: [null as number | null, [Validators.required]]
  });

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private svc: ProcessOwnerService
  ) {}

  save() {
    if (this.form.invalid) return;
    this.saving = true;
    const body = {
      name: this.form.value.name!,
      positionId: Number(this.form.value.positionId)
    };
    this.svc.create(body).subscribe({
      next: (po) => {
        // navigate back and optionally preselect in SOP form
        this.router.navigate(['/sops/new'], { queryParams: { ownerId: po.processOwnerId } });
      },
      error: () => this.error = 'Failed to create process owner'
    });
  }

  cancel() {
    const backTo = this.route.snapshot.queryParamMap.get('backTo');
    if (backTo) { this.router.navigate([backTo]); }
    else { this.router.navigate(['/']); }
  }
}
