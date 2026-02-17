export interface BusinessProcess {
  businessProcessId: number;
  businessProcessName: string;

  businessProcessFamily: number;
  businessProcessFamilyName?: string;

  parentBusinessProcess?: number | null;
  parentProcessName?: string | null;

  departmentId: number;
  departmentName?: string;

  deptSubgroupIds?: number[];
  deptSubgroupNames?: string[];

  createdTimestamp?: string;
  lastUpdatedTimestamp?: string;
}

export interface BusinessProcessCreate {
  businessProcessName: string;
  businessProcessFamilyId: number;
  parentProcessId?: number | null;
  departmentId: number;
  deptSubgroupIds?: number[];
}

export type BusinessProcessUpdate = Partial<BusinessProcessCreate>;
