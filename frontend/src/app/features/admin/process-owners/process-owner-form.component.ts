import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProcessOwnerService } from '../../sops/services/process-owner.service';
import { Router } from '@angular/router';
import { ProcessOwner } from '../../sops/models/process-owner.model';

@Component({
  selector: 'app-process-owner-form',
  templateUrl: './process-owner-form.component.html',
  styleUrls: ['./process-owner-form.component.scss']
})
export class ProcessOwnerFormComponent implements OnInit {
  form!: FormGroup;
  owners: ProcessOwner[] = [];
  loading = false;
  error?: string;

  constructor(
    private fb: FormBuilder,
    private svc: ProcessOwnerService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      processOwnerName: ['', [Validators.required, Validators.maxLength(255)]],
      processOwnerPositionId: [null, [Validators.required]],
      //parent_process_owner_id: [null] // sending as parent object later if needed
    });

    this.svc.list().subscribe({
      next: data => this.owners = data,
      error: () => this.error = 'Failed to load owners'
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    //const parentId = this.form.value.parent_process_owner_id as number | null;
    const body = {
      name: this.form.value.name!,
      positionId: Number(this.form.value.positionId)
    };

    this.loading = true;
    this.svc.create(body).subscribe({ /* ... */ });
  }

  cancel(): void { this.router.navigate(['/admin/process-owners']); }
}
