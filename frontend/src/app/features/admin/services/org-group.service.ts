import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { OrgGroupDto } from '../models/org-group.model';

@Injectable({ providedIn: 'root' })
export class OrgGroupService {
  private base = `${environment.apiBaseUrl}/admin/org-groups`;
  constructor(private http: HttpClient) {}

  list(): Observable<OrgGroupDto[]> {
    return this.http.get<OrgGroupDto[]>(this.base);
  }

  create(body: { orgGroupName: string; orgId: number }) {
    return this.http.post<OrgGroupDto>(this.base, body);
  }

  update(id: number, body: { orgGroupName: string; orgId: number }): Observable<OrgGroupDto> {
    return this.http.put<OrgGroupDto>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
