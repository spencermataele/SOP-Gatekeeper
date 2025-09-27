import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-reports-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page">
      <h2>Reports</h2>
      <p class="muted">Select a report to view and filter.</p>
      <ul class="cards">
        <li class="card">
          <h3>Org Hierarchy Report</h3>
          <p>Org → Org Group → Department → Subgroup, with timestamps.</p>
          <a class="btn" [routerLink]="['/reports/org-hierarchy']">Open</a>
        </li>
        <!-- placeholder for future cards -->
      </ul>
    </div>
  `,
  styles: [`
    .page { display: grid; gap: 1rem; }
    .muted { color: #6b7280; }
    .cards { list-style: none; padding: 0; display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); }
    .card { border: 1px solid #e5e7eb; border-radius: 12px; padding: 1rem; background: #fff; display: grid; gap: .5rem; }
    .btn { display:inline-block; border:1px solid #111827; border-radius: 8px; padding: .4rem .7rem; text-decoration: none; }
  `]
})
export class ReportsHomeComponent {}
