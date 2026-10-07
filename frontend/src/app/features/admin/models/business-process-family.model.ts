export interface BusinessProcessFamily {
  businessProcessFamilyId: number;
  businessProcessFamilyName: string;
  deptSubgroupId: number;
  departmentId: number;
  departmentName?: string;
  deptSubgroupName?: string;
  createdTimestamp?: string;
  lastUpdatedTimestamp?: string;
  businessProcesses: { businessProcessId: number; businessProcessName: string }[];
}

export interface BusinessProcessFamilyCreate {
  businessProcessFamilyName: string;
  deptSubgroupId: number;
}

export type BusinessProcessFamilyUpdate = Partial<BusinessProcessFamilyCreate>;
