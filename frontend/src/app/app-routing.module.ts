import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SopsListComponent } from './features/sops/sops-list/sops-list.component';
import { SopsFormComponent } from './features/sops/sops-form.component';
import { ProcessOwnerCreateComponent } from "./features/admin/process-owners/process-owner-form/process-owner-create";
import { ProcessOwnersComponent } from "./features/admin/process-owners/process-owners.component";
import { AdminHomeComponent } from "./features/admin/admin-home.component";
import { BusinessProcessesPageComponent} from "./features/admin/business-processes/business-processes-page.component";
import { BusinessProcessFormComponent} from "./features/admin/business-processes/business-process-form.component";
import { BusinessProcessFamiliesPageComponent} from "./features/admin/business-process-families/business-process-families-page.component";
import { BusinessProcessFamilyFormComponent} from "./features/admin/business-process-families/business-process-family-form.component";
import { ChangeRequestPageComponent } from "./features/sops/change-requests/change-request-page/change-request-page.component";
import {LoginComponent} from "./features/authorization/login.component";
import {AuthGuard} from "./features/authorization/auth.guard";
import {
  ChangeRequestReviewComponent
} from "./features/sops/change-requests/change-request-review/change-request-review.component";

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'sops' },
  { path: 'login', component: LoginComponent },
  { path: 'sops', component: SopsListComponent },
  { path: 'sops/new', component: SopsFormComponent, canActivate: [AuthGuard] },
  { path: 'sops/:id/edit', component: SopsFormComponent, canActivate: [AuthGuard] },
  { path: 'admin', component: AdminHomeComponent, canActivate: [AuthGuard] },
  { path: 'admin/users', loadComponent: () => import('./features/admin/users/user-page.component').then(m => m.UsersPageComponent), canActivate: [AuthGuard] },
  { path: 'admin/orgs', loadComponent: () => import('./features/admin/orgs/orgs-page.component').then(m => m.OrgsPageComponent), canActivate: [AuthGuard] },
  { path: 'admin/org-groups', loadComponent: () => import('./features/admin/org-groups/org-groups-page.component').then(m => m.OrgGroupsPageComponent), canActivate: [AuthGuard] },
  { path: 'admin/departments', loadComponent: () => import('./features/admin/departments/departments-page.component').then(m => m.DepartmentsPageComponent), canActivate: [AuthGuard] },
  { path: 'admin/dept-subgroups', loadComponent: () => import('./features/admin/dept-subgroups/dept-subgroups-page.component').then(m => m.DeptSubgroupsPageComponent), canActivate: [AuthGuard] },

  { path: 'admin/process-owners', component: ProcessOwnersComponent, canActivate: [AuthGuard] },
  { path: 'admin/process-owners/new', component: ProcessOwnerCreateComponent, canActivate: [AuthGuard] },

  { path: 'admin/business-processes', component: BusinessProcessesPageComponent, canActivate: [AuthGuard] },
  { path: 'admin/business-processes/new', component: BusinessProcessFormComponent, canActivate: [AuthGuard] },
  { path: 'admin/business-processes/:id', component: BusinessProcessFormComponent, canActivate: [AuthGuard] },

  { path: 'admin/business-process-families', component: BusinessProcessFamiliesPageComponent, canActivate: [AuthGuard] },
  { path: 'admin/business-process-families/new', component: BusinessProcessFamilyFormComponent, canActivate: [AuthGuard] },
  { path: 'admin/business-process-families/:id', component: BusinessProcessFamilyFormComponent, canActivate: [AuthGuard] },

  { path: 'change-requests', component: ChangeRequestPageComponent, canActivate: [AuthGuard] },
  { path: 'change-requests/:id/review', loadComponent: () => import('./features/sops/change-requests/change-request-review/change-request-review.component').then(m => m.ChangeRequestReviewComponent), canActivate: [AuthGuard] },

  { path: 'reports', loadComponent: () => import('./features/reports/reports-home.component').then(m => m.ReportsHomeComponent), canActivate: [AuthGuard] },
  { path: 'reports/org-hierarchy', loadComponent: () => import('./features/reports/org-hierarchy-report.component').then(m => m.OrgHierarchyReportComponent), canActivate: [AuthGuard] },
  { path: '**', redirectTo: 'sops' }
];


@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
