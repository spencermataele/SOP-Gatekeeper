import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BusinessProcessFamilyService } from '../services/business-process-family.service';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { CommonModule } from "@angular/common";

interface DepartmentLite { departmentId: number; departmentName: string; }

@Component({
  selector: 'app-business-process-family-form',
  templateUrl: './business-process-family-form.component.html',
  imports: [
    ReactiveFormsModule,
    CommonModule
  ],
  standalone: true
})
export class BusinessProcessFamilyFormComponent implements OnInit {
  id?: number;
  loading = false;
  saving = false;
  error?: string;
  departments: DepartmentLite[] = [];

  form = this.fb.group({
    businessProcessFamilyName: ['', [Validators.required, Validators.maxLength(255)]],
    departmentId: [null as number | null, Validators.required],
  });

  constructor(
    private fb: FormBuilder,
    private svc: BusinessProcessFamilyService,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadDeps();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.id = +idParam;
      this.loading = true;
      this.svc.get(this.id).subscribe({
        next: f => { this.form.patchValue({ businessProcessFamilyName: f.businessProcessFamilyName, departmentId: f.departmentId }); this.loading = false; },
        error: () => { this.error = 'Failed to load.'; this.loading = false; }
      });
    }
  }

  loadDeps() {
    this.http.get<DepartmentLite[]>(`${environment.apiBaseUrl}/admin/departments`)
      .subscribe({ next: d => this.departments = d });
  }

  save() {
    if (this.form.invalid) return;
    this.saving = true;
    const body = this.form.value as { businessProcessFamilyName: string; departmentId: number; };

    const req$ = this.id
      ? this.svc.update(this.id, body)
      : this.svc.create(body);

    req$.subscribe({
      next: () => { this.saving = false; this.router.navigate(['/admin/business-process-families']); },
      error: () => { this.saving = false; this.error = 'Save failed.'; }
    });
  }

  cancel() { this.router.navigate(['/admin/business-process-families']); }
}
