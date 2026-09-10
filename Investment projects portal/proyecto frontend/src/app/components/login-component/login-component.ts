import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {  Router } from '@angular/router';
import { finalize } from 'rxjs';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../service/auth-service';

@Component({
  selector: 'app-login-component',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-component.html',
  styleUrl: './login-component.css'
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router

  ) {
    this.loginForm = this.fb.group({
      login: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

onSubmit() {
    this.errorMessage = '';
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const { login, password } = this.loginForm.value;

    this.auth.login(login, password)
      .pipe(
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: () => this.router.navigateByUrl('/'),
        error: err => {
          if (err.status === 401) {
            this.errorMessage = 'Wrong user or password';
          } else if (err.error?.message) {
            this.errorMessage = err.error.message;
          } else {
            this.errorMessage = 'Connection error, try later';
          }
        }
      });
  }

  get login()    { return this.loginForm.get('login')!; }
  get password() { return this.loginForm.get('password')!; }

}
