import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SopsListComponent } from './sops-list.component';

describe('SopsListComponent', () => {
  let component: SopsListComponent;
  let fixture: ComponentFixture<SopsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ SopsListComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SopsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
