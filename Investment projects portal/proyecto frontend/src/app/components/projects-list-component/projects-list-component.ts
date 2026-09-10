import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ProjectService } from '../../service/project-service';
import { SectorService } from '../../service/sector-service';
import { Project } from '../../model/project';
import { Sector } from '../../model/sector';

@Component({
  selector: 'app-projects-list-component',
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './projects-list-component.html',
  styleUrl: './projects-list-component.css'
})
export class ProjectsListComponent implements OnInit {
  projects: Project[] = [];
  sectors: Sector[] = [];
  loading = true;
  filterForm: FormGroup;

  constructor(
    private projectService: ProjectService,
    private sectorService: SectorService,
    private fb: FormBuilder
  ) {
    this.filterForm = this.fb.group({
      sector: [''],
      country: [''],
      minAmount: [''],
      maxAmount: ['']
    });
  }

  ngOnInit(): void {
    this.sectorService.listActive().subscribe({
      next: sectors => (this.sectors = sectors)
    });
    this.search();
  }

  search(): void {
    this.loading = true;
    const raw = this.filterForm.value;

    this.projectService
      .listPublic({
        sector: raw.sector ? Number(raw.sector) : undefined,
        country: raw.country || undefined,
        minAmount: raw.minAmount ? Number(raw.minAmount) : undefined,
        maxAmount: raw.maxAmount ? Number(raw.maxAmount) : undefined
      })
      .subscribe({
        next: projects => {
          this.projects = projects;
          this.loading = false;
        },
        error: () => (this.loading = false)
      });
  }

  clearFilters(): void {
    this.filterForm.reset({ sector: '', country: '', minAmount: '', maxAmount: '' });
    this.search();
  }

  sectorName(sectorId: number): string {
    return this.sectors.find(s => s.id === sectorId)?.name ?? 'Unknown sector';
  }
}
