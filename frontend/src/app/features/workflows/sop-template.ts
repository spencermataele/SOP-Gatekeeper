export interface SopStep { id: string; who: string; what: string; where: string; notes: string; }
export interface SopTemplate {
  template: 'gatekeeper-sop'; schemaVersion: 2; scope: string; references: string;
  subgroupId?: number | null; steps: SopStep[];
  context?: {organization: string; organizationGroup: string; department: string; processFamily: string; process: string; processOwner: string; subgroup?: string};
}
export function blankStep(): SopStep { return {id: crypto.randomUUID(), who: '', what: '', where: '', notes: ''}; }
export function blankTemplate(): SopTemplate { return {template: 'gatekeeper-sop', schemaVersion: 2, scope: '', references: '', steps: [blankStep()]}; }
export function parseTemplate(text: string): SopTemplate | null {
  try {
    const value = JSON.parse(text);
    return value?.template === 'gatekeeper-sop' && value.schemaVersion === 2 && Array.isArray(value.steps) ? value : null;
  } catch { return null; }
}
export function readyToSubmit(text: string): boolean {
  const value = parseTemplate(text);
  return !!value && value.steps.length > 0 && value.steps.length <= 200 && value.steps.every(s => s.who?.trim() && s.what?.trim() && s.where?.trim());
}
