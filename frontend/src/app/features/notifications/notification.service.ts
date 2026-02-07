import {Injectable} from "@angular/core";
import {environment} from "../../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";


export interface NotificationDto {
  notificationId: number;
  changeRequestId: number;
  userId: number;
  createdTimestamp: string;
  notificationType: string;
  read: boolean
}

@Injectable(
  {providedIn: 'root'}
)
export class NotificationService {

  private base = `${environment.apiBaseUrl}/notifications`;

  constructor(private http: HttpClient) {}

  unreadCount(): Observable<number> {
    return this.http.get<number>(`${this.base}/mine/unread-count`);
  }

  myNotifications(): Observable<NotificationDto[]> {
    return this.http.get<NotificationDto[]>(`${this.base}/mine`);
  }

  markRead(id: number) {
    return this.http.post(`${this.base}/${id}/read`, {});
  }

}
