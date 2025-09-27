
export interface OrgDto {
  orgId: number;
  orgName: string;
  orgGroups: { orgGroupId: number; orgGroupName: string }[];
}
