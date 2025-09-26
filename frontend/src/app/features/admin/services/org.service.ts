import {Injectable} from "@angular/core";
import {environment} from "../../../../environments/environment";
import {HttpClient} from "@angular/common/http";

@Injectable({ providedIn: 'root' })
export class OrgService {
  private base = `${environment.apiBaseUrl}/admin/orgs`;
  constructor(private http: HttpClient) {}
  list() { return this.http.get<Org[]>(this.base); }
  create(body: { orgName: string }) { return this.http.post<Org>(this.base, body); }
}
export interface Org {
  orgId: number;
  orgName: string;
  orgGroups?: OrgGroup[]; // if you serialize children
}
export interface OrgGroup { orgGroupId: number; orgGroupName: string; }
