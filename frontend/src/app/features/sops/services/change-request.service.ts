import {Observable} from "rxjs";
import {Sop} from "../models/sop.model";
import {environment} from "../../../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {ChangeRequest} from "../models/change-request.model";

@Injectable({
  providedIn: 'root'
})
export class ChangeRequestService {

  constructor(private http: HttpClient) { }

  // Create new
  createDraft(sopId: number, summary: string, reason: string) {
    return this.http.post<number>(
      `${environment.apiBaseUrl}/change-requests`, null, {
        params: {
          sopId,
          summary,
          reason
        }
      }
    );
  }

  // Publish upon approval
  publishChangeRequest(
    changeRequestId: number,
    body: {
      title: string;
      orgId: number | null;
      orgGroupId: number | null;
      departmentId: number | null;
      deptSubgroupId: number | null;
      currentProcessOwnerId: number | null;
      processId: number | null;
      processName: string;
      processFamilyId: number | null;
      parentProcessId: number;
      sopDescription: string;
      sopDetails: string;
    }
  ): Observable<Sop> {

    return this.http.post<Sop>(
      `${environment.apiBaseUrl}/change-requests/${changeRequestId}/publish`,
      body
    );
  }

  // list of my requests
  listMine() {
    return this.http.get<ChangeRequest[]>(
      `${environment.apiBaseUrl}/change-requests/mine`
    );
  }

  // list of my pending approvals
  listPendingApproval() {
    return this.http.get<ChangeRequest[]>(
      `${environment.apiBaseUrl}/change-requests/pending-approval`
    );
  }

  // submit
  submit(id: number) {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/change-requests/${id}/submit`, {}
    )
  }

  // get id for approval
  get(id: number): Observable<ChangeRequest> {
    return this.http.get<ChangeRequest>(`${environment.apiBaseUrl}/change-requests/${id}`
    );

  }

  // approve with comments
  approve(approvalId: number, comments?: string) {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/change-requests/approvals/${approvalId}/approve`, {},
      { params: comments ? { comments } : {} }
    );
  }

  // reject
  reject(approvalId: number, comments?: string) {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/change-requests/approvals/${approvalId}/reject`, {},
      { params: comments ? { comments } : {} }
    );
  }

}


