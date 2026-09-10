import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs';
import { AuthService } from '../../service/auth-service';
@Component({
  selector: 'app-signup-component',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './signup-component.html',
  styleUrl: './signup-component.css'
})
export class SignupComponent implements OnInit {
  signupForm: FormGroup;
  errorMessage = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.signupForm = this.fb.group({
      username: ['', [Validators.required]],
      email:    ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
      userType: ['', [Validators.required]]
    }, {
      validators: this.matchPasswords('password', 'confirmPassword')
    });
  }

  ngOnInit(): void {
    // Pre-selects the user type when arriving from the Home page's
    // "Add your project" (?type=PROMOTER) or "Register as an investor"
    // (?type=INVESTOR) buttons. Any other/missing value is ignored, leaving
    // the field for the person to choose themselves.
    const type = this.route.snapshot.queryParamMap.get('type');
    if (type === 'PROMOTER' || type === 'INVESTOR') {
      this.signupForm.patchValue({ userType: type });
    }
  }

  private matchPasswords(passwordKey: string, confirmKey: string): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const pw = group.get(passwordKey)?.value;
      const cpw = group.get(confirmKey)?.value;
      return pw === cpw ? null : { passwordsMismatch: true };
    };
  }

  onSubmit() {
     this.errorMessage = '';
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    const { username, email, password, userType } = this.signupForm.value;

    this.auth.signup(username, email, password, userType)
        .pipe(
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: () => {

       this.router.navigate(['/login']);
        },
        error: err => {
          this.errorMessage = err.error?.message || 'Error in the registration';
        }
      });
  }

  get username() { return this.signupForm.get('username')!; }
  get email()    { return this.signupForm.get('email')!; }
  get password() { return this.signupForm.get('password')!; }
  get confirmPassword() { return this.signupForm.get('confirmPassword')!; }
  get userType() { return this.signupForm.get('userType')!; }
}
