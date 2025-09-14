import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sops-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sops-list.component.html',
  styleUrls: ['./sops-list.component.css']
})
export class SopsListComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}
