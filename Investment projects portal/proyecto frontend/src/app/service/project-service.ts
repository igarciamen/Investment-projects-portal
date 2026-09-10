import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { Project } from '../model/project';
import { ProjectMetrics } from '../model/project-metrics';
import { AuthService } from './auth-service';

export interface ProjectFilters {
  sector?: number;
  country?: string;
  minAmount?: number;
  maxAmount?: number;
}

export interface CreateProjectPayload {
  sectorId: number;
  title: string;
  description: string;
  country: string;
  requestedAmount: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private baseUrl = 'http://localhost:8083/api/projects';

  constructor(private http: HttpClient, private auth: AuthService) {}

  listPublic(filters: ProjectFilters = {}): Observable<Project[]> {
    let params = new HttpParams();
    if (filters.sector != null) params = params.set('sector', filters.sector);
    if (filters.country) params = params.set('country', filters.country);
    if (filters.minAmount != null) params = params.set('minAmount', filters.minAmount);
    if (filters.maxAmount != null) params = params.set('maxAmount', filters.maxAmount);

    return this.http.get<Project[]>(`${this.baseUrl}/public`, { params });
  }

  getPublicById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/public/${id}`);
  }

  getById(id: number): Observable<Project> {
    if (!this.auth.isAuthenticated()) {
      return this.getPublicById(id);
    }
    return this.http.get<Project>(`${this.baseUrl}/${id}`).pipe(
      catchError(() => this.getPublicById(id))
    );
  }

  create(payload: CreateProjectPayload): Observable<Project> {
    return this.http.post<Project>(this.baseUrl, payload);
  }

  submit(id: number): Observable<Project> {
    return this.http.patch<Project>(`${this.baseUrl}/${id}/submit`, {});
  }

  review(id: number): Observable<Project> {
    return this.http.patch<Project>(`${this.baseUrl}/${id}/review`, {});
  }

  approve(id: number): Observable<Project> {
    return this.http.patch<Project>(`${this.baseUrl}/${id}/approve`, {});
  }

  reject(id: number, reason: string): Observable<Project> {
    return this.http.patch<Project>(`${this.baseUrl}/${id}/reject`, reason ? { reason } : {});
  }

  // "My projects" (promoter dashboard): every project owned by the
  // signed-in promoter, any status.
  listMine(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.baseUrl}/mine`);
  }

  // Admin panel: projects waiting for an admin action, oldest first.
  listPendingEvaluation(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.baseUrl}/pending-evaluation`);
  }

  // Admin panel: project counts per status.
  getMetrics(): Observable<ProjectMetrics> {
    return this.http.get<ProjectMetrics>(`${this.baseUrl}/metrics`);
  }
}
