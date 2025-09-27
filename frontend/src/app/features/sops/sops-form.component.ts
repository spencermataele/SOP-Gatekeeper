import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';

import { OrgService } from "../admin/services/org.service";
import { OrgGroupService } from '../admin/services/org-group.service';
import { DepartmentService } from '../admin/services/department.service';
import { DeptSubgroupService } from '../admin/services/dept-subgroup.service';
import { ProcessOwnerService } from '../admin/services/process-owner.service';

import { OrgDto } from '../admin/models/org.model';
import { OrgGroupDto } from '../admin/models/org-group.model';
import { DepartmentDto } from '../admin/models/department.model';
import { DeptSubgroupDto } from '../admin/models/dept-subgroup.model';
import { ProcessOwner } from './models/process-owner.model';

import { SopService } from './services/sop.service';

@Component({
  selector: 'app-sop-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './sops-form.component.html',
  styleUrls: ['./sops-form.component.css']
})
export class SopsFormComponent implements OnInit {
  // edit mode
  id: number | null = null;
  loading = false;
  saving = false;

  // master lists
  orgs: OrgDto[] = [];
  groups: OrgGroupDto[] = [];
  depts: DepartmentDto[] = [];
  subs: DeptSubgroupDto[] = [];
  owners: ProcessOwner[] = [];

  // id->entity maps for autofill logic
  orgById = new Map<number, OrgDto>();
  groupById = new Map<number, OrgGroupDto>();
  deptById = new Map<number, DepartmentDto>();
  subgroupById = new Map<number, DeptSubgroupDto>();
  ownerById = new Map<number, ProcessOwner>();

  // global filter
  nameFilter = this.fb.control<string>('', { nonNullable: true });

  form = this.fb.group({
    // core SOP fields
    title: ['', [Validators.required, Validators.maxLength(255)]],
    sopDetails: ['', [Validators.required]],
    authorName: ['', [Validators.required, Validators.maxLength(255)]],
    processName: ['', [Validators.required, Validators.maxLength(255)]],
    processId: [null as number | null],
    processFamilyId: [null as number | null],
    parentProcessId: [null as number | null],
    versionId: [1, [Validators.min(1)]],

    // hierarchy (optional but selectable)
    orgId: [null as number | null],
    orgGroupId: [null as number | null],
    departmentId: [null as number | null],
    deptSubgroupId: [null as number | null],

    // process owner
    processOwnerId: [null as number | null]
  });

