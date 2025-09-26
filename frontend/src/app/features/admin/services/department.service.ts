import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Department } from '../models/department.model';

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private base = `${environment.apiBaseUrl}/admin/departments`;
  constructor(private http: HttpClient) {}

  list(): Observable<Department[]> {
    return this.http.get<Department[]>(this.base);
  }

  create(body: { departmentName: string; orgGroupId: number }): Observable<Department> {
    return this.http.post<Department>(this.base, body);
  }
}
