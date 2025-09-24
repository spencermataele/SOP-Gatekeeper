import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HttpClientModule } from '@angular/common/http';
import {SopsListComponent} from "./features/sops/sops-list/sops-list.component";
import {SopsFormComponent} from "./features/sops/sops-form/sops-form.component";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule,
    SopsListComponent,
    SopsFormComponent
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
