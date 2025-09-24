import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {SopService} from "../services/sop.service";
import {Sop} from "../models/sop.model";

@Component({
  selector: 'app-sops-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './sops-form.component.html',
  styleUrls: ['./sops-form.component.css']
})
export class SopsFormComponent implements OnInit {
  form!: FormGroup;
  id?: number;
  loading = false;
  error?: string;

  constructor(
    private formbuilder: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private sopService: SopService
  ) { }

  ngOnInit(): void {
    this.form = this.formbuilder.group({
      title: ["", [Validators.required, Validators.maxLength(255)]],
      authorId: [null, [Validators.required]],
      orgId: [null, [Validators.required]],
      orgGroupId: [null, [Validators.required]],
      departmentId: [null, [Validators.required]],
      deptSubgroupId: [null, [Validators.required]],
      currentProcessOwnerId: [null, [Validators.required]],
      currentProcessOwnerPositionId: [null, [Validators.required]],
      processId: [null, [Validators.required]],
      processName: [null, [Validators.required]],
      processFamilyId: [null, [Validators.required]],
      parentProcessId: [null, [Validators.required]],
      sopLocationPath: ["", [Validators.required, Validators.maxLength(255)]],
      versionId: [1, [Validators.required]],
      sopDetails: [null, [Validators.required]],
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.id = Number(idParam);
      this.loading = true;
      this.sopService.get(this.id).subscribe({
        next: sop => {
          this.form.patchValue({
            ...sop,
            versionId: Number(sop.versionId ?? 1)
          });
          this.loading = false;
        },
        error: () => { this.error = 'Failed to load SOPs.'; this.loading = false; }
      });
    }
  }

  private coerceNumbers(raw: any): any {
    const numberKeys = [
      'authorId','orgId','orgGroupId','departmentId','deptSubgroupId',
      'currentProcessOwnerId','currentProcessOwnerPositionId',
      'processId','processFamilyId','parentProcessId','versionId'
    ];
    const out: any = { ...raw };
    numberKeys.forEach(key => { if (out[key] !== null && out[key] !== '')
      out[key] = Number(out[key]); });
    return out;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched(); return; }
    const payload: Sop = this.coerceNumbers(this.form.value);

    this.loading = true;
    const req = this.id
    ? this.sopService.update(this.id, payload)
      :this.sopService.create(payload);

    req.subscribe({
      next: () => this.router.navigate(['/api/sops']), /* FIXME verify api is needed */
      error: () => { this.error = "Failed to update SOPs."; this.loading = false; }
    });
  }

  cancel(): void {
    this.router.navigate(['/api/sops']); /* FIXME verify api is needed */
  }

}
