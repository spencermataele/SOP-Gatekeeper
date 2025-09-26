import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {SopService} from "../services/sop.service";
import {Sop} from "../models/sop.model";
import {ProcessOwner} from "../models/process-owner.model";
import {ProcessOwnerService} from "../../admin/services/process-owner.service";

@Component({
  selector: 'app-sops-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FormsModule],
  templateUrl: './sops-form.component.html',
  styleUrls: ['./sops-form.component.css']
})
export class SopsFormComponent implements OnInit {
  form!: FormGroup;
  id?: number;
  loading = false;
  error?: string;
  owners: ProcessOwner[] = [];
  nameFilter = '';
  idFilter?: number | null;

  constructor(
    private formbuilder: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private sopService: SopService,
    private processOwnerService: ProcessOwnerService
  ) { }

  ngOnInit(): void {
    this.form = this.formbuilder.group({
      title: ['', [Validators.required, Validators.maxLength(255)]],
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
      sopLocationPath: ['', [Validators.required, Validators.maxLength(255)]],
      versionId: [1, [Validators.required]],
      sopDetails: ['', [Validators.required]],
    });
    // Load process owners
    this.processOwnerService.list().subscribe({
      next: (rows: any[]) => {
        this.owners = rows;

        // If we came back from create page with ownerId, preselect it
        const ownerIdParam = this.route.snapshot.queryParamMap.get('ownerId');
        if (ownerIdParam) {
          const ownerId = Number(ownerIdParam);
          if (ownerId) {
            this.form.patchValue({
              currentProcessOwnerId: ownerId,
              // we don’t know positionId unless we look it up:
              currentProcessOwnerPositionId: rows.find((r: { processOwnerId: number; }) => r.processOwnerId === ownerId)?.positionId ?? this.form.value.currentProcessOwnerPositionId
            });
          }
        }
      },
      error: () => (this.error = 'Failed to load process owners')
    });
    // auto-fill positionId when owner is selected
    this.form.get('currentProcessOwnerId')!.valueChanges.subscribe((id: number | null) => {
      const found = this.owners.find(o => o.processOwnerId === id);
      if (found) this.form.get('currentProcessOwnerPositionId')!.setValue(found.positionId);
    });

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

  // list for dropdown
  get ownersFiltered() {
    const n = this.nameFilter.trim().toLowerCase();
    const id = this.idFilter && Number.isFinite(this.idFilter) ? Number(this.idFilter) : undefined;
    return this.owners.filter(o => {
      const nameOk = !n || o.name.toLowerCase().includes(n);
      const idOk = id === undefined || o.processOwnerId === id;
      return nameOk && idOk;
    });

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
      next: () => this.router.navigate(['/sops']), /* FIXME verify api is needed */
      error: () => { this.error = "Failed to update SOPs."; this.loading = false; }
    });
  }

  cancel(): void {
    this.router.navigate(['/sops']); /* FIXME verify api is needed */
  }

}
