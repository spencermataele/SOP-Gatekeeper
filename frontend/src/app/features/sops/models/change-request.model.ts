export interface ChangeRequest {
  changeRequestId: number;
  originalSopId: number;
  proposedSopId: number;
  requestedByUser: number;
  changeSummary: string;
  changeReason: string;
  changeStatus: 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';
  createdTimestamp: string;
  updatedTimestamp: string;
  requestByName: string;
  originalSopTitle: string;
  originalSopVersion: string;
  publishedSopId?: number;
}
