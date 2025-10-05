export interface BusinessProcessFamily {
  businessProcessFamilyId: number;
  businessProcessFamilyName: string;
  departmentId: number;
  departmentName?: string;
  createdTimestamp?: string;
  lastUpdatedTimestamp?: string;
  businessProcesses: { businessProcessId: number; businessProcessName: string }[];
}

export interface BusinessProcessFamilyCreate {
  businessProcessFamilyName: string;
  departmentId: number;
}

export type BusinessProcessFamilyUpdate = Partial<BusinessProcessFamilyCreate>;
