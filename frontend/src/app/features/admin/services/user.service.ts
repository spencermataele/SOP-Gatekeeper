import {Injectable} from "@angular/core";
import {environment} from "../../../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {UserDto} from "../../sops/models/user.model";

@Injectable({ providedIn: 'root' })
export class UserService {
  private base = `${environment.apiBaseUrl}/admin/users`;

  constructor(private http: HttpClient) {}

  list(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(this.base);
  }

  create(body: {
    username: String,
    email: string,
    password: string,
    fullName: string,
    role: string
  }): Observable<UserDto> {
    return this.http.post<UserDto>(this.base, body);
  }

  update(id: number, body: {
    username: String,
    email: string,
    password: string,
    fullName: string,
    role: string
  }): Observable<UserDto> {
    return this.http.put<UserDto>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

}
