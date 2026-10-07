import {TestBed,fakeAsync,tick} from '@angular/core/testing';
import {HttpClientTestingModule,HttpTestingController} from '@angular/common/http/testing';
import {ActivatedRoute,convertToParamMap} from '@angular/router';
import {SuggestionsComponent} from './suggestions.component';

describe('Suggestions and alignment',()=>{
 it('shows N/A while preserving the NONE storage value',()=>{const c=new SuggestionsComponent({} as any,{} as any);expect(c.label('NONE')).toBe('N/A');expect(c.concerns).toContain('NONE');expect(c.label('UNDETERMINED')).toBe('undetermined');});
 beforeEach(async()=>{await TestBed.configureTestingModule({imports:[SuggestionsComponent,HttpClientTestingModule],providers:[{provide:ActivatedRoute,useValue:{snapshot:{queryParamMap:convertToParamMap({})}}}]}).compileComponents();});
 afterEach(()=>TestBed.inject(HttpTestingController).verify());
 function setup(){const f=TestBed.createComponent(SuggestionsComponent);f.detectChanges();const http=TestBed.inject(HttpTestingController);for(const name of ['processes','documents','suggestions'])http.expectOne(f.componentInstance.api+'/'+name).flush([]);tick();f.detectChanges();return f;}
 it('requires a specific process and all required narrative fields before submitting',fakeAsync(()=>{
  const f=setup(),c=f.componentInstance;c.title='Improve';c.problem='Problem';c.proposal='Change';c.benefit='Benefit';
  void c.submit();TestBed.inject(HttpTestingController).expectNone(c.api+'/suggestions');
  c.processId=1;void c.submit();const post=TestBed.inject(HttpTestingController).expectOne(c.api+'/suggestions');expect(post.request.body.processId).toBe(1);expect(post.request.body.documentId).toBeNull();
  post.flush({suggestion_id:'abc',state:'SUBMITTED',concern:'UNDETERMINED'});tick();TestBed.inject(HttpTestingController).expectOne(c.api+'/suggestions').flush([]);tick();expect(c.creating).toBeFalse();
 }));
 it('retains failed responses and reuses the command for uncertain retries',fakeAsync(()=>{
  const f=setup(),c=f.componentInstance;c.selected={suggestion_id:'abc',lock_version:2};c.response='I disagree because the error recurred';
  void c.act('CHALLENGE');const http=TestBed.inject(HttpTestingController);const first=http.expectOne(c.api+'/suggestions/abc/actions');const id=first.request.body.commandId;
  first.error(new ProgressEvent('error'));tick();expect(c.response).toContain('error recurred');void c.act('CHALLENGE');const retry=http.expectOne(c.api+'/suggestions/abc/actions');expect(retry.request.body.commandId).toBe(id);retry.flush({suggestion_id:'abc',lock_version:3,concern:'DEFECT',state:'ALIGNMENT_UNRESOLVED'});tick();http.expectOne(c.api+'/suggestions').flush([]);tick();
 }));
 it('separates unresolved defects from other pending suggestions and retains challenged defects',fakeAsync(()=>{
  const c=setup().componentInstance;c.items=[{concern:'DEFECT',state:'DECLINED_AWAITING_RESPONSE'},{concern:'BOTH',state:'ALIGNMENT_UNRESOLVED'},{concern:'DEFECT',state:'CLOSED'},{concern:'TRAINING',state:'FOLLOW_UP_REQUIRED'}];
  c.queue='defects';expect(c.visible.length).toBe(2);c.queue='pending';expect(c.visible.length).toBe(1);c.queue='alignment';expect(c.visible.length).toBe(1);
 }));
 it('shows agreement and challenge for the submitter without treating decline as closure',fakeAsync(()=>{
  const f=setup(),c=f.componentInstance;c.selected={suggestion_id:'abc',state:'DECLINED_AWAITING_RESPONSE',canRespond:true,canReview:false,communications:[],recipients:[]};f.detectChanges();
  expect(f.nativeElement.textContent).toContain('Agree with explanation and next steps');expect(f.nativeElement.textContent).toContain('Challenge / request alignment');expect(f.nativeElement.textContent).not.toContain('Agree resolution is verified');expect(f.nativeElement.textContent).not.toContain('Accept for implementation');
 }));
 it('shows an implementation handoff after acceptance and keeps history collapsed',fakeAsync(()=>{
  const f=setup(),c=f.componentInstance;c.selected={state:'ACCEPTED',decision:'ACCEPTED',canReview:true,canRespond:false,concern:'NONE',changes:[{id:'change-1',title:'Improve labels',state:'PLANNED',implementationOwner:'Owner'}],revisions:[],communications:[],recipients:[]};f.detectChanges();
  expect(f.nativeElement.textContent).toContain('Accepted for implementation');expect(f.nativeElement.textContent).toContain('Outcome verification & alignment');expect(f.nativeElement.textContent).not.toContain('Decline proposed solution');expect(f.nativeElement.querySelector('a[href="/changes?change=change-1"]')).not.toBeNull();expect(f.nativeElement.querySelector('details.workflow-log').open).toBeFalse();
 }));
});
