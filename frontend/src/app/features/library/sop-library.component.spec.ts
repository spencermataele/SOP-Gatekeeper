import {environment} from '../../../environments/environment';
import {TestBed, fakeAsync, tick} from '@angular/core/testing';
import {HttpClientTestingModule,HttpTestingController} from '@angular/common/http/testing';
import {SopLibraryComponent,LibraryNode,LibraryDocument} from './sop-library.component';
const nodes:LibraryNode[]=[{nodeKey:'org:9',parentKey:null,name:'Example',kind:'Organization',code:'01'},{nodeKey:'department:8',parentKey:'org:9',name:'Operations',kind:'Department',code:'002'},{nodeKey:'process:7',parentKey:'department:8',name:'Setup',kind:'Process',code:'003'},{nodeKey:'department:6',parentKey:'org:9',name:'Finance',kind:'Department',code:'004'}];
const doc:LibraryDocument={documentId:1,nodeKey:'process:7',title:'Initial Setup',description:'Prepare a workstation',details:'Published procedure',revisionId:97,publishedVersion:3,kind:'CLIENT_SOP'};
describe('Published SOP library',()=>{
 it('offers only subgroups beneath departments without hiding department SOPs',()=>{
  const c=new SopLibraryComponent({} as any);
  const subgroup:LibraryNode={nodeKey:'subgroup:5',parentKey:'department:8',name:'Team',kind:'Subgroup',code:'001'};
  c.nodes=[...nodes.filter(n=>n.nodeKey!=='process:7'),subgroup,{nodeKey:'family:4',parentKey:'subgroup:5',name:'Family',kind:'Process family',code:'002'},{...nodes[2],parentKey:'family:4'}];
  c.documents=[doc];c.choose('department:8');
  expect(c.children).toEqual([subgroup]);expect(c.filtered).toEqual([doc]);
  c.choose('subgroup:5');expect(c.children.map(n=>n.nodeKey)).toEqual(['family:4']);expect(c.filtered).toEqual([doc]);
 });
 it('includes descendants, excludes other branches, and searches within the selection',()=>{
  const c=new SopLibraryComponent({} as any);c.nodes=nodes;c.documents=[doc];c.choose('org:9');expect(c.filtered).toEqual([doc]);c.choose('department:6');expect(c.filtered).toEqual([]);c.choose('department:8');c.search='initial';expect(c.filtered).toEqual([doc]);c.search='missing';expect(c.filtered).toEqual([]);
 });
 it('uses stored hierarchy codes and publication numbers rather than database revision IDs',()=>{
  const c=new SopLibraryComponent({} as any);c.nodes=nodes;expect(c.label(doc)).toContain('01-002-003');expect(c.label(doc)).toContain('Initial Setup · v3');expect(c.label(doc)).not.toContain('97');
 });
 beforeEach(async()=>{await TestBed.configureTestingModule({imports:[SopLibraryComponent,HttpClientTestingModule]}).compileComponents();});
 it('fits long navigation and SOP text within the cards',fakeAsync(()=>{
  const fixture=TestBed.createComponent(SopLibraryComponent);const http=TestBed.inject(HttpTestingController);
  fixture.nativeElement.style.width='960px';fixture.detectChanges();
  http.expectOne(environment.apiBaseUrl+'/api/lifecycle/library/hierarchy').flush([{...nodes[0],name:'VeryLongOrganizationName'.repeat(15)},...nodes.slice(1)]);
  http.expectOne(environment.apiBaseUrl+'/api/lifecycle/documents').flush([{...doc,title:'VeryLongProcedureTitle'.repeat(15),description:'Detailed purpose '.repeat(30)}]);
  tick();fixture.detectChanges();
  const buttons:HTMLElement[]=Array.from(fixture.nativeElement.querySelectorAll('.child,.document'));
  expect(buttons.length).toBe(2);
  buttons.forEach(button=>{
   expect(button.scrollWidth).toBeLessThanOrEqual(button.clientWidth+1);
   expect(button.scrollHeight).toBeLessThanOrEqual(button.clientHeight+1);
   expect(button.getBoundingClientRect().width).toBeLessThanOrEqual(button.parentElement!.getBoundingClientRect().width);
  });
  http.verify();
 }));
 it('opens only the current publication until history is explicitly requested',fakeAsync(()=>{
  const fixture=TestBed.createComponent(SopLibraryComponent);const c=fixture.componentInstance;const http=TestBed.inject(HttpTestingController);fixture.detectChanges();
  http.expectOne(environment.apiBaseUrl+'/api/lifecycle/library/hierarchy').flush(nodes);http.expectOne(environment.apiBaseUrl+'/api/lifecycle/documents').flush([doc]);tick();fixture.detectChanges();
  fixture.nativeElement.querySelector('.document').click();fixture.detectChanges();expect(fixture.nativeElement.querySelectorAll('app-sop-content').length).toBe(1);expect(fixture.nativeElement.querySelector('.history')).toBeNull();
  void c.compare();http.expectOne(environment.apiBaseUrl+'/api/lifecycle/documents/1/history').flush([{id:97,title:'Initial Setup',description:'Purpose',details:'Current'},{id:11,title:'Earlier',description:'Purpose',details:'Earlier'}]);tick();fixture.detectChanges();expect(fixture.nativeElement.querySelectorAll('app-sop-content').length).toBe(3);http.verify();
 }));
});
