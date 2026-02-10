import {Component, OnInit} from "@angular/core";
import {FormBuilder, Validators} from "@angular/forms";
import {AuthService} from "./auth.service";
import {Router} from "@angular/router";


@Component({
  selector: 'app-login',
  templateUrl: "./login.component.html",
  styleUrls: ['../../../styles.css']
})
export class LoginComponent implements OnInit{

  form = this.fb.group(
    { username: ['', Validators.required], password: ['', Validators.required] }
  );
  err?: string;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.authService.token()) {
      this.router.navigate(['/sops']);
  }
  }

  submit() {
    if (this.form.invalid) return;
    const { username, password } = this.form.value as any;
    this.authService.login(username, password).subscribe({
      next: () => this.router.navigate(['/sops']),
      error: () => this.err = 'Login failed'
    });
  }

}
