import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DeptSubgroup } from '../models/dept-subgroup.model';

@Injectable({ providedIn: 'root' })
export class DeptSubgroupService {
  private base = `${environment.apiBaseUrl}/admin/dept-subgroups`;
  constructor(private http: HttpClient) {}

  list(): Observable<DeptSubgroup[]> {
    return this.http.get<DeptSubgroup[]>(this.base);
  }

  create(body: { deptSubgroupName: string; departmentId: number }): Observable<DeptSubgroup> {
    return this.http.post<DeptSubgroup>(this.base, body);
  }
}
