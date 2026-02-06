import { Component, OnInit } from '@angular/core';
import {Sop} from "../../models/sop.model";
import {ActivatedRoute, Router} from "@angular/router";
import {SopService} from "../../services/sop.service";
import {ChangeRequestService} from "../../services/change-request.service";

@Component({
  selector: 'app-sop-view',
  templateUrl: './sop-view.component.html',
  styleUrls: ['./sop-view.component.css']
})
export class SopViewComponent implements OnInit {

  sop?: Sop;
  loading = false;
  error?: string;

  constructor(
    private route: ActivatedRoute,
    private sopService: SopService,
    private changeRequestService: ChangeRequestService,
    private router: Router
  ) {}

  ngOnInit(): void {

    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error = 'Invalid SOP ID';
      return;
    }

    this.loading = true;

    this.sopService.get(id).subscribe({
      next: sop => {
        this.sop = sop;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load SOP.';
        this.loading = false;
      }
    });
  }

  back(): void {
    this.router.navigate(['/sops']);
  }

  requestChange(): void {
    if (!this.sop?.sopId) return;

    this.changeRequestService.createDraft(
      this.sop.sopId,
      'Change Requested',
      'Initial draft',
    ).subscribe({
      next: changeRequest => {
        this.router.navigate(['/sops', changeRequest.proposedSopId, 'edit']);
      },
      error: () => {
        this.error = 'Failed to create draft.';
      }
    });
  }

}
