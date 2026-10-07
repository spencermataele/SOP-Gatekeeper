import {Component, OnInit, ViewChild} from '@angular/core';
import {NotificationService} from "./features/notifications/notification.service";
import {NavigationEnd, Router, RouterOutlet} from "@angular/router";
import {WorkflowComponent} from './features/workflows/workflow.component';
import {filter} from "rxjs";
import {AuthService} from "./features/authorization/auth.service";
import {Location} from "@angular/common";
import {environment} from '../environments/environment';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['../styles.css', './workflow-shell.css', './app.component.css']
})
export class AppComponent implements OnInit {

  unreadCount = 0;
  lifecycleEnabled = environment.lifecycleEnabled;
  @ViewChild(RouterOutlet) outlet?: RouterOutlet;

  constructor(
    private notificationService: NotificationService,
    public authService: AuthService,
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
    if (this.lifecycleEnabled) return;
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
    const active = this.outlet?.isActivated ? this.outlet.component : undefined;
    if (active instanceof WorkflowComponent) {
      if (!active.canLeave()) return;
      active.dirty = false;
    }
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goBack(): void {
    this.location.back();
  }

}
