import {TestBed, fakeAsync, tick} from '@angular/core/testing';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {HierarchyCodesComponent} from './hierarchy-codes.component';
import {environment} from '../../../environments/environment';
import {LibraryNode} from '../library/sop-library.component';

describe('Hierarchy code dropdowns',()=>{
 const entries:LibraryNode[]=[
  {nodeKey:'org:1',parentKey:null,name:'First organization',kind:'Organization',code:'01'},
  {nodeKey:'org:2',parentKey:null,name:'Other organization',kind:'Organization',code:'02'},
  {nodeKey:'group:10',parentKey:'org:1',name:'Ten',kind:'Group',code:'10'},
  {nodeKey:'group:2',parentKey:'org:1',name:'Two',kind:'Group',code:'2'},
  {nodeKey:'group:3',parentKey:'org:1',name:'Zulu',kind:'Group',code:null},
  {nodeKey:'group:4',parentKey:'org:1',name:'Alpha',kind:'Group',code:null},
  {nodeKey:'group:5',parentKey:'org:2',name:'Other branch',kind:'Group',code:'01'},
  {nodeKey:'department:1',parentKey:'group:2',name:'Department',kind:'Department',code:'001'}
 ];
 beforeEach(async()=>{await TestBed.configureTestingModule({imports:[HierarchyCodesComponent,HttpClientTestingModule]}).compileComponents();});
 afterEach(()=>TestBed.inject(HttpTestingController).verify());
 function setup(){
  const fixture=TestBed.createComponent(HierarchyCodesComponent);
  fixture.detectChanges();
  TestBed.inject(HttpTestingController).expectOne(environment.apiBaseUrl+'/api/lifecycle/library/hierarchy').flush(entries.map(n=>({...n})));
  tick();fixture.detectChanges();tick();
  return fixture;
 }
 it('renders numeric code order, then alphabetic uncoded entries, within the selected parent',fakeAsync(()=>{
  const fixture=setup();const c=fixture.componentInstance;
  c.choose(0,'org:1');fixture.detectChanges();tick();
  const menus=fixture.nativeElement.querySelectorAll('select') as NodeListOf<HTMLSelectElement>;
  expect(Array.from(menus[1].options).slice(1).map(o=>o.textContent?.trim())).toEqual(['2 · Two','10 · Ten','Needs code · Alpha','Needs code · Zulu']);
  expect(menus[2].disabled).toBeTrue();
  c.choose(1,'group:2');c.choose(2,'department:1');c.reason='Old edit';
  menus[0].value='org:2';menus[0].dispatchEvent(new Event('change'));tick();fixture.detectChanges();
  expect(c.selection).toEqual(['org:2']);expect(c.key).toBe('org:2');expect(c.reason).toBe('');expect(c.code).toBe('02');
  expect(c.options(1).map(n=>n.nodeKey)).toEqual(['group:5']);expect(c.options(2)).toEqual([]);
 }));
 it('saves the selected level with its previous code and reorders the options after saving',fakeAsync(()=>{
  const fixture=setup();const c=fixture.componentInstance;
  c.choose(0,'org:1');c.choose(1,'group:10');c.code='1';c.reason='Correct sequence';
  void c.save();
  const request=TestBed.inject(HttpTestingController).expectOne(environment.apiBaseUrl+'/api/lifecycle/admin/hierarchy/group%3A10/code');
  expect(request.request.method).toBe('PUT');expect(request.request.body).toEqual({code:'1',expectedCode:'10',reason:'Correct sequence'});
  request.flush({});tick();fixture.detectChanges();
  expect(c.key).toBe('group:10');expect(c.options(1)[0].nodeKey).toBe('group:10');expect(c.reason).toBe('');expect(c.error).toBe('');
 }));
});
