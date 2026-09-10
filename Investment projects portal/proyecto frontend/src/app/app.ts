import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './service/auth-service';
import { map, Observable } from 'rxjs';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
 
  username$: Observable<string | null>;
  


  constructor(
    public auth: AuthService,
    public router: Router,

  ) {
    this.username$ = this.auth.userInfo$.pipe(
      map((info) => info?.username ?? null),
    );

  }

  ngOnInit(): void {

  }

  onLogout(event: MouseEvent) {
    event.preventDefault();
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  


}