import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DeptSubgroupDto } from '../models/dept-subgroup.model';

@Injectable({ providedIn: 'root' })
export class DeptSubgroupService {
  private base = `${environment.apiBaseUrl}/admin/dept-subgroups`;

  constructor(private http: HttpClient) {}

  list(): Observable<DeptSubgroupDto[]> {
    return this.http.get<DeptSubgroupDto[]>(this.base);
  }

  // Filter to list by department
  listByDepartment(departmentId: number): Observable<DeptSubgroupDto[]> {
    return this.http.get<DeptSubgroupDto[]>(`${this.base}/by-department/${departmentId}`);
  }

  create(body: { deptSubgroupName: string; departmentId: number }): Observable<DeptSubgroupDto> {
    return this.http.post<DeptSubgroupDto>(this.base, body);
  }

  update(id: number, body: { deptSubgroupName: string; departmentId: number }): Observable<DeptSubgroupDto> {
    return this.http.put<DeptSubgroupDto>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
