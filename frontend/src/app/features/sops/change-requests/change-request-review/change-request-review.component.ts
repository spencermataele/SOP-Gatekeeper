import { Component, OnInit } from '@angular/core';
import {CommonModule} from "@angular/common";
import {ActivatedRoute, Router, RouterModule} from "@angular/router";
import {FormsModule} from "@angular/forms";
import {ChangeRequest} from "../../models/change-request.model";
import {ChangeRequestService} from "../../services/change-request.service";

@Component({
  selector: 'app-change-request-review',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './change-request-review.component.html',
  styleUrls: ['../../../../../styles.css']
})
export class ChangeRequestReviewComponent implements OnInit {

  changeRequest?: ChangeRequest;
  loading = false;
  submitting = false;
  errorMsg?: string;

  comments = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private changeRequestService: ChangeRequestService,
  ) {}

  ngOnInit(): void {
    //Bring up the relative id for THIS change approval
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id) {
      this.errorMsg = 'Invalid Change Request ID.';
      return;
    }

    this.load(id);

  }

  private load(id: number): void {
    this.loading = true;
    this.errorMsg = undefined;

    this.changeRequestService.get(id).subscribe({
      next: changeReq => {
        this.changeRequest = changeReq;
        this.loading = false;
      },
      error: err => {
        console.error(err);
        this.errorMsg = 'Failed to load change request.';
        this.loading = false;
      }
    });
  }

  approve(): void {
    if (!this.changeRequest) return;

    const ok = confirm('Approve and publish this change request');
    if (!ok) return;

    this.submitting = true;

    this.changeRequestService.approve(
      this.changeRequest.changeRequestId,
      this.comments).subscribe({
      next: () => {
        this.router.navigate(['/change-requests']);
      },
      error: err => {
        console.error(err);
        this.errorMsg = err?.error?.message ?? 'Failed to approve change request.';
        this.submitting = false;
      }
    });
  }

  reject(): void{
    if (!this.changeRequest) return;

    const ok = confirm('Reject this change request?');
    if (!ok) return;

    this.submitting = true;

    this.changeRequestService.reject(
      this.changeRequest.changeRequestId,
      this.comments).subscribe({
      next: () => {
        this.router.navigate(['/change-requests']);
      },
      error: err => {
        console.error(err);
        this.errorMsg = err?.error?.message ?? 'Failed to reject change request.';
        this.submitting = false;
      }
    });
  }


}
