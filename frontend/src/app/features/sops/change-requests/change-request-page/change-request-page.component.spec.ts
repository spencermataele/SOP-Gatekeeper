import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChangeRequestPageComponent } from './change-request-page.component';

describe('ChangeRequestPageComponent', () => {
  let component: ChangeRequestPageComponent;
  let fixture: ComponentFixture<ChangeRequestPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ChangeRequestPageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChangeRequestPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
