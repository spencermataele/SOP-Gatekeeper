import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { OrgDto } from '../models/org.model';
import { OrgGroup } from '../models/org-group.model';
import { OrgService } from '../services/org.service';
import { OrgGroupService } from '../services/org-group.service';

@Component({
  selector: 'app-org-groups-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
  <h2>Org Groups</h2>

  <form [formGroup]="form" (ngSubmit)="save()" class="form">
    <label>Org
      <select formControlName="orgId">
        <option [ngValue]="null">-- select --</option>
        <option *ngFor="let o of orgs" [ngValue]="o.orgId">{{ o.orgName }}</option>
      </select>
    </label>

    <label>Org Group Name
      <input formControlName="orgGroupName" />
    </label>

    <button type="submit" [disabled]="form.invalid || saving">Add Org Group</button>
    <span class="error" *ngIf="error">{{ error }}</span>
  </form>

  <section *ngFor="let group of groupsByOrg">
    <h3>{{ group.org.orgName }}</h3>
    <ul>
      <li *ngFor="let g of group.groups">
        <strong>{{ g.orgGroupName }}</strong>
        <div *ngIf="g.departments?.length; else noDept">
          <em>Departments:</em>
          <ul>
            <li *ngFor="let d of g.departments">{{ d.departmentName }}</li>
          </ul>
        </div>
        <ng-template #noDept><em>No departments yet.</em></ng-template>
      </li>
    </ul>
  </section>
  `,
  styles: [`
    .form { display:flex; gap:12px; align-items:end; flex-wrap:wrap; margin-bottom:1rem; }
    .form label { display:flex; flex-direction:column; gap:4px; }
    .error { color:#b00020; margin-left:.5rem; }
  `]
})
export class OrgGroupsPageComponent implements OnInit {
  orgs: OrgDto[] = [];
  groups: OrgGroup[] = [];
  groupsByOrg: { org: OrgDto; groups: OrgGroup[] }[] = [];
  saving = false;
  error?: string;

  form = this.fb.group({
    orgId: [null as number | null, [Validators.required]],
    orgGroupName: ['', [Validators.required, Validators.maxLength(255)]],
  });

  constructor(
    private fb: FormBuilder,
    private orgSvc: OrgService,
    private svc: OrgGroupService
  ) {}

  ngOnInit(): void {
    this.loadOrgs();
    this.refresh();
  }

  loadOrgs() {
    this.orgSvc.list().subscribe({
      next: rows => this.orgs = rows,
      error: () => this.error = 'Failed to load orgs'
    });
  }

  refresh() {
    this.svc.list().subscribe({
      next: rows => {
        this.groups = rows;
        this.groupsByOrg = this.groupByOrg(rows);
      },
      error: () => this.error = 'Failed to load org groups'
    });
  }

  save() {
    if (this.form.invalid) return;
    this.saving = true;
    const body = {
      orgId: Number(this.form.value.orgId),
      orgGroupName: this.form.value.orgGroupName!
    };
    this.svc.create(body).subscribe({
      next: () => { this.form.reset(); this.saving = false; this.refresh(); },
      error: () => { this.error = 'Failed to create org group'; this.saving = false; }
    });
  }

  private groupByOrg(rows: OrgGroup[]): { org: OrgDto; groups: OrgGroup[] }[] {
    const orgMap = new Map<number, { org: OrgDto; groups: OrgGroup[] }>();
    for (const g of rows) {
      const org = this.orgs.find(o => o.orgId === g.orgId);
      if (!org) continue;
      if (!orgMap.has(org.orgId)) orgMap.set(org.orgId, { org, groups: [] });
      orgMap.get(org.orgId)!.groups.push(g);
    }
    return Array.from(orgMap.values());
  }
}
