import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatMessage, ConversationSummary, MessageThread } from '../model/message';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private baseUrl = 'http://localhost:8087/api/messages';

  constructor(private http: HttpClient) {}

  getThread(projectId: number, investorId: number): Observable<MessageThread> {
    return this.http.get<MessageThread>(`${this.baseUrl}/projects/${projectId}/investors/${investorId}`);
  }

  sendMessage(projectId: number, investorId: number, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.baseUrl}/projects/${projectId}/investors/${investorId}`, { content });
  }

  markAsRead(projectId: number, investorId: number): Observable<{ updated: number }> {
    return this.http.put<{ updated: number }>(`${this.baseUrl}/projects/${projectId}/investors/${investorId}/read`, {});
  }

  // Number of unread messages in a specific thread, from the caller's point
  // of view. Used by the promoter's "Investor conversations" list to show a
  // badge per conversation, without opening each one.
  getUnreadCount(projectId: number, investorId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.baseUrl}/projects/${projectId}/investors/${investorId}/unread-count`);
  }

  // Promoter/admin only: every investor thread open on this project.
  listConversationsForProject(projectId: number): Observable<ConversationSummary[]> {
    return this.http.get<ConversationSummary[]>(`${this.baseUrl}/projects/${projectId}`);
  }
}