import {Observable} from "rxjs";
import {Sop} from "../models/sop.model";
import {environment} from "../../../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";

@Injectable({
  providedIn: 'root'
})
export class ChangeRequestService {

  constructor(private http: HttpClient) { }

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
}


