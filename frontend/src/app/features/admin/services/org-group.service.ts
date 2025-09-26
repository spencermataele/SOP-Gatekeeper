import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { OrgGroup } from '../models/org-group.model';

@Injectable({ providedIn: 'root' })
export class OrgGroupService {
  private base = `${environment.apiBaseUrl}/admin/org-groups`;
  constructor(private http: HttpClient) {}

  list(): Observable<OrgGroup[]> {
    return this.http.get<OrgGroup[]>(this.base);
  }

  create(body: { orgGroupName: string; orgId: number }): Observable<OrgGroup> {
    return this.http.post<OrgGroup>(this.base, body);
  }
}
