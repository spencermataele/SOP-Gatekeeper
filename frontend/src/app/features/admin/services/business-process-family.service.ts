import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import {
  BusinessProcessFamily,
  BusinessProcessFamilyCreate,
  BusinessProcessFamilyUpdate
} from '../models/business-process-family.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BusinessProcessFamilyService {
  private base = `${environment.apiBaseUrl}/admin/business-process-families`;

  constructor(private http: HttpClient) {}

  list(): Observable<BusinessProcessFamily[]> {
    return this.http.get<BusinessProcessFamily[]>(this.base);
  }

  get(id: number): Observable<BusinessProcessFamily> {
    return this.http.get<BusinessProcessFamily>(`${this.base}/${id}`);
  }

  create(body: BusinessProcessFamilyCreate): Observable<BusinessProcessFamily> {
    return this.http.post<BusinessProcessFamily>(this.base, body);
  }

  update(id: number, body: BusinessProcessFamilyUpdate): Observable<BusinessProcessFamily> {
    return this.http.put<BusinessProcessFamily>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
