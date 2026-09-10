import { TestBed } from '@angular/core/testing';
import {
  HTTP_INTERCEPTORS,
  HttpClient,
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthInterceptor } from './auth-interceptor';

describe('AuthInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('adds Authorization: Bearer on a normal request', () => {
    localStorage.setItem('authToken', 'token-abc');

    http.get('http://localhost:8083/api/products').subscribe();

    const req = httpMock.expectOne('http://localhost:8083/api/products');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-abc');

    console.log('=== interceptor: normal request ===');
    console.log('Authorization:', req.request.headers.get('Authorization'));

    req.flush({});
  });

  it('does NOT add the header on /api/auth/login', () => {
    localStorage.setItem('authToken', 'token-abc');

    http.post('http://localhost:8081/api/auth/login', {}).subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);

    console.log('=== interceptor: login route ===');
    console.log('Has Authorization?', req.request.headers.has('Authorization'));

    req.flush({});
  });

  it('without a token, adds no header', () => {
    http.get('http://localhost:8083/api/products').subscribe();

    const req = httpMock.expectOne('http://localhost:8083/api/products');
    expect(req.request.headers.has('Authorization')).toBe(false);

    console.log('=== interceptor: no token ===');
    console.log('Has Authorization?', req.request.headers.has('Authorization'));

    req.flush({});
  });
});