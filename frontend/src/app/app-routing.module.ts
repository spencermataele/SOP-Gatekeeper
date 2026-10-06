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
import {AdminGuard} from './features/authorization/admin.guard';
import {AuthGuard} from "./features/authorization/auth.guard";
import {WorkflowComponent, WorkflowUnsavedGuard} from './features/workflows/workflow.component';
import {environment} from '../environments/environment';
import { SopViewComponent} from "./features/sops/sops-view/sop-view/sop-view.component";
import {
  ChangeRequestReviewComponent
} from "./features/sops/change-requests/change-request-review/change-request-review.component";

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent },
  { path: 'sops', component: environment.lifecycleEnabled ? WorkflowComponent : SopsListComponent, canActivate: [AuthGuard], canDeactivate: environment.lifecycleEnabled ? [WorkflowUnsavedGuard] : [] },
  ...(environment.lifecycleEnabled ? [
    {path: 'sops/new', redirectTo: 'sops'},
    {path: 'sops/:id/edit', redirectTo: 'sops'},
    {path: 'sops/:id/view', redirectTo: 'sops'}
  ] : [
    {path: 'sops/new', component: SopsFormComponent, canActivate: [AuthGuard]},
    {path: 'sops/:id/edit', component: SopsFormComponent, canActivate: [AuthGuard]},
    {path: 'sops/:id/view', component: SopViewComponent, canActivate: [AuthGuard]}
  ]),
  { path: 'admin/workflow-setup', loadComponent: () => import('./features/admin/workflow-setup.component').then(m => m.WorkflowSetupComponent), canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin', component: AdminHomeComponent, canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/users', loadComponent: () => import('./features/admin/users/user-page.component').then(m => m.UsersPageComponent), canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/orgs', loadComponent: () => import('./features/admin/orgs/orgs-page.component').then(m => m.OrgsPageComponent), canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/org-groups', loadComponent: () => import('./features/admin/org-groups/org-groups-page.component').then(m => m.OrgGroupsPageComponent), canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/departments', loadComponent: () => import('./features/admin/departments/departments-page.component').then(m => m.DepartmentsPageComponent), canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/dept-subgroups', loadComponent: () => import('./features/admin/dept-subgroups/dept-subgroups-page.component').then(m => m.DeptSubgroupsPageComponent), canActivate: [AuthGuard, AdminGuard] },

  { path: 'admin/process-owners', component: ProcessOwnersComponent, canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/process-owners/new', component: ProcessOwnerCreateComponent, canActivate: [AuthGuard, AdminGuard] },

  { path: 'admin/business-processes', component: BusinessProcessesPageComponent, canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/business-processes/new', component: BusinessProcessFormComponent, canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/business-processes/:id', component: BusinessProcessFormComponent, canActivate: [AuthGuard, AdminGuard] },

  { path: 'admin/business-process-families', component: BusinessProcessFamiliesPageComponent, canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/business-process-families/new', component: BusinessProcessFamilyFormComponent, canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/business-process-families/:id', component: BusinessProcessFamilyFormComponent, canActivate: [AuthGuard, AdminGuard] },

  ...(environment.lifecycleEnabled ? [
    {path: 'change-requests', redirectTo: 'sops'},
    {path: 'change-requests/:id/review', redirectTo: 'sops'}
  ] : [
    {path: 'change-requests', component: ChangeRequestPageComponent, canActivate: [AuthGuard]},
    {path: 'change-requests/:id/review', component: ChangeRequestReviewComponent, canActivate: [AuthGuard]}
  ]),

  { path: 'reports', loadComponent: () => import('./features/reports/reports-home.component').then(m => m.ReportsHomeComponent), canActivate: [AuthGuard] },
  { path: 'reports/org-hierarchy', loadComponent: () => import('./features/reports/org-hierarchy-report.component').then(m => m.OrgHierarchyReportComponent), canActivate: [AuthGuard] },
  { path: '**', redirectTo: 'login' }
];


@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
