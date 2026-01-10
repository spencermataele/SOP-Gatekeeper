import {Injectable} from "@angular/core";
import {environment} from "../../../environments/environment";
import {BehaviorSubject, Observable, tap} from "rxjs";
import {HttpClient} from "@angular/common/http";

export interface AuthResponse { token: string; id: number; username: string; fullName: string; roles: string[] }
export interface MeResponse { id: number; username: string; fullName: string; roles: string[] }

@Injectable({ providedIn: 'root' })
export class AuthService {

  private base = `${environment.apiBaseUrl}/auth`;
  private me$ = new BehaviorSubject<MeResponse | null>(null);

  constructor(private http: HttpClient) {
    const token = localStorage.getItem('token');
    if (token) this.refreshMe().subscribe();
  }

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, { username, password }).pipe(
      tap(res => {
        localStorage.setItem('token', res.token);
        this.me$.next({id: res.id, username: res.username, fullName: res.fullName, roles: res.roles});
      })
    );
  }

  logout() { localStorage.removeItem('token'); this.me$.next(null); }
  token(): string | null {return localStorage.getItem('token');}

  me(): Observable<MeResponse | null> { return this.me$.asObservable(); }

  refreshMe(): Observable<MeResponse> {
    return this.http.get<MeResponse>(`${this.base}/me`).pipe(tap(m => this.me$.next(m)));
  }

  currentUser(): MeResponse | null { return this.me$.value; }

}
