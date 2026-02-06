import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {Sop} from "../models/sop.model";
import {SopService} from "../services/sop.service";
import {Router} from "@angular/router";
import {ChangeRequestService} from "../services/change-request.service";

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
    private changeRequestService: ChangeRequestService,
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
        this.loading = false;
        },
      error: () => { this.error = "Failed to load SOPs."; this.loading = false; }
    });
  }

  create(): void {
    this.router.navigate(['/sops/new']);
  }

  view(s: Sop): void {
    if (s.sopId) {
      this.router.navigate(['/sops', s.sopId, 'view']);
    }
  }

  requestChange(row: Sop): void {

    if (!row.sopId) return;

    this.loading = true;

    this.changeRequestService.createDraft(
      row.sopId,
      'Change Requested',
      'Initial draft created'
    ).subscribe({
      next: changeRequest => {
        const proposedSopId = changeRequest.proposedSopId;

        if (!proposedSopId) {
          this.error = "Draft not created.";
          this.loading = false;
          return;
        }

        this.router.navigate(['/sops', proposedSopId, 'edit'], {
          queryParams: { cr: changeRequest.changeRequestId }
        });

      },

      error: () => {
        this.error = "Failed to start change.";
        this.loading = false;
      }

    });
  }

  trackByID(_: number, s: Sop) { return s.sopId ?? -1; }

}