  errorMsg?: string;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private sopSvc: SopService,
    private orgSvc: OrgService,
    private groupSvc: OrgGroupService,
    private deptSvc: DepartmentService,
    private subSvc: DeptSubgroupService,
    private ownerSvc: ProcessOwnerService
  ) {}

  ngOnInit(): void {
    // detect edit mode
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') this.id = Number(idParam);

    // load reference data
    this.orgSvc.list().subscribe({ next: rows => { this.orgs = rows; rows.forEach(o => this.orgById.set(o.orgId, o)); }});
    this.groupSvc.list().subscribe({ next: rows => { this.groups = rows; rows.forEach(g => this.groupById.set(g.orgGroupId, g)); }});
    this.deptSvc.list().subscribe({ next: rows => { this.depts = rows; rows.forEach(d => this.deptById.set(d.departmentId, d)); }});
    this.subSvc.list().subscribe({ next: rows => { this.subs = rows; rows.forEach(s => this.subgroupById.set(s.deptSubgroupId, s)); }});
    this.ownerSvc.list().subscribe({ next: rows => { this.owners = rows; rows.forEach(p => this.ownerById.set(p.processOwnerId, p)); }});

    if (this.id != null) {
      this.loading = true;
      this.sopSvc.get(this.id).subscribe({
        next: (sop: any) => {
          this.form.patchValue({
            title: sop.title ?? '',
            sopDetails: sop.sopDetails ?? '',
            authorName: sop.authorName ?? '',
            processName: sop.processName ?? '',
            processId: sop.processId ?? null,
            processFamilyId: sop.processFamilyId ?? null,
            parentProcessId: sop.parentProcessId ?? null,
            versionId: sop.versionId ?? 1,
            orgId: sop.orgId ?? null,
            orgGroupId: sop.orgGroupId ?? null,
            departmentId: sop.departmentId ?? null,
            deptSubgroupId: sop.deptSubgroupId ?? null,
            processOwnerId: sop.processOwnerId ?? null
          });
          // ensure parent chain is consistent if only a child id is present
          this.onSubChange();
          this.loading = false;
        },
        error: err => { this.errorMsg = err?.error?.message ?? 'Failed to load SOP'; this.loading = false; }
      });
    }
  }

  //  global filter (applies to all dropdowns)
  private match(s: string | null | undefined): boolean {
    const q = this.nameFilter.value.trim().toLowerCase();
    if (!q) return true;
    return (s ?? '').toLowerCase().includes(q);
  }

  filteredOrgs(): OrgDto[] {
    return this.orgs.filter(o => this.match(o.orgName));
  }
  filteredGroups(): OrgGroupDto[] {
    const orgId = this.form.value.orgId ?? null;
    return this.groups.filter(g => (!orgId || g.orgId === orgId) && this.match(g.orgGroupName));
  }
  filteredDepts(): DepartmentDto[] {
    const groupId = this.form.value.orgGroupId ?? null;
    return this.depts.filter(d => (!groupId || d.orgGroupId === groupId) && this.match(d.departmentName));
  }
  filteredSubs(): DeptSubgroupDto[] {
    const deptId = this.form.value.departmentId ?? null;
    return this.subs.filter(s => (!deptId || s.departmentId === deptId) && this.match(s.deptSubgroupName));
  }
  filteredOwners(): ProcessOwner[] {
    return this.owners.filter(p => this.match(p.name));
  }

  // autofill parents
  onOrgChange() {
    const orgId = this.form.value.orgId ?? null;

    const group = this.form.value.orgGroupId ? this.groupById.get(this.form.value.orgGroupId) : undefined;
    if (group && group.orgId !== orgId) {
      this.form.patchValue({ orgGroupId: null, departmentId: null, deptSubgroupId: null });
      return;
    }

    const dept = this.form.value.departmentId ? this.deptById.get(this.form.value.departmentId) : undefined;
    if (dept) {
      const deptGroup = this.groupById.get(dept.orgGroupId);
      if (!deptGroup || deptGroup.orgId !== orgId) {
        this.form.patchValue({ orgGroupId: null, departmentId: null, deptSubgroupId: null });
        return;
      }
    }

    const sub = this.form.value.deptSubgroupId ? this.subgroupById.get(this.form.value.deptSubgroupId) : undefined;
    if (sub) {
      const subDept = this.deptById.get(sub.departmentId);
      const subGroup = subDept ? this.groupById.get(subDept.orgGroupId) : undefined;
      if (!subDept || !subGroup || subGroup.orgId !== orgId) {
        this.form.patchValue({ orgGroupId: null, departmentId: null, deptSubgroupId: null });
      }
    }
  }

  onGroupChange() {
    const groupId = this.form.value.orgGroupId ?? null;
    const group = groupId ? this.groupById.get(groupId) : undefined;

    if (group && this.form.value.orgId !== group.orgId) {
      this.form.patchValue({ orgId: group.orgId });
    }

    const dept = this.form.value.departmentId ? this.deptById.get(this.form.value.departmentId) : undefined;
    if (dept && dept.orgGroupId !== groupId) {
      this.form.patchValue({ departmentId: null, deptSubgroupId: null });
      return;
    }

    const sub = this.form.value.deptSubgroupId ? this.subgroupById.get(this.form.value.deptSubgroupId) : undefined;
    if (sub) {
      const subDept = this.deptById.get(sub.departmentId);
      if (!subDept || subDept.orgGroupId !== groupId) {
        this.form.patchValue({ deptSubgroupId: null });
      }
    }
  }

  onDeptChange() {
    const deptId = this.form.value.departmentId ?? null;
    const dept = deptId ? this.deptById.get(deptId) : undefined;

    if (dept) {
      if (this.form.value.orgGroupId !== dept.orgGroupId) {
        this.form.patchValue({ orgGroupId: dept.orgGroupId });
      }
      const group = this.groupById.get(dept.orgGroupId);
      if (group && this.form.value.orgId !== group.orgId) {
        this.form.patchValue({ orgId: group.orgId });
      }
    }

    const sub = this.form.value.deptSubgroupId ? this.subgroupById.get(this.form.value.deptSubgroupId) : undefined;
    if (sub && sub.departmentId !== deptId) {
      this.form.patchValue({ deptSubgroupId: null });
    }
  }

  onSubChange() {
    const subId = this.form.value.deptSubgroupId ?? null;
    const sub = subId ? this.subgroupById.get(subId) : undefined;

    if (sub) {
      const dept = this.deptById.get(sub.departmentId);
      if (dept) {
        if (this.form.value.departmentId !== dept.departmentId) {
          this.form.patchValue({ departmentId: dept.departmentId });
        }
        const group = this.groupById.get(dept.orgGroupId);
        if (group) {
          if (this.form.value.orgGroupId !== group.orgGroupId) {
            this.form.patchValue({ orgGroupId: group.orgGroupId });
          }
          if (this.form.value.orgId !== group.orgId) {
            this.form.patchValue({ orgId: group.orgId });
          }
        }
      }
    }
  }

  //  submit, update, delete
  submit() {
    if (this.form.invalid) return;
    this.saving = true;

    const f = this.form.value;
    const body = {
      title: f.title!,
      sopDetails: f.sopDetails!,
      authorName: f.authorName!,
      processName: f.processName!,
      processId: f.processId ?? null,
      processFamilyId: f.processFamilyId ?? null,
      parentProcessId: f.parentProcessId ?? null,
      versionId: Number(f.versionId ?? 1),

      orgId: f.orgId ?? null,
      orgGroupId: f.orgGroupId ?? null,
      departmentId: f.departmentId ?? null,
      deptSubgroupId: f.deptSubgroupId ?? null,

      processOwnerId: f.processOwnerId ?? null
    };

    const obs = this.id == null
      ? this.sopSvc.create(body)
      : this.sopSvc.update(this.id, body);

    obs.subscribe({
      next: () => {
        this.saving = false;
        // navigate back to SOPs list or detail as you prefer
        this.router.navigate(['/sops']);
      },
      error: err => {
        this.saving = false;
        const apiErrors = err?.error?.errors;
        if (apiErrors) {
          Object.entries(apiErrors).forEach(([field, messages]) => {
            const ctrl = this.form.get(field as string);
            if (ctrl) ctrl.setErrors({ api: (messages as string[]).join(' ') });
          });
        } else {
          this.errorMsg = err?.error?.message ?? 'Failed to save SOP';
        }
      }
    });
  }

  delete() {
    if (this.id == null) return;
    const ok = confirm('Delete this SOP?');
    if (!ok) return;

    this.sopSvc.delete(this.id).subscribe({
      next: () => this.router.navigate(['/sops']),
      error: err => alert(err?.error?.message ?? 'Failed to delete SOP')
    });
  }
}

