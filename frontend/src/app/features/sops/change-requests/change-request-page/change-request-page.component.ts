import { Component, OnInit } from '@angular/core';
import {CommonModule} from "@angular/common";
import {Router, RouterModule} from "@angular/router";
import {ChangeRequest} from "../../models/change-request.model";
import {ChangeRequestService} from "../../services/change-request.service";
import {FormsModule} from "@angular/forms";
import {NotificationService} from "../../../notifications/notification.service";
import {error} from "@angular/compiler-cli/src/transformers/util";

@Component({
  selector: 'app-change-request-page',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './change-request-page.component.html',
  styleUrls: ['../../../../../styles.css']
})
export class ChangeRequestPageComponent implements OnInit {

  myRequests: ChangeRequest[] = [];
  pendingApprovals: ChangeRequest[] = [];
  newRequest = {
    sopId: null as number | null,
    summary: '',
    reason: ''
  };

  loading = false;
  errorMsg?: string;

  constructor(
    private changeRequestService: ChangeRequestService,
    private notificationService: NotificationService,
    private router: Router,
  ) {}

  ngOnInit(): void {

    this.load();

    //Mark all unread notifications as read
    this.notificationService.myNotifications().subscribe(list => {

      const unread = list.filter(n => !n.read);

      if (unread.length === 0) return;

      let completed = 0;

      unread.forEach( notification => {
        this.notificationService.markRead(notification.notificationId).subscribe({
          next: () => {
            completed++;

            //after last check, refresh count
            if (completed === unread.length) {
              this.notificationService.unreadCount().subscribe();
            }
          }
        });
      });
    });

  }

  private load(): void {
    this.loading = true;

    //My requests
    this.changeRequestService.listMine().subscribe({
      next: req => this.myRequests = req,
      error: () => this.errorMsg = 'Failed to load your change requests',
      complete: () => this.loading = false
    });

    //My pending approvals
    this.changeRequestService.listPendingApproval().subscribe({
      next: apvl => this.pendingApprovals = apvl,
      error: () => this.errorMsg = 'Failed to load your pending approvals'
    });

  }

  //create
  create() {
    if (!this.newRequest.sopId) return;

    this.loading = true; //Do I need this?

    this.changeRequestService.createDraft(
      this.newRequest.sopId,
      this.newRequest.summary,
      this.newRequest.reason
    ).subscribe({
      next: () => {
        this.newRequest = {
          sopId: null,
          summary: '',
          reason: ''
        };

        this.load();
        this.loading = false;
      },
      error: err => {
        alert(err?.error?.message ?? 'Failed to create new change request');
        this.loading = false;
      }
    });
  }

  //Submit draft
  submit(id: number) {
    //Confirm submit
    const ok = confirm("Submit this change request for approval?");
    if (!ok) return;

    this.changeRequestService.submit(id).subscribe({
      next: () => this.load(),
      error: err => alert(err?.error?.message ?? 'Failed to submit this change request.')
    });
  }

  cancel(id: number) {
    const ok = confirm("Cancel this change request?");
    if (!ok) return;

    this.changeRequestService.cancel(id).subscribe({
      next: () => this.load(),
      error: err =>  alert(err?.error?.message ?? 'Failed to cancel this change request.')
    });
  }

  //Navigate to approval
  goToApproval(id: number) {
    this.router.navigate(['/change-requests', id, 'review']);
  }

}
