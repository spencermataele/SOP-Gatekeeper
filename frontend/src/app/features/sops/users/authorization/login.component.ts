import {Component} from "@angular/core";
import {FormBuilder, Validators, ReactiveFormsModule} from "@angular/forms";
import {AuthService} from "./auth.service";
import {Router} from "@angular/router";


@Component({ selector: 'app-login', templateUrl: "./login.component.html" })
export class LoginComponent {

  form = this.fb.group({ username: ['', Validators.required], password: ['', Validators.required] });
  err?: string;
  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) { }
  submit() {
    if (this.form.invalid) return;
    const { username, password } = this.form.value as any;
    this.authService.login(username, password).subscribe({
      next: () => this.router.navigate(['/']),
      error: () => this.err = 'Login failed'
    });
  }

}
