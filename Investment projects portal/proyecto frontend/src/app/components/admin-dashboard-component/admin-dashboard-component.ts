import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProjectService } from '../../service/project-service';
import { Project } from '../../model/project';
import { ProjectMetrics } from '../../model/project-metrics';

@Component({
  selector: 'app-admin-dashboard-component',
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard-component.html',
  styleUrl: './admin-dashboard-component.css'
})
export class AdminDashboardComponent implements OnInit {
  metrics: ProjectMetrics | null = null;
  pending: Project[] = [];
  loading = true;

  constructor(private projectService: ProjectService) {}

  ngOnInit(): void {
    forkJoin({
      metrics: this.projectService.getMetrics(),
      pending: this.projectService.listPendingEvaluation()
    }).subscribe({
      next: ({ metrics, pending }) => {
        this.metrics = metrics;
        this.pending = pending;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  // A submission's evaluation deadline coming up within 3 days is flagged in
  // the UI -- purely visual, matches the "purely informational" nature of
  // evaluationDeadline on the backend (nothing there enforces it either).
  isDueSoon(deadline: string | null): boolean {
    if (!deadline) return false;
    const daysLeft = (new Date(deadline).getTime() - Date.now()) / (1000 * 60 * 60 * 24);
    return daysLeft <= 3;
  }
}
