import { Injectable } from '@angular/core';
import {environment} from "../../../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Sop} from "../models/sop.model";

@Injectable({
  providedIn: 'root'
})
export class SopService {
  private base = `${environment.apiBaseUrl}/sops`;

  constructor(private http: HttpClient) { }

  list(): Observable<Sop[]> {
    return this.http.get<Sop[]>(this.base);
  }
  get(id: number): Observable<Sop> {
    return this.http.get<Sop>(`${this.base}/${id}`);
  }

  create(body: {
    departmentId: number | null;
    currentProcessOwnerPositionId: number | null;
    processFamilyId: number | null;
    deptSubgroupId: number | null;
    title: string;
    orgId: number | null;
    sopDetails: string;
    versionId: number;
    currentProcessOwnerId: number | null;
    parentProcessId: number | null;
    processId: number | null;
    processName: string;
    orgGroupId: number | null
  }): Observable<Sop> {
    return this.http.post<Sop>(this.base, body);
  }

  update(id: number, body: {
    departmentId: number | null;
    currentProcessOwnerPositionId: number | null;
    processFamilyId: number | null;
    deptSubgroupId: number | null;
    title: string;
    orgId: number | null;
    sopDetails: string;
    versionId: number;
    currentProcessOwnerId: number | null;
    parentProcessId: number | null;
    processId: number | null;
    processName: string;
    orgGroupId: number | null
  }): Observable<Sop> {
    return this.http.put<Sop>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
