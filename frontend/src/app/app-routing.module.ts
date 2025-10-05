import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SopsListComponent } from './features/sops/sops-list/sops-list.component';
import { SopsFormComponent } from './features/sops/sops-form.component';
import { ProcessOwnerCreateComponent } from "./features/sops/process-owners/process-owner-create";
import { ProcessOwnersComponent } from "./features/admin/process-owners/process-owners.component";
import { AdminHomeComponent } from "./features/admin/admin-home.component";
import { BusinessProcessesPageComponent} from "./features/admin/business-processes/business-processes-page.component";
import { BusinessProcessFormComponent} from "./features/admin/business-processes/business-process-form.component";
import { BusinessProcessFamiliesPageComponent} from "./features/admin/business-process-families/business-process-families-page.component";
import { BusinessProcessFamilyFormComponent} from "./features/admin/business-process-families/business-process-family-form.component";

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'sops' },
  { path: 'sops', component: SopsListComponent },
  { path: 'sops/new', component: SopsFormComponent },
  { path: 'sops/:id/edit', component: SopsFormComponent },
  { path: 'admin', component: AdminHomeComponent },
  { path: 'admin/orgs', loadComponent: () => import('./features/admin/orgs/orgs-page.component').then(m => m.OrgsPageComponent) },
  { path: 'admin/org-groups', loadComponent: () => import('./features/admin/org-groups/org-groups-page.component').then(m => m.OrgGroupsPageComponent) },
  { path: 'admin/departments', loadComponent: () => import('./features/admin/departments/departments-page.component').then(m => m.DepartmentsPageComponent) },
  { path: 'admin/dept-subgroups', loadComponent: () => import('./features/admin/dept-subgroups/dept-subgroups-page.component').then(m => m.DeptSubgroupsPageComponent) },
  { path: 'admin/process-owners', component: ProcessOwnersComponent },
  { path: 'admin/process-owners/new', component: ProcessOwnerCreateComponent },
  { path: 'admin/business-processes', component: BusinessProcessesPageComponent },
  { path: 'admin/business-processes/new', component: BusinessProcessFormComponent },
  { path: 'admin/business-processes/:id', component: BusinessProcessFormComponent },
  { path: 'admin/business-process-families', component: BusinessProcessFamiliesPageComponent },
  { path: 'admin/business-process-families/new', component: BusinessProcessFamilyFormComponent },
  { path: 'admin/business-process-families/:id', component: BusinessProcessFamilyFormComponent},
  { path: 'reports', loadComponent: () => import('./features/reports/reports-home.component').then(m => m.ReportsHomeComponent) },
  { path: 'reports/org-hierarchy', loadComponent: () => import('./features/reports/org-hierarchy-report.component').then(m => m.OrgHierarchyReportComponent) },
  { path: '**', redirectTo: 'sops' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
