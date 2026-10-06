import {Injectable} from '@angular/core';
import {CanActivate, Router, UrlTree} from '@angular/router';
import {firstValueFrom} from 'rxjs';
import {AuthService} from './auth.service';
@Injectable({providedIn:'root'})
export class AdminGuard implements CanActivate {
  constructor(private auth: AuthService,private router: Router){}
  async canActivate(): Promise<boolean|UrlTree> {
    try { await firstValueFrom(this.auth.refreshMe()); return this.auth.isAdmin() || this.router.parseUrl('/sops'); }
    catch { return this.router.parseUrl('/login'); }
  }
}
