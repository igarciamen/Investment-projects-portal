import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProjectService } from '../../service/project-service';
import { SectorService } from '../../service/sector-service';
import { AuthService } from '../../service/auth-service';
import { Project } from '../../model/project';
import { Sector } from '../../model/sector';

interface SectorOverviewRow {
  sector: Sector;
  projectCount: number;
}

@Component({
  selector: 'app-home-component',
  imports: [CommonModule, RouterLink],
  templateUrl: './home-component.html',
  styleUrl: './home-component.css'
})
export class HomeComponent implements OnInit {
  latestProjects: Project[] = [];
  sectorsOverview: SectorOverviewRow[] = [];
  totalApprovedProjects = 0;
  loading = true;

  constructor(
    private projectService: ProjectService,
    private sectorService: SectorService,
    public auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    forkJoin({
      projects: this.projectService.listPublic(),
      sectors: this.sectorService.listActive()
    }).subscribe({
      next: ({ projects, sectors }) => {
        this.totalApprovedProjects = projects.length;

        this.latestProjects = [...projects]
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 3);

        this.sectorsOverview = sectors
          .map(sector => ({
            sector,
            projectCount: projects.filter(p => p.sectorId === sector.id).length
          }))
          .sort((a, b) => b.projectCount - a.projectCount);

        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  // Already signed in as a promoter -> straight to the real "create project"
  // form. Anyone else (no session, or signed in as something else) -> signup,
  // same as an anonymous visitor on the real InvestEU Portal.
  onAddProject(event: MouseEvent): void {
    if (this.auth.isAuthenticated() && this.auth.hasRole('ROLE_PROMOTER')) {
      event.preventDefault();
      this.router.navigate(['/projects/new']);
    }
    // Otherwise, let the routerLink on the <a> navigate to /signup as usual.
  }

  // Already signed in as an investor -> straight to the catalog, where
  // expressing interest happens per project. Anyone else -> signup.
  onRegisterInvestor(event: MouseEvent): void {
    if (this.auth.isAuthenticated() && this.auth.hasRole('ROLE_INVESTOR')) {
      event.preventDefault();
      this.router.navigate(['/projects']);
    }
  }
}