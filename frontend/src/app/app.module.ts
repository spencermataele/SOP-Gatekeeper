import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HttpClientModule } from '@angular/common/http';
import { SopsListComponent } from "./features/sops/sops-list/sops-list.component";
import { SopsFormComponent } from "./features/sops/sops-form.component";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { ProcessOwnersListComponent} from "./features/admin/process-owners/process-owner-list/process-owners-list.component";
import { ProcessOwnerFormComponent} from "./features/admin/process-owners/process-owner-form/process-owner-form.component";
import { ProcessOwnersComponent } from "./features/admin/process-owners/process-owners.component";
import { AdminHomeComponent } from "./features/admin/admin-home.component";
import { ReportsHomeComponent} from "./features/reports/reports-home.component";
import {RouterModule} from "@angular/router";
import {LoginComponent} from "./features/sops/users/authorization/login.component";
import {CommonModule} from "@angular/common";

@NgModule({
  declarations: [
    AppComponent,
    ProcessOwnersListComponent,
    ProcessOwnerFormComponent,
    ProcessOwnersComponent,
    AdminHomeComponent,
    LoginComponent
  ],
  imports: [
    BrowserModule,
    RouterModule,
    AppRoutingModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule,
    SopsListComponent,
    SopsFormComponent,
    ReportsHomeComponent,
    CommonModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
