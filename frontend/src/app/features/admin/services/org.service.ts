import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../../environments/environment";
import { OrgDto } from "../models/org.model";

@Injectable({ providedIn: 'root' })
export class OrgService {
  private base = `${environment.apiBaseUrl}/admin/orgs`;

  constructor(private http: HttpClient) {}

  list(): Observable<OrgDto[]> {
    return this.http.get<OrgDto[]>(this.base);
  }

  create(body: { orgName: string }): Observable<OrgDto> {
    return this.http.post<OrgDto>(this.base, body);
  }

  update(id: number, body: { orgName: string }): Observable<OrgDto> {
    return this.http.put<OrgDto>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}


