import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SopsListComponent } from './features/sops/sops-list/sops-list.component';
import { SopsFormComponent } from './features/sops/sops-form.component';
import { ProcessOwnerCreateComponent } from "./features/sops/process-owners/process-owner-create";
import { ProcessOwnersComponent } from "./features/admin/process-owners/process-owners.component";
import { AdminHomeComponent } from "./features/admin/admin-home.component";

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'sops' },
  { path: 'sops', component: SopsListComponent },
  { path: 'sops/new', component: SopsFormComponent },
  { path: 'sops/:id/edit', component: SopsFormComponent },
  { path: 'admin', component: AdminHomeComponent },
  /*{ path: 'admin', loadComponent: () => import('./features/admin/admin-home.component').then(m => m.AdminHomeComponent) },*/
  { path: 'admin/orgs', loadComponent: () => import('./features/admin/orgs/orgs-page.component').then(m => m.OrgsPageComponent) },
  { path: 'admin/org-groups', loadComponent: () => import('./features/admin/org-groups/org-groups-page.component').then(m => m.OrgGroupsPageComponent) },
  { path: 'admin/departments', loadComponent: () => import('./features/admin/departments/departments-page.component').then(m => m.DepartmentsPageComponent) },
  { path: 'admin/dept-subgroups', loadComponent: () => import('./features/admin/dept-subgroups/dept-subgroups-page.component').then(m => m.DeptSubgroupsPageComponent) },
  { path: 'admin/process-owners', component: ProcessOwnersComponent },
  { path: 'process-owners/new', component: ProcessOwnerCreateComponent },
  { path: 'reports', loadComponent: () => import('./features/reports/reports-home.component').then(m => m.ReportsHomeComponent) },
  { path: 'reports/org-hierarchy', loadComponent: () => import('./features/reports/org-hierarchy-report.component').then(m => m.OrgHierarchyReportComponent) },
  { path: '**', redirectTo: 'sops' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
