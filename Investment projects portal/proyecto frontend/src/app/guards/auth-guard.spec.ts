import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';

import { AuthGuard } from './auth-guard';

import { UserInfo } from '../model/user-info';
import { AuthService } from '../service/auth-service';

describe('AuthGuard', () => {
  let router: jasmine.SpyObj<Router>;

  const routeWith = (roles?: string[]) =>
    ({ data: roles ? { roles } : {} } as any as ActivatedRouteSnapshot);
  const state = { url: '/protected' } as RouterStateSnapshot;

  beforeEach(() => {
    router = jasmine.createSpyObj('Router', ['navigate']);
  });

  it('no session: redirects to /login and denies', (done) => {
    const auth = { isAuthenticated: () => false } as unknown as AuthService;
    const guard = new AuthGuard(auth, router);

    guard.canActivate(routeWith(['ROLE_ADMIN']), state).subscribe((ok) => {
      expect(ok).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(
        ['/login'], { queryParams: { returnUrl: '/protected' } });

      console.log('=== guard: no session ===');
      console.log('Allowed?', ok, '-> redirects to /login');
      done();
    });
  });

  it('route without required roles: allows', (done) => {
    const auth = { isAuthenticated: () => true } as unknown as AuthService;
    const guard = new AuthGuard(auth, router);

    guard.canActivate(routeWith(), state).subscribe((ok) => {
      expect(ok).toBe(true);
      console.log('=== guard: no required roles ===');
      console.log('Allowed?', ok);
      done();
    });
  });

  it('with the required role: allows', (done) => {
    const user: UserInfo = { id: 1, username: 'marco', email: 'm@m.com', roles: ['ROLE_PROMOTER'] };
    const auth = {
      isAuthenticated: () => true,
      getRoles: () => ['ROLE_PROMOTER'],
      userInfo$: of(user),
      fetchUserInfo: () => of(user),
    } as unknown as AuthService;
    const guard = new AuthGuard(auth, router);

    guard.canActivate(routeWith(['ROLE_PROMOTER']), state).subscribe((ok) => {
      expect(ok).toBe(true);
      expect(router.navigate).not.toHaveBeenCalled();
      console.log('=== guard: correct role ===');
      console.log('Allowed?', ok);
      done();
    });
  });

  it('without the required role: denies and goes to Home (/)', (done) => {
    const user: UserInfo = { id: 1, username: 'marco', email: 'm@m.com', roles: ['ROLE_PROMOTER'] };
    const auth = {
      isAuthenticated: () => true,
      getRoles: () => ['ROLE_PROMOTER'],
      userInfo$: of(user),
      fetchUserInfo: () => of(user),
    } as unknown as AuthService;
    const guard = new AuthGuard(auth, router);

    guard.canActivate(routeWith(['ROLE_ADMIN']), state).subscribe((ok) => {
      expect(ok).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/']);
      console.log('=== guard: insufficient role ===');
      console.log('Allowed?', ok, '-> redirects to Home (/)');
      done();
    });
  });
});
