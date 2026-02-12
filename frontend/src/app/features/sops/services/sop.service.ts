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
    title: string;
    authorId: number | null;
    orgId: number | null;
    orgGroupId: number | null
    departmentId: number | null;
    deptSubgroupId: number | null;
    currentProcessOwnerId: number | null;
    currentProcessOwnerPositionId: number | null;
    processId: number | null;
    processName: string;
    processFamilyId: number | null;
    parentProcessId: number | null;
    versionId: string;
    sopDescription: string;
    sopDetails: string
  }): Observable<Sop> {
    return this.http.post<Sop>(this.base, body);
  }

  updateDraft(id: number, body: {
    title: string;
    orgId: number | null;
    orgGroupId: number | null
    departmentId: number | null;
    deptSubgroupId: number | null;
    currentProcessOwnerId: number | null;
    currentProcessOwnerPositionId: number | null;
    processId: number | null;
    processName: string;
    processFamilyId: number | null;
    parentProcessId: number;
    sopDescription: string;
    sopDetails: string
  }): Observable<Sop> {
    return this.http.put<Sop>(`${this.base}/${id}`, body);
  }
}
