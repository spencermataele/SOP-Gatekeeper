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
  businessProcessId: number;
  businessProcessName: string;
  businessProcessFamilyId: number;
  parentProcessId: number;
  sopLocationPath: string;
  createdTimestamp?: string;
  updatedTimestamp?: string;
  versionId: string;
  sopDetails: string;
}
