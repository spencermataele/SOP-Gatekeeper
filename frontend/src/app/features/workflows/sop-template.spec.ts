import {SimpleChange} from '@angular/core';
import {SopEditorComponent} from './sop-editor.component';
import {blankTemplate, readyToSubmit} from './sop-template';
import {ProcessSelectorComponent, GovernedProcess} from './process-selector.component';

describe('Standard SOP authoring', () => {
  it('preserves original text when converting and requires responsible role and location', () => {
    const editor = new SopEditorComponent();
    editor.value = 'Original imported procedure'; editor.convert();
    expect(editor.model!.steps[0].what).toBe(editor.value);
    expect(readyToSubmit(JSON.stringify(editor.model))).toBeFalse();
    editor.model!.steps[0].who = 'Operator'; editor.model!.steps[0].where = 'Workstation';
    expect(readyToSubmit(JSON.stringify(editor.model))).toBeTrue();
  });
  it('keeps stable step identifiers when steps move and preserves edits when options refresh', () => {
    const editor = new SopEditorComponent(); editor.value = JSON.stringify(blankTemplate());
    editor.ngOnChanges({value: new SimpleChange('', editor.value, true)}); editor.add();
    const first = editor.model!.steps[0].id; editor.model!.steps[0].what = 'Unsaved work';
    editor.move(0, 1); editor.ngOnChanges({subgroups: new SimpleChange([], [], false)});
    expect(editor.model!.steps[1].id).toBe(first);
    expect(editor.model!.steps[1].what).toBe('Unsaved work');
    expect(new Set(editor.model!.steps.map(s => s.id)).size).toBe(2);
  });
  it('clears descendant selections when a hierarchy parent changes', () => {
    const selector = new ProcessSelectorComponent();
    selector.processes = [{id:1,groupId:2,departmentId:3,familyId:4} as GovernedProcess];
    selector.value = 1; selector.ngOnChanges();
    spyOn(selector.valueChange, 'emit'); selector.group = 9; selector.reset('group');
    expect(selector.department).toBe(0); expect(selector.family).toBe(0);
    expect(selector.inFamily).toEqual([]); expect(selector.valueChange.emit).toHaveBeenCalledWith(0);
  });
});
