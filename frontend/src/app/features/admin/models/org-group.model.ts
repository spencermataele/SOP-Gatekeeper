
export interface OrgGroupDto {
  orgGroupId: number;
  orgGroupName: string;
  orgId: number;

  // child associations
  departments?: {
    departmentId: number;
    departmentName: string;
  }[];
}
