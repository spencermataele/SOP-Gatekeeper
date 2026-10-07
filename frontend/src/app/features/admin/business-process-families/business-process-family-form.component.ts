import {AdminParentSelectorComponent} from '../admin-parent-selector.component';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BusinessProcessFamilyService } from '../services/business-process-family.service';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from "@angular/common";


@Component({
  selector: 'app-business-process-family-form',
  templateUrl: './business-process-family-form.component.html',
  imports: [
    ReactiveFormsModule,
    CommonModule, AdminParentSelectorComponent
  ],
  standalone: true
})
export class BusinessProcessFamilyFormComponent implements OnInit {
  id?: number;
  loading = false;
  saving = false;
  error?: string;

  form = this.fb.group({
    businessProcessFamilyName: ['', [Validators.required, Validators.maxLength(255)]],
    deptSubgroupId: [null as number | null, Validators.required],
  });

  constructor(
    private fb: FormBuilder,
    private svc: BusinessProcessFamilyService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.id = +idParam;
      this.loading = true;
      this.svc.get(this.id).subscribe({
        next: f => { this.form.patchValue({ businessProcessFamilyName: f.businessProcessFamilyName, deptSubgroupId: f.deptSubgroupId }); this.loading = false; },
        error: () => { this.error = 'Failed to load.'; this.loading = false; }
      });
    }
  }


  save() {
    if (this.form.invalid) return;
    this.saving = true;
    const body = this.form.value as { businessProcessFamilyName: string; deptSubgroupId: number; };

    const req$ = this.id
      ? this.svc.update(this.id, body)
      : this.svc.create(body);

    req$.subscribe({
      next: () => { this.saving = false; this.router.navigate(['/admin/business-process-families']); },
      error: e => { this.saving = false; this.error = e.error?.message || 'Save failed.'; }
    });
  }

  cancel() { this.router.navigate(['/admin/business-process-families']); }
}
