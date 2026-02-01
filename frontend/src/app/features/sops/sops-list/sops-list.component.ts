import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {Sop} from "../models/sop.model";
import {SopService} from "../services/sop.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-sops-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sops-list.component.html',
  styleUrls: ['../../../../styles.css']
})
export class SopsListComponent implements OnInit {
  sops: Sop[] = [];
  loading = false;
  error?: string;

  constructor(
    private svc: SopService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    this.svc.list().subscribe({
      next: data => {
        this.sops = data;
        this.loading = false;},
      error: err => { this.error = "Failed to load SOPs."; this.loading = false; }
    });
  }

  create(): void {
    this.router.navigate(['/sops/new']);
  }

  edit(row: Sop): void {
    if (row.sopId != null) this.router.navigate(['/sops', row.sopId, 'edit']);
  }

  delete(row: Sop): void {
    if (row.sopId == null) { return; }
    if (!confirm(`Delete SOP "${row.title}"?`)) { return; }
    this.svc.delete(row.sopId).subscribe({
      next: () => this.refresh(),
      error: () => this.error = 'Delete failed.'
    });
  }

  requestChange(row: Sop): void {
    /*** TODO: Beyond MVP - direct to new cr form with this.sopId ***/
    /*** TODO: Beyond MVP - check to see if there is already a pending change request to this SOP ***/
    /*** TODO: update to /change-request/new when ready ***/
    this.router.navigate(['/change-requests']);
  }

  trackByID(_: number, s: Sop) { return s.sopId ?? -1; }

}
