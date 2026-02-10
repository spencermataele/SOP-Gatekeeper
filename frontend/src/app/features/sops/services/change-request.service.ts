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
  createDraft(originalSopId: number, summary: string, reason: string) {
    return this.http.post<ChangeRequest>(
      `${environment.apiBaseUrl}/change-requests/start`, null, {
        params: {
          originalSopId,
          summary,
          reason
        }
      }
    );
  }

  // Publish upon approval
  publishChangeRequest(
    changeRequestId: number
  ): Observable<Sop> {

    return this.http.post<Sop>(
      `${environment.apiBaseUrl}/change-requests/${changeRequestId}/publish`,
      {}
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

  cancel(id: number) {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/change-requests/${id}/cancel`, {}
    );
  }

}


