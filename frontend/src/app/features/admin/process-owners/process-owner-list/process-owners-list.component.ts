import { Component, OnInit } from '@angular/core';
import { ProcessOwnerService} from "../../services/process-owner.service";
import { ProcessOwner } from '../../../sops/models/process-owner.model';
import { Router } from '@angular/router';

@Component({
  selector: 'app-process-owners-list',
  templateUrl: './process-owners-list.component.html',
  styleUrls: ['./process-owners-list.component.scss']
})
export class ProcessOwnersListComponent implements OnInit {
  owners: ProcessOwner[] = [];
  loading = false;
  error?: string;
  q = '';

  constructor(private svc: ProcessOwnerService, private router: Router) {}

  ngOnInit(): void { this.refresh(); }

  refresh(): void {
    this.loading = true;
    this.svc.list().subscribe({
      next: data => { this.owners = data; this.loading = false; },
      error: () => { this.error = 'Failed to load process owners'; this.loading = false; }
    });
  }

  create(): void { this.router.navigate(['/admin/process-owners/new']); }

  remove(row: ProcessOwner): void {
    if (!confirm(`Delete ${row.name}?`)) return;
    this.svc.delete(row.processOwnerId).subscribe({
      next: () => this.refresh(),
      error: () => this.error = 'Delete failed'
    });
  }

  filtered(): ProcessOwner[] {
    const t = this.q.trim().toLowerCase();
    if (!t) return this.owners;
    return this.owners.filter(o =>
      (o.name ?? '').toLowerCase().includes(t) ||
      String(o.positionId ?? '').includes(t)
    );
  }
}
