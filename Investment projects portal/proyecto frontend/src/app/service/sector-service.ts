import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Sector } from '../model/sector';

@Injectable({
  providedIn: 'root'
})
export class SectorService {
  private baseUrl = 'http://localhost:8082/api/sectors';

  constructor(private http: HttpClient) {}

  // Public catalog: active sectors only, no token required.
  listActive(): Observable<Sector[]> {
    return this.http.get<Sector[]>(this.baseUrl);
  }
}
