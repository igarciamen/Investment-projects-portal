import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProjectDocument } from '../model/document';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private baseUrl = 'http://localhost:8084/api/documents';

  constructor(private http: HttpClient) {}

  // Access is delegated entirely to "projects" on the backend (owner
  // promoter or admin) -- an investor calling this will simply get a 403
  // from the API, there is no separate check needed on the frontend side.
  listForProject(projectId: number): Observable<ProjectDocument[]> {
    return this.http.get<ProjectDocument[]>(`${this.baseUrl}/projects/${projectId}`);
  }

  upload(projectId: number, documentType: string, file: File): Observable<ProjectDocument> {
    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('file', file);
    return this.http.post<ProjectDocument>(`${this.baseUrl}/projects/${projectId}`, formData);
  }

  // Triggers a real browser download using the Blob response and the
  // document's own original filename (no need to parse Content-Disposition).
  download(documentId: number, originalFilename: string): void {
    this.http.get(`${this.baseUrl}/${documentId}/download`, { responseType: 'blob' }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = originalFilename;
      link.click();
      window.URL.revokeObjectURL(url);
    });
  }

  deleteDocument(documentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${documentId}`);
  }
}