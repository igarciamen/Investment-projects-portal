export interface ChatMessage {
  id: number;
  senderId: number;
  senderRole: string;
  content: string;
  createdAt: string;
  readAt: string | null;
}

export interface MessageThread {
  projectId: number;
  investorId: number;
  messages: ChatMessage[];
}

export interface ConversationSummary {
  projectId: number;
  investorId: number;
  createdAt: string;
  lastMessageAt: string | null;
}