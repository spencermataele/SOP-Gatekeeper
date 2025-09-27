import { Injectable } from "@angular/core";
import { environment } from "../../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import {OrgDto} from "../models/org.model";

@Injectable({ providedIn: 'root' })
export class OrgService {
  private base = `${environment.apiBaseUrl}/admin/orgs`;

  constructor(private http: HttpClient) {}

  list(): Observable<OrgDto[]> {
    return this.http.get<OrgDto[]>(`${environment.apiBaseUrl}/admin/orgs`);
  }

  create(body: { orgName: string }): Observable<Org> {
    return this.http.post<Org>(this.base, body);
  }
}

export interface Org {
  orgId: number;
  orgName: string;
  orgGroups?: OrgGroup[];
}

export interface OrgGroup {
  orgGroupId: number;
  orgGroupName: string;
}

