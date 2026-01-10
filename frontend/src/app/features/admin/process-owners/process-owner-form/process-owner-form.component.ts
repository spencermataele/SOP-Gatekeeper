import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProcessOwnerService } from '../../services/process-owner.service';
import { Router } from '@angular/router';
import { ProcessOwner } from '../../../sops/models/process-owner.model';

@Component({
  selector: 'app-process-owner-form',
  templateUrl: './process-owner-form.component.html',
  styleUrls: ['./process-owner-form.component.scss']
})
export class ProcessOwnerFormComponent implements OnInit {
  form!: FormGroup;
  owners: ProcessOwner[] = [];
  saving = false;
  error?: string;

  constructor(
    private fb: FormBuilder,
    private svc: ProcessOwnerService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      positionId: [null, [Validators.required]],
      parentProcessOwnerId: [null] // sending as parent object later if needed
    });

    this.svc.list().subscribe({
      next: data => this.owners = data,
      error: () => this.error = 'Failed to load owners'
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const body = {
      name: this.form.value.name!,
      positionId: Number(this.form.value.positionId),
      parentProcessOwnerId: this.form.value.parentProcessOwnerId || null
    };

    this.saving = true;
    this.svc.create(body).subscribe({
      next: () => this.router.navigate(['/admin/process-owners']),
      error: () => this.error = 'Failed to create process owner'
    });
  }

  cancel(): void { this.router.navigate(['/admin/process-owners']); }
}
