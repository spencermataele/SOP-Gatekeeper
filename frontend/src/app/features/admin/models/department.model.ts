
export interface DeptSubgroupSlimDto {
  deptSubgroupId: number;
  deptSubgroupName: string;
}

export interface DepartmentDto {
  departmentId: number;
  departmentName: string;
  orgGroupId: number;
  subgroups: DeptSubgroupSlimDto[]; // always an array
}

