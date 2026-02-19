import {HttpClientTestingModule, HttpTestingController} from "@angular/common/http/testing";
import {TestBed} from "@angular/core/testing";
import {OrgGroupDto} from "../../models/org-group.model";
import {OrgGroupService} from "../org-group.service";
import {DepartmentDto} from "../../models/department.model";
import {DepartmentService} from "../department.service";


describe('Org Hierarchy Setup', () => {

  let orgGroupService: OrgGroupService;
  let departmentService: DepartmentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OrgGroupService]
    });

    orgGroupService = TestBed.inject(OrgGroupService);
    departmentService = TestBed.inject(DepartmentService);
    httpMock = TestBed.inject(HttpTestingController);

  });

  afterEach(() => {
    httpMock.verify();
  });

  /*** PASS ***/
  it('PASS: Org Hierarchy is save with parent relationship', () => {

    const childOrgGroup: OrgGroupDto = {
      orgGroupId: 2,
      orgGroupName: 'Operations',
      orgId: 1
    } ;

    orgGroupService.create(childOrgGroup).subscribe(response => {

      expect(response.orgGroupId).toBe(2);
      expect(response.orgId).toBe(1);

    })

    const req = httpMock.expectOne('http://localhost:8080/admin/org-groups');

    expect(req.request.method).toBe('POST');

    expect(req.request.body.orgGroupId).toBe(2);

    req.flush(childOrgGroup)

  });

  /*** PASS ***/
  it('PASS: Org hierarchy is retrieved with subgroup assignment', () => {

    const department: DepartmentDto[] = [
      { departmentId: 1, departmentName: 'Receiving', orgGroupId: 2, subgroups: [
          { deptSubgroupId: 101, deptSubgroupName: 'Unloading' },
          { deptSubgroupId: 102, deptSubgroupName: 'Putaway' }
        ]},
    { departmentId: 2, departmentName: 'Transfers', orgGroupId: 2, subgroups: [
        { deptSubgroupId: 201, deptSubgroupName: 'Loading' },
        { deptSubgroupId: 202, deptSubgroupName: 'Pulling' }
    ]}
    ];

    departmentService.list().subscribe(response => {

      expect(response.length).toBe(2);

      const receiving = response.find(d => d.departmentId === 1);

      expect(receiving?.orgGroupId).toBe(2);
    });

    const req = httpMock.expectOne('http://localhost:8080/admin/departments');

    expect(req.request.method).toBe('GET');

    req.flush(department)

  })

  /*** FAIL ***/
  it('FAIL: Parent relationship missing and should be detected', () => {

    const invalidDepartment: DepartmentDto[] = [
      { departmentId: 1, departmentName: 'Receiving', orgGroupId: 2, subgroups: null}
    ];

    departmentService.list().subscribe(response => {

      expect(response.length).toBe(1);

      const dept = response[0];

      expect(dept.subgroups).not.toBeNull();

    });

    const req = httpMock.expectOne('http://localhost:8080/admin/departments');

    req.flush(invalidDepartment)

  });

})
