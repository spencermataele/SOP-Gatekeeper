import {Component, OnInit} from '@angular/core';
import {NotificationService} from "./features/notifications/notification.service";
import {NavigationEnd, Router} from "@angular/router";
import {filter} from "rxjs";
import {AuthService} from "./features/authorization/auth.service";
import {Location} from "@angular/common";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['../styles.css']
})
export class AppComponent implements OnInit {

  unreadCount = 0;

  constructor(
    private notificationService: NotificationService,
    private authService: AuthService,
    private router: Router,
    private location: Location
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

  public logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goBack(): void {
    this.location.back();
  }

}
