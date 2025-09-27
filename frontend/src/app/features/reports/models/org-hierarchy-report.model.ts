export interface OrgHierarchyRow {
  orgId: number | null; // can be null if using LEFT JOINs and missing parents
  orgName: string | null;
  orgGroupId: number | null;
  orgGroupName: string | null;
  departmentId: number | null;
  departmentName: string | null;
  deptSubgroupId: number | null;
  deptSubgroupName: string | null;
  createdTimestamp: string;
  lastUpdatedTimestamp: string; 
}
