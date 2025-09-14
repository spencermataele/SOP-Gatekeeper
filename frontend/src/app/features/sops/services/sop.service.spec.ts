import { TestBed } from '@angular/core/testing';

import { SopService } from './sop.service';

describe('SopService', () => {
  let service: SopService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SopService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
