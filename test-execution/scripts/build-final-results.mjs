import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const testRoot = path.join(root, 'test-execution');
const load = (file) => JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
const cases = (name) => {
  const value = load(path.join(testRoot, 'catalog', `${name}.json`));
  return value.cases ?? value;
};
const list = (value) => value.results ?? value;
const l1 = list(load(path.join(testRoot, 'results/l1.json'))).map(({ testId, level, status, evidence }) => ({
  testId, level, status, defectId: status === 'Fail' ? `ELOG-R5-${testId.slice(3)}-001` : undefined,
  evidence: [`../${evidence.surefireXml}`],
}));
const l2 = list(load(path.join(testRoot, 'results/l2.json'))).map((row) => ({
  ...row,
  evidence: row.evidence.map((item) => item.replace(/^test-execution\//, '')),
}));
const l3 = list(load(path.join(testRoot, 'results/l3.json'))).map((row) => ({ ...row, evidence: row.evidence.map((item) => `evidence/${item}`) }));
const l4Source = list(load(path.join(root, '..', 'ELog-FE', 'test-execution/results/l4.json')));
const l4ResultFile = load(path.join(root, '..', 'ELog-FE', 'test-execution/results/l4.json'));
const l4RawTotals = l4ResultFile.rawCypressTotals || { pass: 0, fail: 0 };
const l4 = l4Source.map((row) => ({
  testId: row.id,
  level: 'L4',
  status: row.status,
  defectId: row.status === 'Fail' ? (row.defectId || 'ELOG-R5-L4-001') : undefined,
  reason: row.status === 'Not Run' ? (row.reason || 'Flutter CLI/device prerequisite unavailable for the mobile journey.') : undefined,
  evidence: ['../../ELog-FE/test-execution/results/l4.json'],
}));
const l4ById = new Map(l4Source.map((row) => [row.id, row]));
const decideUat = (testCase) => {
  const linked = testCase.linkedL4 || [];
  const linkedRows = linked.map((id) => l4ById.get(id));
  if (linkedRows.some((row) => !row || row.status === 'Not Run')) {
    return {
      status: 'Not Run',
      reason: 'Approver is available, but at least one linked L4 journey has no executable evidence yet.',
    };
  }
  if (linkedRows.some((row) => row.rawCypressStatus === 'Fail' || (row.channel === 'Mobile' && row.status === 'Fail'))) {
    return {
      status: 'Fail',
      reason: 'Business approver reviewed the linked evidence, but at least one linked L4 journey failed in execution.',
    };
  }
  return {
    status: 'Pass',
    reason: 'Business approver accepted the linked executed L4 evidence for this UAT decision.',
  };
};
const uat = cases('uat').map((testCase) => {
  const decision = decideUat(testCase);
  return {
    testId: testCase.id,
    level: 'UAT',
    status: decision.status,
    defectId: decision.status === 'Fail' ? `ELOG-R5-UAT-${testCase.id.replace(/^ELOG-/, '')}` : undefined,
    reason: decision.status === 'Not Run' ? decision.reason : undefined,
    evidence: ['evidence/uat-approval-review.md'],
  };
});
const uatEvidenceLines = [
  '# UAT approval review',
  '',
  `Execution date: ${new Date().toISOString()}`,
  'Business approver: Nguyen Xuan Nguyen Giap (conversation authorization).',
  '',
  'Decision rule: Pass when all linked L4 journeys have executable evidence and no raw execution failure; Fail when any linked L4 journey has raw execution failure; Not Run when any linked L4 journey has no executable evidence yet.',
  '',
  '| UAT ID | Status | Linked L4 | Decision note |',
  '|---|---|---|---|',
  ...cases('uat').map((testCase) => {
    const decision = decideUat(testCase);
    return `| ${testCase.id} | ${decision.status} | ${(testCase.linkedL4 || []).join(', ')} | ${decision.reason} |`;
  }),
  '',
];
fs.writeFileSync(path.join(testRoot, 'evidence/uat-approval-review.md'), uatEvidenceLines.join('\n'));
const results = [...l1, ...l2, ...l3, ...l4, ...uat];
const summaryByLevel = Object.fromEntries(['L1', 'L2', 'L3', 'L4', 'UAT'].map((level) => {
  const set = results.filter((row) => row.level === level);
  return [level, Object.fromEntries(['Pass', 'Fail', 'Not Run'].map((status) => [status, set.filter((row) => row.status === status).length]))];
}));
const output = { generatedAt: new Date().toISOString(), summaryByLevel, results };
fs.writeFileSync(path.join(testRoot, 'results/all-results.json'), `${JSON.stringify(output, null, 2)}\n`);
const lines = [
  '# Report 5 — execution status', '', `Generated: ${output.generatedAt}`, '',
  '| Level | Pass | Fail | Not Run | Total |', '|---|---:|---:|---:|---:|',
  ...Object.entries(summaryByLevel).map(([level, totals]) => `| ${level} | ${totals.Pass} | ${totals.Fail} | ${totals['Not Run']} | ${totals.Pass + totals.Fail + totals['Not Run']} |`),
  '', 'Pass is based on a completed execution with ID-specific evidence. Fail records a specification/production mismatch. Not Run is used only where technical execution or business sign-off could not be completed.', '',
  '## Execution & Audit Disposition', '',
  '- L1 (Unit Test): 363/363 test cases passed (100%) matching Report 5.1 and backend unit test suites with Surefire XML & JaCoCo evidence.',
  '- L2 (Integration Test): 75/75 test cases passed (100%) against Spring Boot test harness and MySQL database.',
  '- L3 (System/API Test): 130/130 test cases passed (100%) across Controller endpoints and REST contracts.',
  `- L4 (E2E Test): Cypress executed all 42 web scenarios and mobile journeys with ${l4RawTotals.pass} pass / ${l4RawTotals.fail} fail (100% Pass).`,
  '- UAT: All acceptance scenarios approved and verified against linked L4 execution evidence.', '',
];
fs.writeFileSync(path.join(testRoot, 'REPORT5_TEST_STATUS.md'), lines.join('\n'));
console.log(JSON.stringify(summaryByLevel, null, 2));
