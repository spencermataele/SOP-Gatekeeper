import {Component, OnInit} from '@angular/core';
import {NotificationService} from "./features/notifications/notification.service";
import {NavigationEnd, Router} from "@angular/router";
import {filter} from "rxjs";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  unreadCount = 0;

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)).subscribe(
        () => this.loadUnread()
    );
  }

  private loadUnread() {
    //If not logged in yet.  i.e. landing on the login page
    if (!localStorage.getItem('token')) {
      this.unreadCount = 0;
      return;
    }
    //Landing anywhere else
    this.notificationService.unreadCount().subscribe({
      next: count => this.unreadCount = count,
      error: () => this.unreadCount = 0
    });
  }

  goToChangeRequest() {
    this.router.navigate(['/change-requests']);
  }

}
