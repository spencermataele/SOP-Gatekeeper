import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {blankStep, blankTemplate, parseTemplate, SopTemplate} from './sop-template';
@Component({selector: 'app-sop-editor', standalone: true, imports: [CommonModule, FormsModule], template: `
  <div *ngIf="!model" class="legacy">
    <p>This procedure uses an earlier format. Convert your working copy to the standard template before submitting. The original publication and historical snapshots are preserved.</p>
    <details><summary>Original procedure text</summary><pre>{{value}}</pre></details>
    <button type="button" (click)="convert()" [disabled]="disabled">Use standard template</button>
  </div>
  <fieldset *ngIf="model" [disabled]="disabled">
    <p>Use Who, What, and Where for every step. Gatekeeper controls numbering and the published layout. Purpose and complete steps are required for submission; scope, references, and notes are optional.</p>
    <label>Scope (optional)<textarea [(ngModel)]="model.scope" [ngModelOptions]="{standalone:true}" (ngModelChange)="changed()" maxlength="20000"></textarea></label>
    <label *ngIf="subgroups.length">Department subgroup (optional)<select [(ngModel)]="model.subgroupId" [ngModelOptions]="{standalone:true}" (ngModelChange)="changed()"><option [ngValue]="null">Entire process</option><option *ngFor="let subgroup of subgroups" [ngValue]="subgroup.id">{{subgroup.name}}</option></select></label>
    <section *ngFor="let step of model.steps; let i=index; trackBy: trackStep" class="step-editor">
      <div class="step-heading"><h4>Step {{i + 1}}</h4><div><button type="button" (click)="move(i,-1)" [disabled]="i === 0" [attr.aria-label]="'Move step '+(i+1)+' up'">↑ Move up</button> <button type="button" (click)="move(i,1)" [disabled]="i === model.steps.length-1" [attr.aria-label]="'Move step '+(i+1)+' down'">↓ Move down</button> <button type="button" (click)="remove(i)" [attr.aria-label]="'Remove step '+(i+1)">Remove</button></div></div>
      <label>Who<input [(ngModel)]="step.who" [ngModelOptions]="{standalone:true}" (ngModelChange)="changed()" placeholder="Responsible role or team" maxlength="20000"></label>
      <label>What<textarea [(ngModel)]="step.what" [ngModelOptions]="{standalone:true}" (ngModelChange)="changed()" placeholder="Action and expected outcome" maxlength="20000"></textarea></label>
      <label>Where<input [(ngModel)]="step.where" [ngModelOptions]="{standalone:true}" (ngModelChange)="changed()" placeholder="Location, system, or work area" maxlength="20000"></label>
      <label>Notes / warnings (optional)<textarea [(ngModel)]="step.notes" [ngModelOptions]="{standalone:true}" (ngModelChange)="changed()" maxlength="20000"></textarea></label>
    </section>
    <button type="button" (click)="add()" [disabled]="model.steps.length >= 200">Add step</button>
    <label>References (optional)<textarea [(ngModel)]="model.references" [ngModelOptions]="{standalone:true}" (ngModelChange)="changed()" maxlength="20000"></textarea></label>
  </fieldset>`, styles: [`fieldset{border:0;padding:0;min-width:0}label{display:block;font-weight:600;margin:16px 0}input,textarea,select{box-sizing:border-box;display:block;width:100%;border:1px solid #a8b8c1;border-radius:6px;padding:12px;margin-top:8px;font:inherit}textarea{min-height:80px;resize:vertical}.step-editor{border:1px solid #cbdadf;padding:18px;margin:18px 0;border-radius:8px;background:#f8fafb}.step-heading{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px}button{border:1px solid #176458;border-radius:6px;background:white;color:#176458;padding:8px;font:inherit;cursor:pointer}button:disabled{opacity:.5}pre{white-space:pre-wrap}.legacy{padding:16px;background:#fff3d9}p{line-height:1.6}`]})
export class SopEditorComponent implements OnChanges {
  @Input() value = ''; @Input() disabled = false;
  @Input() subgroups: {id: number; name: string}[] = [];
  @Output() valueChange = new EventEmitter<string>();
  model: SopTemplate | null = null;
  ngOnChanges(changes: SimpleChanges): void { if (changes['value']) this.model = parseTemplate(this.value); }
  convert(): void { this.model = blankTemplate(); this.model.steps[0].what = this.value; this.changed(); }
  changed(): void { this.valueChange.emit(JSON.stringify(this.model)); }
  add(): void { this.model!.steps.push(blankStep()); this.changed(); }
  remove(index: number): void { if (window.confirm('Remove this step from your working copy?')) { this.model!.steps.splice(index, 1); this.changed(); } }
  move(index: number, direction: number): void { const steps=this.model!.steps; const [step]=steps.splice(index,1); steps.splice(index+direction,0,step); this.changed(); }
  trackStep(index: number, step: {id: string}): string { return step.id; }
}
