
export interface ProcessOwner {
  businessProcessOwnerId: number;
  name: string;
  positionId: number;
  //parent?: ProcessOwner | null;
}

export type ProcessOwnerCreate = Pick<ProcessOwner, 'name' | 'positionId'>;
