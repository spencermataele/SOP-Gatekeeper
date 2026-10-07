import {AdminParentSelectorComponent} from './admin-parent-selector.component';
describe('Cascading Admin parent selection',()=>{
 const entries=[{nodeKey:'org:1',parentKey:null,name:'Org',kind:'Organization'},{nodeKey:'group:2',parentKey:'org:1',name:'Group',kind:'Group'},{nodeKey:'department:3',parentKey:'group:2',name:'Department',kind:'Department'},{nodeKey:'subgroup:4',parentKey:'department:3',name:'Subdepartment',kind:'Subgroup'},{nodeKey:'family:5',parentKey:'subgroup:4',name:'Family',kind:'Process family'},{nodeKey:'group:6',parentKey:'org:9',name:'Other group',kind:'Group'}];
 it('filters each dropdown to the selected parent and invalidates descendants on parent changes',()=>{
  const c=new AdminParentSelectorComponent({} as any);c.target='family';c.nodes=entries;const emit=jasmine.createSpy();c.registerOnChange(emit);
  c.choose(0,'org:1');expect(c.options(1).map(n=>n.nodeKey)).toEqual(['group:2']);expect(emit).toHaveBeenCalledWith(null);
  c.choose(1,'group:2');c.choose(2,'department:3');c.choose(3,'subgroup:4');c.choose(4,'family:5');expect(emit).toHaveBeenCalledWith(5);
  c.choose(0,'org:9');expect(c.selection).toEqual(['org:9']);expect(emit.calls.mostRecent().args).toEqual([null]);expect(c.options(2)).toEqual([]);
 });
 it('reconstructs the full path when an existing record is edited',()=>{
  const c=new AdminParentSelectorComponent({} as any);c.target='subgroup';c.nodes=entries;c.writeValue(4);
  expect(c.selection).toEqual(['org:1','group:2','department:3','subgroup:4']);c.writeValue(null);expect(c.selection).toEqual([]);
 });
});
