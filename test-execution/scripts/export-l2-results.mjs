import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const readCatalog = () => {
  const value = JSON.parse(fs.readFileSync(path.join(root, 'test-execution/catalog/l2.json'), 'utf8').replace(/^\uFEFF/, ''));
  return value.cases ?? value;
};
const evidence = 'evidence/l2-junit-results.ndjson';
const rawEvidence = fs.readFileSync(path.join(root, 'test-execution', evidence), 'utf8');
const latest = new Map();
const entryPattern = /\{"testId":"([^"]+)","status":"([^"]+)","class":"([^"]+)","method":"([^"]+)"/g;
for (const match of rawEvidence.matchAll(entryPattern)) {
  latest.set(match[1], { testId: match[1], status: match[2], class: match[3], method: match[4] });
}
const defects = new Map([
  ['L2-DRV-01', 'ELOG-R5-DRV-001'],
  ['L2-IMP-03', 'ELOG-R5-IMP-001'],
  ['L2-RCD-01', 'ELOG-R5-RCD-001'],
  ['L2-RCD-02', 'ELOG-R5-RCD-001'],
  ['L2-RCD-03', 'ELOG-R5-RCD-001'],
]);
const results = readCatalog().map((testCase) => {
  const raw = latest.get(testCase.id);
  if (!raw) throw new Error(`No execution record for ${testCase.id}`);
  return {
    testId: testCase.id,
    level: 'L2',
    status: raw.status,
    defectId: raw.status === 'Fail' ? defects.get(testCase.id) : undefined,
    evidence: [`test-execution/${evidence}`],
    detail: `${raw.class}#${raw.method}`,
  };
});
const summary = Object.fromEntries(['Pass', 'Fail', 'Not Run'].map((status) => [status, results.filter((x) => x.status === status).length]));
const output = { generatedAt: new Date().toISOString(), level: 'L2', summary, results };
fs.mkdirSync(path.join(root, 'test-execution/results'), { recursive: true });
fs.writeFileSync(path.join(root, 'test-execution/results/l2.json'), `${JSON.stringify(output, null, 2)}\n`);
console.log(JSON.stringify(summary));
