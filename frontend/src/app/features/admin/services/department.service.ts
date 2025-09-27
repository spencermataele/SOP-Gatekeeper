import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DepartmentDto } from '../models/department.model';

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private base = `${environment.apiBaseUrl}/admin/departments`;

  constructor(private http: HttpClient) {}

  list(): Observable<DepartmentDto[]> {
    return this.http.get<DepartmentDto[]>(this.base);
  }

  create(body: { departmentName: string; orgGroupId: number }): Observable<DepartmentDto> {
    return this.http.post<DepartmentDto>(this.base, body);
  }

  update(id: number, body: { departmentName: string; orgGroupId: number }): Observable<DepartmentDto> {
    return this.http.put<DepartmentDto>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
