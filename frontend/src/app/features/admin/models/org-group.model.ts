import { Department } from './department.model';

export interface OrgGroup {
  orgGroupId: number;
  orgGroupName: string;
  orgId: number;              // backend can also send parent id
  departments?: Department[]; // if your DTO includes children
}
