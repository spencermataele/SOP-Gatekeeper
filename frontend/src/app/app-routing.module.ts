import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SopsListComponent } from './features/sops/sops-list/sops-list.component';
import { SopsFormComponent } from './features/sops/sops-form/sops-form.component';
import { ProcessOwnerCreateComponent } from "./features/sops/process-owners/process-owner-create";
import {ProcessOwnersComponent} from "./features/admin/process-owners/process-owners.component";

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'sops' },
  { path: 'sops', component: SopsListComponent },
  { path: 'sops/new', component: SopsFormComponent },
  { path: 'sops/:id/edit', component: SopsFormComponent },
  { path: 'admin/process-owners', component: ProcessOwnersComponent },
  { path: 'process-owners/new', component: ProcessOwnerCreateComponent},
  { path: '**', redirectTo: 'sops' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
