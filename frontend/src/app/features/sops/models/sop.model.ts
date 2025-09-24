export interface Sop {
  sopId?: number;
  title: string;
  authorId: number;
  orgId: number;
  orgGroupId: number;
  departmentId: number;
  deptSubgroupId: number;
  currentProcessOwnerId: number;
  currentProcessOwnerPositionId: number;
  processId: number;
  processName: string;
  processFamilyId: number;
  parentProcessId: number;
  sopLocationPath: string;
  createdTimestamp?: string;
  updatedTimestamp?: string;
  versionId: number;
  sopDetails: string;
}
