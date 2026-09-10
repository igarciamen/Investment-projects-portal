import { Injectable } from '@angular/core';

import { UserInfo } from '../model/user-info';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, catchError, Observable, of, switchMap, tap } from 'rxjs';
import { JwtResponse } from '../model/jwt-response';

export interface UpdateProfilePayload {
  country?: string;
  occupation?: string;
  preferredContactLanguage?: string;
  organisationName?: string;
  organisationCountry?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authUrl  = 'http://localhost:8081/api/auth';
  private userUrl  = 'http://localhost:8081/api/user';
  private tokenKey = 'authToken';

  private userInfoSubject = new BehaviorSubject<UserInfo | null>(null);
  public  userInfo$      = this.userInfoSubject.asObservable();

  constructor(private http: HttpClient) {
    const token = this.getToken();
    if (token) {

      Promise.resolve().then(() =>
        this.fetchUserInfo().subscribe({ error: () => this.logout() })
      );
    }
  }

  login(login: string, password: string): Observable<UserInfo | null> {
    return this.http.post<JwtResponse>(`${this.authUrl}/login`, { login, password })
      .pipe(
        tap(res => {
          localStorage.setItem(this.tokenKey, res.token);
        }),
        switchMap(() => this.fetchUserInfo())
      );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.userInfoSubject.next(null);
  }

  signup(username: string, email: string, password: string, userType: string): Observable<UserInfo | null> {
    return this.http.post<any>(`${this.authUrl}/signup`, {
      username, email, password, userType
    });
  }

  fetchUserInfo(): Observable<UserInfo | null> {
    if (!this.getToken()) {
      this.userInfoSubject.next(null);
      return of(null);
    }
    return this.http.get<UserInfo>(`${this.userUrl}/me`)
      .pipe(
        tap(info => {
          this.userInfoSubject.next(info);
        }),
        catchError(() => {
          this.userInfoSubject.next(null);
          return of(null);
        })
      );
  }

  // Updates the signed-in user's own profile/organisation fields, and
  // refreshes the local userInfo$ with the server's response -- so any
  // component reading userInfo$ (e.g. the navbar) sees the update
  // immediately, without a separate fetchUserInfo() call.
  updateProfile(payload: UpdateProfilePayload): Observable<UserInfo> {
    return this.http.put<UserInfo>(`${this.userUrl}/me/profile`, payload)
      .pipe(
        tap(info => this.userInfoSubject.next(info))
      );
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role);
  }

  getRoles(): string[] {
    return this.userInfoSubject.value?.roles ?? [];
  }

  getUserId(): number | null {
    return this.userInfoSubject.value?.id ?? null;
  }
}