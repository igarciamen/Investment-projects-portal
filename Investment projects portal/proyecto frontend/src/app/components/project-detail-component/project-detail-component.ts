import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin, of, catchError, Observable } from 'rxjs';
import { ProjectService } from '../../service/project-service';
import { SectorService } from '../../service/sector-service';
import { InterestService } from '../../service/interest-service';
import { DocumentService } from '../../service/document-service';
import { MessageService } from '../../service/message-service';
import { AuthService } from '../../service/auth-service';
import { Project } from '../../model/project';
import { ProjectDocument, DocumentType } from '../../model/document';
import { ChatMessage, ConversationSummary } from '../../model/message';

@Component({
  selector: 'app-project-detail-component',
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './project-detail-component.html',
  styleUrl: './project-detail-component.css'
})
export class ProjectDetailComponent implements OnInit {
  project: Project | null = null;
  sectorName = '';
  loading = true;
  notFound = false;

  submitting = false;
  submitError = '';

  adminActionInProgress = false;
  adminActionError = '';
  rejectForm: FormGroup;
  showRejectForm = false;

  interestForm: FormGroup;
  alreadyInterested = false;
  sendingInterest = false;
  interestError = '';
  interestSent = false;

  documents: ProjectDocument[] = [];
  documentsLoading = false;
  documentsError = '';
  uploadForm: FormGroup;
  selectedFile: File | null = null;
  uploading = false;
  uploadError = '';

