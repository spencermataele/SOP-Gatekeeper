
export interface DeptSubgroupSlimDto {
  deptSubgroupId: number;
  deptSubgroupName: string;
}

export interface DepartmentDto {
  departmentId: number;
  departmentName: string;
  orgGroupId: number;
  subgroups: DeptSubgroupSlimDto[] | null; // always an array
}

