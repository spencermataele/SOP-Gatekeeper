
export interface ProcessOwner {
  businessProcessOwnerId: number;
  name: string;
  positionId: number;
  //parent?: ProcessOwner | null;
  createdTimestamp?: string;
  lastUpdatedTimestamp?: string;
}

export type ProcessOwnerCreate = Pick<ProcessOwner, 'name' | 'positionId'>;
