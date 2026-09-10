import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, catchError, of, Observable } from 'rxjs';
import { InterestService } from '../../service/interest-service';
import { ProjectService } from '../../service/project-service';
import { MessageService } from '../../service/message-service';
import { AuthService } from '../../service/auth-service';
import { Interest } from '../../model/interest';
import { Project } from '../../model/project';

interface InvestmentRow {
  interest: Interest;
  project: Project | null;
  unreadCount: number;
}

@Component({
  selector: 'app-my-investments-component',
  imports: [CommonModule, RouterLink],
  templateUrl: './my-investments-component.html',
  styleUrl: './my-investments-component.css'
})
export class MyInvestmentsComponent implements OnInit {
  rows: InvestmentRow[] = [];
  loading = true;

  constructor(
    private interestService: InterestService,
    private projectService: ProjectService,
    private messageService: MessageService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.interestService.listMine().subscribe({
      next: interests => this.loadRowDetails(interests),
      error: () => (this.loading = false)
    });
  }

  // Combines three already-existing endpoints (interests/investor/me,
  // projects/public/{id}, messages/.../unread-count) client-side, the same
  // pattern already used for the Home page's "Sectors overview" -- no new
  // backend endpoint needed. Each project's details and unread count are
  // fetched with their own catchError, so a single failing project (e.g.
  // one that was later un-approved) does not wipe out the whole list --
  // the exact lesson learned from the earlier forkJoin incident on the
  // promoter's unread badge.
  private loadRowDetails(interests: Interest[]): void {
    if (interests.length === 0) {
      this.loading = false;
      return;
    }

    const investorId = this.auth.getUserId()!;

    const projectCalls: Record<number, Observable<Project | null>> = {};
    const unreadCalls: Record<number, Observable<number>> = {};

    for (const interest of interests) {
      projectCalls[interest.projectId] = this.projectService.getPublicById(interest.projectId).pipe(
        catchError(err => {
          console.error(`Could not load project ${interest.projectId}:`, err);
          return of(null);
        })
      );
      unreadCalls[interest.projectId] = this.messageService.getUnreadCount(interest.projectId, investorId).pipe(
        catchError(err => {
          console.error(`Could not load unread count for project ${interest.projectId}:`, err);
          return of({ count: 0 });
        }),
        // getUnreadCount resolves { count: number }; project this down to
        // just the number for a simpler InvestmentRow shape.
        (source => new Observable<number>(subscriber => source.subscribe({
          next: res => subscriber.next((res as { count: number }).count),
          error: e => subscriber.error(e),
          complete: () => subscriber.complete()
        })))
      );
    }

    forkJoin({ projects: forkJoin(projectCalls), unreads: forkJoin(unreadCalls) }).subscribe({
      next: ({ projects, unreads }) => {
        this.rows = interests.map(interest => ({
          interest,
          project: projects[interest.projectId],
          unreadCount: unreads[interest.projectId] ?? 0
        }));
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }
}