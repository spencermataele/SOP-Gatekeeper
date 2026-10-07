import {TestBed,fakeAsync,tick} from '@angular/core/testing';
import {HttpClientTestingModule,HttpTestingController} from '@angular/common/http/testing';
import {ActivatedRoute,convertToParamMap} from '@angular/router';
import {ChangeTrackingComponent} from './change-tracking.component';
describe('Change tracking',()=>{
 beforeEach(async()=>{await TestBed.configureTestingModule({imports:[ChangeTrackingComponent,HttpClientTestingModule],providers:[{provide:ActivatedRoute,useValue:{snapshot:{queryParamMap:convertToParamMap({})}}}]}).compileComponents();});
 afterEach(()=>TestBed.inject(HttpTestingController).verify());
 it('renders manually linked tickets safely and keeps the activity log collapsed',fakeAsync(()=>{
  const f=TestBed.createComponent(ChangeTrackingComponent);f.detectChanges();TestBed.inject(HttpTestingController).expectOne(f.componentInstance.api).flush([]);tick();
  f.componentInstance.selected={title:'Implement labels',state:'PLANNED',canEdit:false,revisions:[],suggestions:[],activity:[],tickets:[{system:'Jira',number:'OPS-123',url:'https://example.atlassian.net/browse/OPS-123'}]};f.detectChanges();
  const link=f.nativeElement.querySelector('a');expect(link.textContent).toContain('OPS-123');expect(link.rel).toBe('noopener noreferrer');expect(f.nativeElement.textContent).toContain('Manual links');expect(f.nativeElement.querySelector('.workflow-log').open).toBeFalse();expect(f.nativeElement.querySelector('form')).toBeNull();
  expect(f.componentInstance.safeUrl('javascript:alert(1)')).toBeNull();expect(f.componentInstance.safeUrl('https://name:password@example.com')).toBeNull();
 }));
 it('keeps blocked changes in the active queue and allows viewing cancelled records',()=>{
  const c=new ChangeTrackingComponent({} as any,{} as any);c.items=[{state:'PLANNED'},{state:'BLOCKED'},{state:'CANCELLED'},{state:'EFFECTIVENESS_VERIFIED'}];expect(c.visible.length).toBe(2);c.filter='ALL';expect(c.visible.length).toBe(4);c.filter='CANCELLED';expect(c.visible.length).toBe(1);
 });
});
