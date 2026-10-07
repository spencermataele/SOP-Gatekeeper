import {TestBed} from '@angular/core/testing';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {DepartmentsPageComponent} from './admin/departments/departments-page.component';
import {DeptSubgroupsPageComponent} from './admin/dept-subgroups/dept-subgroups-page.component';
import {OrgHierarchyReportComponent} from './reports/org-hierarchy-report.component';

for (const component of [DepartmentsPageComponent, DeptSubgroupsPageComponent, OrgHierarchyReportComponent]) {
  describe(component.name + ' navigation destination', () => {
    it('opens and loads its initial data', async () => {
      await TestBed.configureTestingModule({imports:[component,HttpClientTestingModule,RouterTestingModule]}).compileComponents();
      const fixture=TestBed.createComponent(component as any);
      fixture.detectChanges();
      const http=TestBed.inject(HttpTestingController);
      http.match(() => true).forEach(r => r.flush([]));
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('h2').textContent).toBeTruthy();
      http.verify();
    });
  });
}
