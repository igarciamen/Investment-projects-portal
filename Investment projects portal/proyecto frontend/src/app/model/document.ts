export type DocumentType = 'BUSINESS_PLAN' | 'ANNUAL_ACCOUNTS' | 'TECHNICAL_REPORT';

export interface ProjectDocument {
  id: number;
  projectId: number;
  uploaderUserId: number;
  uploaderRole: string;
  documentType: DocumentType;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}