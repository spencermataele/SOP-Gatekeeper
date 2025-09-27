import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SopsFormComponent } from './sops-form.component';

describe('SopsFormComponent', () => {
  let component: SopsFormComponent;
  let fixture: ComponentFixture<SopsFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ SopsFormComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SopsFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
