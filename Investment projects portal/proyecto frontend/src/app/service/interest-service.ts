import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Interest } from '../model/interest';

@Injectable({
  providedIn: 'root'
})
export class InterestService {
  private baseUrl = 'http://localhost:8086/api/interests';

  constructor(private http: HttpClient) {}

  // Requires an investor token; the auth interceptor attaches it automatically.
  create(projectId: number, message: string): Observable<Interest> {
    return this.http.post<Interest>(this.baseUrl, { projectId, message });
  }

  // "My expressions of interest" -- used here just to check whether the
  // signed-in investor has already expressed interest in this project, so
  // the button can be disabled instead of letting a 409 surprise them.
  listMine(): Observable<Interest[]> {
    return this.http.get<Interest[]>(`${this.baseUrl}/investor/me`);
  }
}