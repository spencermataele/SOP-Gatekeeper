import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {environment} from '../../../../../environments/environment';
import {OrgGroupService} from '../org-group.service';
import {DepartmentService} from '../department.service';

describe('Organizational hierarchy API contract', () => {
  let groups: OrgGroupService;
  let departments: DepartmentService;
  let http: HttpTestingController;
  const base = environment.apiBaseUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({imports: [HttpClientTestingModule]});
    groups = TestBed.inject(OrgGroupService);
    departments = TestBed.inject(DepartmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates an organization group with its parent organization', () => {
    const group = {orgGroupId: 2, orgGroupName: 'Operations', orgId: 1};
    groups.create(group).subscribe(result => expect(result.orgId).toBe(1));
    const request = http.expectOne(`${base}/admin/org-groups`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.orgId).toBe(1);
    request.flush(group);
  });

  it('preserves department parent identity and child subgroups', () => {
    departments.list().subscribe(result => {
      expect(result.length).toBe(1);
      expect(result[0].orgGroupId).toBe(2);
      expect(result[0].subgroups?.[0].deptSubgroupName).toBe('Unloading');
    });
    const request = http.expectOne(`${base}/admin/departments`);
    expect(request.request.method).toBe('GET');
    request.flush([{departmentId: 1, departmentName: 'Receiving', orgGroupId: 2,
      subgroups: [{deptSubgroupId: 101, deptSubgroupName: 'Unloading'}]}]);
  });

  it('accepts an empty child list without losing the parent relationship', () => {
    departments.list().subscribe(result => {
      expect(result[0].orgGroupId).toBe(2);
      expect(result[0].subgroups).toEqual([]);
    });
    http.expectOne(`${base}/admin/departments`).flush([
      {departmentId: 1, departmentName: 'Receiving', orgGroupId: 2, subgroups: []}
    ]);
  });

  it('propagates rejected parent validation to the caller', () => {
    departments.create({departmentName: 'Receiving', orgGroupId: 999}).subscribe({
      next: () => fail('A rejected create request must not report success'),
      error: (error: HttpErrorResponse) => {
        expect(error.status).toBe(400);
        expect(error.error.detail).toBe('Parent organization group is invalid');
      }
    });
    const request = http.expectOne(`${base}/admin/departments`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.orgGroupId).toBe(999);
    request.flush({detail: 'Parent organization group is invalid'}, {status: 400, statusText: 'Bad Request'});
  });
});
