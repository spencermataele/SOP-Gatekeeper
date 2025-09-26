import { Injectable } from "@angular/core";
import { environment } from "../../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { ProcessOwner, ProcessOwnerCreate } from "../../sops/models/process-owner.model";

@Injectable({ providedIn: 'root' })
export class ProcessOwnerService {
  private base = `${environment.apiBaseUrl}/process-owners`;

  constructor(private http: HttpClient) {}

  list(): Observable<ProcessOwner[]> {
    return this.http.get<ProcessOwner[]>(this.base);
  }

  get(id: number): Observable<ProcessOwner> {
    return this.http.get<ProcessOwner>(`${this.base}/${id}`);
  }

  create(body: ProcessOwnerCreate): Observable<ProcessOwner> {
    return this.http.post<ProcessOwner>(this.base, body);
  }

  update(id: number, body: ProcessOwner): Observable<ProcessOwner> {
    return this.http.put<ProcessOwner>(`${this.base}/${id}`, body);
  }

  delete(id: number | undefined): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}