  conversations: ConversationSummary[] = [];
  conversationsLoading = false;
  // investorId -> unread count, populated right after loading the
  // conversation list, so each item can show its own badge without opening it.
  conversationUnreadCounts = new Map<number, number>();
  activeInvestorId: number | null = null;
  threadMessages: ChatMessage[] = [];
  threadLoading = false;
  chatForm: FormGroup;
  sendingChatMessage = false;
  chatError = '';

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private sectorService: SectorService,
    private interestService: InterestService,
    private documentService: DocumentService,
    private messageService: MessageService,
    public auth: AuthService,
    private fb: FormBuilder
  ) {
    this.interestForm = this.fb.group({
      message: ['', Validators.maxLength(1000)]
    });
    this.rejectForm = this.fb.group({
      reason: ['', Validators.maxLength(500)]
    });
    this.uploadForm = this.fb.group({
      documentType: ['', Validators.required]
    });
    this.chatForm = this.fb.group({
      content: ['', [Validators.required, Validators.maxLength(2000)]]
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.projectService.getById(id).subscribe({
      next: project => {
        this.project = project;
        this.loading = false;

        this.sectorService.listActive().subscribe(sectors => {
          this.sectorName = sectors.find(s => s.id === project.sectorId)?.name ?? 'Unknown sector';
        });

        this.checkExistingInterest(id);
        this.loadOwnerGatedSections(project);
      },
      error: () => {
        this.notFound = true;
        this.loading = false;
      }
    });
  }

  // Waits until AuthService actually knows the signed-in user's id/roles
  // before deciding whether to load Documents/Chat.
  private loadOwnerGatedSections(project: Project): void {
    const ready$ = (!this.auth.isAuthenticated() || this.auth.getRoles().length > 0)
      ? of(null)
      : this.auth.fetchUserInfo();

    ready$.subscribe(() => {
      if (this.isOwner || this.isAdmin) {
        this.loadDocuments();
      }

      if (this.auth.isAuthenticated() && this.auth.hasRole('ROLE_INVESTOR') && project.status === 'APPROVED') {
        this.openConversation(this.auth.getUserId()!);
      } else if (this.isOwner || this.isAdmin) {
        this.loadConversations();
      }
    });
  }

  private checkExistingInterest(projectId: number): void {
    if (!this.auth.isAuthenticated() || !this.auth.hasRole('ROLE_INVESTOR')) {
      return;
    }
    this.interestService.listMine().subscribe({
      next: interests => {
        this.alreadyInterested = interests.some(i => i.projectId === projectId);
      }
    });
  }

  get isOwner(): boolean {
    return this.auth.getUserId() === this.project?.promoterId;
  }

  get isAdmin(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  onSubmitProject(): void {
    if (!this.project) return;
    this.submitting = true;
    this.submitError = '';

    this.projectService.submit(this.project.id)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: updated => (this.project = updated),
        error: err => (this.submitError = err.error?.message || 'Could not submit the project.')
      });
  }

  onStartReview(): void {
    if (!this.project) return;
    this.adminActionInProgress = true;
    this.adminActionError = '';

    this.projectService.review(this.project.id)
      .pipe(finalize(() => (this.adminActionInProgress = false)))
      .subscribe({
        next: updated => (this.project = updated),
        error: err => (this.adminActionError = err.error?.message || 'Could not start the review.')
      });
  }

  onApprove(): void {
    if (!this.project) return;
    this.adminActionInProgress = true;
    this.adminActionError = '';

    this.projectService.approve(this.project.id)
      .pipe(finalize(() => (this.adminActionInProgress = false)))
      .subscribe({
        next: updated => (this.project = updated),
        error: err => (this.adminActionError = err.error?.message || 'Could not approve the project.')
      });
  }

  onReject(): void {
    if (!this.project) return;
    this.adminActionInProgress = true;
    this.adminActionError = '';

    this.projectService.reject(this.project.id, this.rejectForm.value.reason)
      .pipe(finalize(() => (this.adminActionInProgress = false)))
      .subscribe({
        next: updated => {
          this.project = updated;
          this.showRejectForm = false;
        },
        error: err => (this.adminActionError = err.error?.message || 'Could not reject the project.')
      });
  }

  onExpressInterest(): void {
    if (!this.project) return;
    this.sendingInterest = true;
    this.interestError = '';

    this.interestService.create(this.project.id, this.interestForm.value.message)
      .pipe(finalize(() => (this.sendingInterest = false)))
      .subscribe({
        next: () => {
          this.interestSent = true;
          this.alreadyInterested = true;
        },
        error: err => (this.interestError = err.error?.message || 'Could not send your interest. Please try again.')
      });
  }

  loadDocuments(): void {
    if (!this.project) return;
    this.documentsLoading = true;
    this.documentsError = '';

    this.documentService.listForProject(this.project.id)
      .pipe(finalize(() => (this.documentsLoading = false)))
      .subscribe({
        next: docs => (this.documents = docs),
        error: err => (this.documentsError = err.error?.message || 'Could not load the documents.')
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  onUploadDocument(): void {
    if (!this.project || !this.selectedFile || this.uploadForm.invalid) {
      this.uploadForm.markAllAsTouched();
      return;
    }

    this.uploading = true;
    this.uploadError = '';
    const documentType: DocumentType = this.uploadForm.value.documentType;

    this.documentService.upload(this.project.id, documentType, this.selectedFile)
      .pipe(finalize(() => (this.uploading = false)))
      .subscribe({
        next: () => {
          this.selectedFile = null;
          this.uploadForm.reset();
          this.loadDocuments();
        },
        error: err => (this.uploadError = err.error?.message || 'Could not upload the file.')
      });
  }

  onDownloadDocument(doc: ProjectDocument): void {
    this.documentService.download(doc.id, doc.originalFilename);
  }

  onDeleteDocument(doc: ProjectDocument): void {
    this.documentService.deleteDocument(doc.id).subscribe({
      next: () => this.loadDocuments(),
      error: err => (this.documentsError = err.error?.message || 'Could not delete the document.')
    });
  }

  loadConversations(): void {
    if (!this.project) return;
    this.conversationsLoading = true;
    const projectId = this.project.id;

    this.messageService.listConversationsForProject(projectId)
      .pipe(finalize(() => (this.conversationsLoading = false)))
      .subscribe({
        next: conversations => {
          this.conversations = conversations;
          this.loadUnreadCounts(projectId, conversations);
        }
      });
  }

  // Fetches the unread count for every conversation in parallel. Each call
  // is wrapped with catchError so that ONE failing conversation (e.g. a
  // transient 403/network error) does NOT wipe out the counts for every
  // other conversation -- forkJoin fails as a whole the moment any single
  // inner observable errors, which was silently emptying the entire map
  // before this fix, with no visible error anywhere.
  private loadUnreadCounts(projectId: number, conversations: ConversationSummary[]): void {
    if (conversations.length === 0) return;

    const calls = conversations.reduce((acc, conv) => {
      acc[conv.investorId] = this.messageService.getUnreadCount(projectId, conv.investorId).pipe(
        catchError(err => {
          console.error(`Could not load unread count for investor ${conv.investorId}:`, err);
          return of({ count: 0 });
        })
      );
      return acc;
    }, {} as Record<number, Observable<{ count: number }>>);

    forkJoin(calls).subscribe({
      next: results => {
        this.conversationUnreadCounts = new Map(
          Object.entries(results).map(([investorId, res]) => [Number(investorId), res.count])
        );
      },
      error: err => console.error('Could not load unread counts:', err)
    });
  }

  openConversation(investorId: number): void {
    if (!this.project) return;
    this.activeInvestorId = investorId;
    this.threadLoading = true;
    this.chatError = '';

    this.messageService.getThread(this.project.id, investorId)
      .pipe(finalize(() => (this.threadLoading = false)))
      .subscribe({
        next: thread => {
          this.threadMessages = thread.messages;
          this.messageService.markAsRead(this.project!.id, investorId).subscribe(() => {
            // Reflects the read state immediately in the list's badge,
            // without waiting for a full reload of every conversation.
            this.conversationUnreadCounts.set(investorId, 0);
          });
        },
        error: err => (this.chatError = err.error?.message || 'Could not load the conversation.')
      });
  }

  onSendChatMessage(): void {
    if (!this.project || !this.activeInvestorId || this.chatForm.invalid) return;
    this.sendingChatMessage = true;

    this.messageService.sendMessage(this.project.id, this.activeInvestorId, this.chatForm.value.content)
      .pipe(finalize(() => (this.sendingChatMessage = false)))
      .subscribe({
        next: message => {
          this.threadMessages = [...this.threadMessages, message];
          this.chatForm.reset();
        },
        error: err => (this.chatError = err.error?.message || 'Could not send the message.')
      });
  }
}