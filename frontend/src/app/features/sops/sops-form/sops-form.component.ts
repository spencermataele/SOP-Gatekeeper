import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sops-form',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sops-form.component.html',
  styleUrls: ['./sops-form.component.css']
})
export class SopsFormComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}
