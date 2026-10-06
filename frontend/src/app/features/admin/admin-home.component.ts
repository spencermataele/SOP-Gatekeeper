import {Component} from "@angular/core";
import {RouterLinkWithHref} from "@angular/router";
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-admin-home',
  templateUrl: './admin-home.component.html',
  styleUrls: ['../../../styles.css']
})
export class AdminHomeComponent { lifecycleEnabled=environment.lifecycleEnabled; }
