import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ProcessOwnerService } from '../../services/process-owner.service';

@Component({
  selector: 'app-process-owner-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './process-owner-form.component.html',
  styleUrls: ['./process-owner-form.component.scss']
})
export class ProcessOwnerCreateComponent {
  saving = false;
  error?: string;
  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    positionId: [null as number | null, [Validators.required]],
    parentProcessOwnerId: [null as number | null]
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
      positionId: Number(this.form.value.positionId),
      //parentProcessOwnerId: this.form.value.parentProcessOwnerId || null
    };
    this.svc.create(body).subscribe({
      next: (po) => {
        // navigate back and optionally preselect in SOP form
        this.router.navigate(['/sops/new'], { queryParams: { ownerId: po.businessProcessOwnerId } });
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
