import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { OrgHierarchyRow } from '../models/org-hierarchy-report.model';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private base = `${environment.apiBaseUrl}/reports`;

  constructor(private http: HttpClient) {}

  getOrgHierarchy(params: {
    orgId?: number | null;
    nameLike?: string | null;
    createdFrom?: string | null; // ISO
    createdTo?: string | null;   // ISO
    includeEmptyChildren?: boolean;
  }): Observable<OrgHierarchyRow[]> {
    let p = new HttpParams();
    if (params.orgId != null) p = p.set('orgId', String(params.orgId));
    if (params.nameLike) p = p.set('nameLike', params.nameLike);
    if (params.createdFrom) p = p.set('createdFrom', params.createdFrom);
    if (params.createdTo) p = p.set('createdTo', params.createdTo);
    if (params.includeEmptyChildren !== undefined) p = p.set('includeEmptyChildren', String(params.includeEmptyChildren));
    return this.http.get<OrgHierarchyRow[]>(`${this.base}/org-hierarchy`, { params: p });
  }
}
