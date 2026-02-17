import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ReportsService } from './services/reports.service';
import { OrgHierarchyRow } from './models/org-hierarchy-report.model';

@Component({
  selector: 'app-org-hierarchy-report',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="page">
      <div class="header">
        <h2>Org Hierarchy Report</h2>
        <div class="stamp">
          <div>Generated: {{ now | date:'short' }}</div>
        </div>
      </div>

      <!-- Filters -->
      <form [formGroup]="filters" (ngSubmit)="load()" class="filters">
        <label>Org Id
          <input type="number" formControlName="orgId" />
        </label>
        <label>Name contains
          <input type="text" formControlName="nameLike" />
        </label>
        <label>Created From
          <input type="datetime-local" formControlName="createdFrom" />
        </label>
        <label>Created To
          <input type="datetime-local" formControlName="createdTo" />
        </label>
        <label class="chk">
          <input type="checkbox" formControlName="includeEmptyChildren" />
          Include empty children
        </label>
        <button type="submit">Apply</button>
        <button type="button" (click)="reset()">Reset</button>
        <button type="button" (click)="exportCsv()" [disabled] = "true">Export CSV</button>
      </form>

      <!-- Table -->
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Org</th>
              <th>Org Group</th>
              <th>Department</th>
              <th>Subgroup</th>
              <th>Created</th>
              <th>Last Updated</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let r of rows">
              <td>
                <div class="cell-title">{{ r.orgName || '—' }}</div>
                <div class="cell-sub">#{{ r.orgId ?? '—' }}</div>
              </td>
              <td>
                <div class="cell-title">{{ r.orgGroupName || '—' }}</div>
                <div class="cell-sub">#{{ r.orgGroupId ?? '—' }}</div>
              </td>
              <td>
                <div class="cell-title">{{ r.departmentName || '—' }}</div>
                <div class="cell-sub">#{{ r.departmentId ?? '—' }}</div>
              </td>
              <td>
                <div class="cell-title">{{ r.deptSubgroupName || '—' }}</div>
                <div class="cell-sub">#{{ r.deptSubgroupId ?? '—' }}</div>
              </td>
              <td>{{ r.createdTimestamp | date:'short' }}</td>
              <td>{{ r.lastUpdatedTimestamp | date:'short' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="footer">
        <div class="title">Title: Org Hierarchy Report</div>
        <div class="count">{{ rows.length }} row{{ rows.length === 1 ? '' : 's' }}</div>
      </div>
    </div>
  `,
  styles: [`
    .page { display:grid; gap:1rem; }
    .header { display:flex; justify-content:space-between; align-items:end; }
    .stamp { color:#6b7280; font-size:.9rem; }
    .filters { display:flex; gap:12px; align-items:end; flex-wrap:wrap; }
    .filters label { display:flex; flex-direction:column; gap:4px; }
    .filters .chk { flex-direction:row; gap:8px; align-items:center; }
    .table-wrap { overflow:auto; border:1px solid #e5e7eb; border-radius:10px; }
    table { width:100%; border-collapse:collapse; font-size:14px; background:#fff; }
    thead th { text-align:left; background:#f9fafb; border-bottom:1px solid #e5e7eb; padding:.5rem .75rem; white-space:nowrap; }
    tbody td { border-bottom:1px solid #f3f4f6; padding:.5rem .75rem; vertical-align:top; }
    .cell-title { font-weight:600; }
    .cell-sub { color:#6b7280; font-size:.85rem; }
    .footer { display:flex; justify-content:space-between; color:#6b7280; }
  `]
})
export class OrgHierarchyReportComponent implements OnInit {
  now = new Date();
  rows: OrgHierarchyRow[] = [];

  filters = this.fb.group({
    orgId: [null as number | null],
    nameLike: [''],
    createdFrom: [''], // yyyy-MM-ddTHH:mm
    createdTo: [''],
    includeEmptyChildren: [true]
  });

  constructor(private fb: FormBuilder, private svc: ReportsService) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    const f = this.filters.value;
    const params = {
      orgId: f.orgId ?? undefined,
      nameLike: f.nameLike || undefined,
      createdFrom: f.createdFrom ? new Date(f.createdFrom!).toISOString() : undefined,
      createdTo: f.createdTo ? new Date(f.createdTo!).toISOString() : undefined,
      includeEmptyChildren: f.includeEmptyChildren ?? true
    };
    this.svc.getOrgHierarchy(params).subscribe({
      next: rows => this.rows = rows,
      error: err => console.error('Report load failed', err)
    });
  }

  reset() {
    this.filters.reset({ orgId: null, nameLike: '', createdFrom: '', createdTo: '', includeEmptyChildren: true });
    this.load();
  }

  exportCsv() {
    if (!this.rows.length) return;
    const header = [
      'orgId','orgName','orgGroupId','orgGroupName','departmentId','departmentName',
      'deptSubgroupId','deptSubgroupName','createdTimestamp','lastUpdatedTimestamp'
    ];
    const lines = [header.join(',')];
    for (const r of this.rows) {
      lines.push([
        r.orgId ?? '', wrap(r.orgName),
        r.orgGroupId ?? '', wrap(r.orgGroupName),
        r.departmentId ?? '', wrap(r.departmentName),
        r.deptSubgroupId ?? '', wrap(r.deptSubgroupName),
        r.createdTimestamp, r.lastUpdatedTimestamp
      ].join(','));
    }
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `org-hierarchy-${new Date().toISOString().slice(0,19).replace(/[:T]/g,'-')}.csv`;
    a.click(); URL.revokeObjectURL(url);

    function wrap(s: string | null): string {
      if (!s && s !== '') return '';
      const needs = /[",\n,]/.test(s);
      const v = s.replace(/"/g, '""');
      return needs ? `"${v}"` : v;
    }
  }
}
