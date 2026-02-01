export interface ChangeRequest {
  changeRequestId: number;
  sopId: number;
  requestedByUser: number;
  changeSummary: string;
  changeReason: string;
  changeStatus: 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';
  createdTimestamp: string;
  updatedTimestamp: string;
  requestByName: string;
  sopTitle: string;
  sopVersion: string;
  publishedSopId?: number;
}
