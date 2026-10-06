import {Component, Input, OnChanges} from '@angular/core';
import {CommonModule} from '@angular/common';
import {parseTemplate, SopStep, SopTemplate} from './sop-template';

@Component({selector: 'app-sop-content', standalone: true, imports: [CommonModule], template: `
  <article *ngIf="snapshot" class="sop-document">
    <h3>{{snapshot.title || 'Untitled SOP'}}</h3>
    <dl class="context" *ngIf="content?.context as context">
      <dt>Organization</dt><dd>{{context.organization}} / {{context.organizationGroup}}</dd>
      <dt>Department</dt><dd>{{context.department}}<span *ngIf="context.subgroup"> / {{context.subgroup}}</span></dd>
      <dt>Process</dt><dd>{{context.processFamily}} / {{context.process}}</dd>
      <dt>Process owner</dt><dd>{{context.processOwner}}</dd>
    </dl>
    <h4>Purpose</h4><p class="text">{{snapshot.description || 'Not yet provided'}}</p>
    <ng-container *ngIf="content; else legacy">
      <h4>Scope</h4><p class="text">{{content.scope || 'Not specified'}}</p>
      <h4>Procedure</h4>
      <section class="step" *ngFor="let step of content.steps; let i = index">
        <h5>Step {{i + 1}} <span class="change" *ngIf="change(step,i)">{{change(step,i)}}</span></h5>
        <dl><dt>Who</dt><dd>{{step.who || 'Not yet provided'}}</dd><dt>What</dt><dd class="text">{{step.what || 'Not yet provided'}}</dd><dt>Where</dt><dd>{{step.where || 'Not yet provided'}}</dd><ng-container *ngIf="step.notes"><dt>Notes / warnings</dt><dd class="text">{{step.notes}}</dd></ng-container></dl>
      </section>
      <p *ngIf="!content.steps.length">No steps yet.</p>
      <h4>References</h4><p class="text">{{content.references || 'None specified'}}</p>
      <p class="template-version">Standard SOP template · version 2</p>
    </ng-container>
    <ng-template #legacy><h4>Procedure</h4><p class="text">{{snapshot.details}}</p><p class="template-version">Preserved original format</p></ng-template>
  </article>`, styles: [`
    .sop-document{overflow-wrap:anywhere}h3{font-size:21px;margin:20px 0}h4{font-size:15px;border-bottom:1px solid #d5e0e3;padding-bottom:8px;margin-top:24px}h5{font-size:16px;margin:0 0 12px}.text{white-space:pre-wrap;line-height:1.6}.step{border-left:3px solid #39766b;padding:16px;margin:16px 0;background:#fff}dl{margin:0}dt{font-size:12px;font-weight:bold;text-transform:uppercase;color:#506875;margin-top:12px}dd{margin:4px 0;line-height:1.6}.template-version{font-size:12px;color:#607781}.change{font-size:11px;background:#fff0cf;padding:4px 8px;margin-left:8px}.context{font-size:13px}
  `]})
export class SopContentComponent implements OnChanges {
  @Input() snapshot?: {title: string; description: string; details: string};
  @Input() compareWith?: string;
  content: SopTemplate | null = null; previous: SopTemplate | null = null;
  ngOnChanges(): void { this.content = parseTemplate(this.snapshot?.details || ''); this.previous = parseTemplate(this.compareWith || ''); }
  change(step: SopStep, index: number): string {
    if (!this.previous) return '';
    const oldIndex = this.previous.steps.findIndex(s => s.id === step.id);
    if (oldIndex < 0) return 'Added';
    const old = this.previous.steps[oldIndex];
    const changed = ['who', 'what', 'where', 'notes'].some(key => old[key as keyof SopStep] !== step[key as keyof SopStep]);
    return [changed ? 'Edited' : '', oldIndex !== index ? 'Moved' : ''].filter(Boolean).join(' · ');
  }
}
