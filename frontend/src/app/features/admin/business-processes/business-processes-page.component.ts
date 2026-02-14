import { Component, OnInit } from '@angular/core';
import { BusinessProcess } from '../models/business-process.model';
import { BusinessProcessService } from '../services/business-process.service';
import {Router, RouterLink, RouterModule} from '@angular/router';
import {CommonModule, DatePipe} from "@angular/common";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";

@Component({
  selector: 'app-business-processes-page',
  standalone: true,
  templateUrl: './business-processes-page.component.html',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    RouterModule,
    ReactiveFormsModule,
    CommonModule
  ],
})
export class BusinessProcessesPageComponent implements OnInit {
  rows: BusinessProcess[] = [];
  loading = false;
  q = '';

  constructor(
    private svc: BusinessProcessService,
    private router: Router
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.svc.list().subscribe({
      next: data => {
        this.rows = data;
        this.loading = false;
        },
      error: () => {
        this.loading = false;
      }
    });
  }

  create() {
    this.router.navigate(['/admin/business-processes/new']);
  }
  edit(id: number) {
    this.router.navigate(['/admin/business-processes', id]);
  }

  remove(id: number, name: string) {
    if (!confirm(`Delete business process "${name}"?`)) return;
    this.svc.delete(id).subscribe({ next: () => this.load() });
  }

  get filtered() {
    const q = this.q.trim().toLowerCase();
    if (!q) return this.rows;
    return this.rows.filter(r =>
      (r.businessProcessName || '').toLowerCase().includes(q) ||
      (r.businessProcessFamilyName || '').toLowerCase().includes(q) ||
      (r.parentProcessName || '').toLowerCase().includes(q)
    );
  }
}
