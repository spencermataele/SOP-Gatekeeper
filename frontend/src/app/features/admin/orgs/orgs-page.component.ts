import {Component, OnInit} from "@angular/core";
import {CommonModule} from "@angular/common";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {RouterModule} from "@angular/router";
import {Org, OrgService} from "../services/org.service";

@Component({
  selector: 'app-orgs-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
  <h2>Organizations</h2>
  <form [formGroup]="form" (ngSubmit)="save()">
    <label>Org Name <input formControlName="orgName" /></label>
    <button type="submit" [disabled]="form.invalid || saving">Add Org</button>
  </form>
  <div *ngIf="error" class="error">{{ error }}</div>

  <h3>All Orgs</h3>
  <ul>
    <li *ngFor="let org of orgs">
      <strong>{{ org.orgName }}</strong>
      <div *ngIf="org.orgGroups?.length">
        <em>Groups:</em>
        <ul>
          <li *ngFor="let g of org.orgGroups">{{ g.orgGroupName }}</li>
        </ul>
      </div>
      <div *ngIf="!org.orgGroups?.length"><em>No groups yet.</em></div>
    </li>
  </ul>

  <p>
    Manage: <a [routerLink]="['/admin/org-groups']">Org Groups</a> →
    <a [routerLink]="['/admin/departments']">Departments</a> →
    <a [routerLink]="['/admin/dept-subgroups']">Dept Subgroups</a>
  </p>
  `
})
export class OrgsPageComponent implements OnInit {
  form = this.fb.group({ orgName: ['', [Validators.required, Validators.maxLength(255)]] });
  orgs: Org[] = [];
  saving = false;
  error?: string;

  constructor(private fb: FormBuilder, private svc: OrgService) {}

  ngOnInit() { this.refresh(); }

  refresh() {
    this.svc.list().subscribe({
      next: (rows) => this.orgs = rows,
      error: () => this.error = 'Failed to load orgs'
    });
  }

  save() {
    if (this.form.invalid) return;
    this.saving = true;
    this.svc.create({ orgName: this.form.value.orgName! }).subscribe({
      next: () => { this.form.reset(); this.saving = false; this.refresh(); },
      error: () => { this.error = 'Failed to create org'; this.saving = false; }
    });
  }
}
