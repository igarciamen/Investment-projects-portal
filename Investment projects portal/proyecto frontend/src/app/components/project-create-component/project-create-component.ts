import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ProjectService } from '../../service/project-service';
import { SectorService } from '../../service/sector-service';
import { Sector } from '../../model/sector';

@Component({
  selector: 'app-project-create-component',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './project-create-component.html',
  styleUrl: './project-create-component.css'
})
export class ProjectCreateComponent implements OnInit {
  form: FormGroup;
  sectors: Sector[] = [];
  loading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private projectService: ProjectService,
    private sectorService: SectorService,
    private router: Router
  ) {
    this.form = this.fb.group({
      sectorId: ['', Validators.required],
      title: ['', [Validators.required, Validators.maxLength(120)]],
      description: ['', Validators.maxLength(2000)],
      country: ['', [Validators.required, Validators.maxLength(100)]],
      requestedAmount: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    this.sectorService.listActive().subscribe({
      next: sectors => (this.sectors = sectors)
    });
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const raw = this.form.value;

    this.projectService
      .create({
        sectorId: Number(raw.sectorId),
        title: raw.title,
        description: raw.description,
        country: raw.country,
        requestedAmount: Number(raw.requestedAmount)
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: project => this.router.navigate(['/projects', project.id]),
        error: err => {
          this.errorMessage = err.error?.message || 'Could not create the project. Please try again.';
        }
      });
  }

  get sectorId() { return this.form.get('sectorId')!; }
  get title() { return this.form.get('title')!; }
  get country() { return this.form.get('country')!; }
  get requestedAmount() { return this.form.get('requestedAmount')!; }
}