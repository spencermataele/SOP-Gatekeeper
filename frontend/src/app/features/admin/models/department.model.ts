import { DeptSubgroup } from './dept-subgroup.model';

export interface Department {
  departmentId: number;
  departmentName: string;
  orgGroupId: number;
  subgroups?: DeptSubgroup[]; // if your DTO includes children
}
