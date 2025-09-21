import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'sops' },
  { path: 'sops', loadComponent: () => import('./features/sops/sops-list/sops-list.component').then(m => m.SopsListComponent)},
  { path: 'sops/new', loadComponent: () => import('./features/sops/sops-form/sops-form.component').then(m => m.SopsFormComponent)},
  { path: 'sops/:id/edit', loadComponent: () => import('./features/sops/sops-form/sops-form.component').then(m => m.SopsFormComponent)},
  { path: '**', redirectTo: 'sops'}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
