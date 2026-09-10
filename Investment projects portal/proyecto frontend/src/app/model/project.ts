export interface Project {
  id: number;
  promoterId: number;
  sectorId: number;
  title: string;
  description: string;
  country: string;
  requestedAmount: number;
  status: string;
  rejectionReason: string | null;
  submittedAt: string | null;
  evaluationDeadline: string | null;
  createdAt: string;
}
