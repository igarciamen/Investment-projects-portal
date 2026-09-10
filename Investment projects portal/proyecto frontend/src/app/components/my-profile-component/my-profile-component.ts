import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../service/auth-service';

@Component({
  selector: 'app-my-profile-component',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './my-profile-component.html',
  styleUrl: './my-profile-component.css'
})
export class MyProfileComponent implements OnInit {
  form: FormGroup;
  saving = false;
  saveError = '';
  saved = false;

  constructor(public auth: AuthService, private fb: FormBuilder) {
    this.form = this.fb.group({
      country: ['', Validators.maxLength(100)],
      occupation: ['', Validators.maxLength(150)],
      preferredContactLanguage: ['', Validators.maxLength(50)],
      organisationName: ['', Validators.maxLength(150)],
      organisationCountry: ['', Validators.maxLength(100)]
    });
  }

  ngOnInit(): void {
    // userInfo$ already holds the signed-in user's data (populated at login
    // / app startup by AuthService); this just pre-fills the form with
    // whatever was already saved, if anything.
    this.auth.userInfo$.subscribe(info => {
      if (!info) return;
      this.form.patchValue({
        country: info.country ?? '',
        occupation: info.occupation ?? '',
        preferredContactLanguage: info.preferredContactLanguage ?? '',
        organisationName: info.organisationName ?? '',
        organisationCountry: info.organisationCountry ?? ''
      });
    });
  }

  onSubmit(): void {
    this.saveError = '';
    this.saved = false;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.auth.updateProfile(this.form.value)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => (this.saved = true),
        error: err => (this.saveError = err.error?.message || 'Could not save your profile. Please try again.')
      });
  }
}