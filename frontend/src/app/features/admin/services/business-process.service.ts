import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import {
  BusinessProcess,
  BusinessProcessCreate,
  BusinessProcessUpdate
} from '../models/business-process.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BusinessProcessService {
  private base = `${environment.apiBaseUrl}/admin/business-processes`;

  constructor(private http: HttpClient) {}

  list(params?: { familyId?: number }): Observable<BusinessProcess[]> {
    let httpParams = new HttpParams();
    if (params?.familyId != null) httpParams = httpParams.set('businessProcessFamilyId', params.familyId);
    return this.http.get<BusinessProcess[]>(this.base, { params: httpParams });
  }

  get(id: number): Observable<BusinessProcess> {
    return this.http.get<BusinessProcess>(`${this.base}/${id}`);
  }

  create(body: BusinessProcessCreate): Observable<BusinessProcess> {
    return this.http.post<BusinessProcess>(this.base, body);
  }

  update(id: number, body: BusinessProcessUpdate): Observable<BusinessProcess> {
    return this.http.put<BusinessProcess>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
