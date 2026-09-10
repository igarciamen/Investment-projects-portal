import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../service/project-service';
import { Project } from '../../model/project';

@Component({
  selector: 'app-my-projects-component',
  imports: [CommonModule, RouterLink],
  templateUrl: './my-projects-component.html',
  styleUrl: './my-projects-component.css'
})
export class MyProjectsComponent implements OnInit {
  projects: Project[] = [];
  loading = true;

  constructor(private projectService: ProjectService) {}

  ngOnInit(): void {
    this.projectService.listMine().subscribe({
      next: projects => {
        this.projects = projects;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }
}
