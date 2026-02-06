import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SopViewComponent } from './sop-view.component';

describe('SopViewComponent', () => {
  let component: SopViewComponent;
  let fixture: ComponentFixture<SopViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SopViewComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SopViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
