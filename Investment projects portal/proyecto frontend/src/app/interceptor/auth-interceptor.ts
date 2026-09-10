import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent
} from '@angular/common/http';
import { Observable } from 'rxjs';
 
@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  private tokenKey = 'authToken'; 
  
 
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const url = req.url.toLowerCase();
 
   
    if (url.endsWith('/api/auth/login') || url.endsWith('/api/auth/signup')) {
      return next.handle(req);
    }
 

    const token = localStorage.getItem(this.tokenKey);
   
    if (token) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
    return next.handle(req);
  }
}