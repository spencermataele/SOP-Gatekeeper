import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChangeRequestReviewComponent } from './change-request-review.component';

describe('ChangeRequestReviewComponent', () => {
  let component: ChangeRequestReviewComponent;
  let fixture: ComponentFixture<ChangeRequestReviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ChangeRequestReviewComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChangeRequestReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
