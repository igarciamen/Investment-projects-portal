import { Injectable } from '@angular/core';
import {
  CanActivate,
  Router,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';
import { AuthService } from '../service/auth-service';


@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

   canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
  ): Observable<boolean> {

    if (!this.auth.isAuthenticated()) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: state.url },
      });
      return of(false);
    }

    const requiredRoles = (route.data['roles'] as string[]) || [];
    if (requiredRoles.length === 0) {
      return of(true);
    }

    const info$ =
      this.auth.getRoles().length > 0
        ? this.auth.userInfo$
        : this.auth.fetchUserInfo();

    return info$.pipe(
      take(1),
      switchMap((info) =>
        info ? of(info) : this.auth.fetchUserInfo().pipe(take(1)),
      ),
      map((info) => {
        const roles = info?.roles ?? [];
        const hasRole = requiredRoles.some((role) => roles.includes(role));
        if (!hasRole) {
          this.router.navigate(['/']);
        }
        return hasRole;
      }),
    );
  }
}
