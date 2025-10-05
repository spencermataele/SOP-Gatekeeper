import { Component, OnInit } from '@angular/core';
import { CommonModule } from "@angular/common";
import { BusinessProcessFamily } from '../models/business-process-family.model';
import { BusinessProcessFamilyService } from '../services/business-process-family.service';
import { Router, RouterLink } from '@angular/router';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import { DatePipe } from "@angular/common";

@Component({
  selector: 'app-business-process-families-page',
  templateUrl: './business-process-families-page.component.html',
  imports: [
    FormsModule,
    DatePipe,
    RouterLink,
    CommonModule,
    ReactiveFormsModule
  ],
  standalone: true,
  styles: [`
    .form { display:flex; gap:12px; align-items:end; flex-wrap:wrap; margin-bottom:1rem; }
    .form label { display:flex; flex-direction:column; gap:4px; }
    .error { color:#b00020; margin-left:.5rem; }

    .list { list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:.5rem; }
    .row { display:flex; justify-content:space-between; align-items:center; gap:12px; padding:.75rem 1rem; border:1px solid #e5e7eb; border-radius:10px; background:#fff; }
    .row.edit { gap:8px; }
    .muted { color:#6b7280; font-size:.9rem; }
    .actions { display:flex; gap:.5rem; }
    .danger { color:#b00020; }
    .footer { margin-top:1rem; color:#6b7280; }
  `]

})
export class BusinessProcessFamiliesPageComponent implements OnInit {
  rows: BusinessProcessFamily[] = [];
  loading = false;
  q = '';

  constructor(private svc: BusinessProcessFamilyService, private router: Router) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.svc.list().subscribe({
      next: data => { this.rows = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  create() { this.router.navigate(['/admin/business-process-families/new']); }
  edit(id: number) { this.router.navigate(['/admin/business-process-families', id]); }

  remove(id: number, name: string) {
    if (!confirm(`Delete business process family "${name}"?`)) return;
    this.svc.delete(id).subscribe({ next: () => this.load() });
  }

  get filtered() {
    const q = this.q.trim().toLowerCase();
    if (!q) return this.rows;
    return this.rows.filter(r => (r.businessProcessFamilyName || '').toLowerCase().includes(q) ||
      (r.departmentName || '').toLowerCase().includes(q));
  }
}
