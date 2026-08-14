import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ALLOWED_STATUSES = new Set(['Pass', 'Fail', 'Not Run']);

export function parseJson(text) {
  return JSON.parse(text.replace(/^\uFEFF/, ''));
}

export function validateLedger({ catalogs, results, evidenceExists, evidenceContains = () => true }) {
  const errors = [];
  const expected = new Map();

  for (const [level, cases] of Object.entries(catalogs)) {
    for (const testCase of cases) {
      if (expected.has(testCase.id)) errors.push(`duplicate catalog id ${testCase.id}`);
      expected.set(testCase.id, level);
    }
  }

  const actualCounts = new Map();
  for (const result of results) {
    actualCounts.set(result.testId, (actualCounts.get(result.testId) ?? 0) + 1);
  }

  for (const [testId, count] of actualCounts) {
    if (count > 1) errors.push(`duplicate result id ${testId}`);
    if (!expected.has(testId)) errors.push(`extra result id ${testId}`);
  }
  for (const testId of expected.keys()) {
    if (!actualCounts.has(testId)) errors.push(`missing result id ${testId}`);
  }

  for (const result of results) {
    const expectedLevel = expected.get(result.testId);
    if (expectedLevel && result.level !== expectedLevel) {
      errors.push(`${result.testId} level ${result.level} does not match ${expectedLevel}`);
    }
    if (!ALLOWED_STATUSES.has(result.status)) {
      errors.push(`${result.testId} has invalid status ${result.status}`);
      continue;
    }
    if (result.status === 'Not Run') {
      if (!String(result.reason ?? '').trim()) errors.push(`${result.testId} Not Run requires reason`);
      const evidence = Array.isArray(result.evidence) ? result.evidence : [];
      if (evidence.length === 0) errors.push(`${result.testId} Not Run requires attempted-execution evidence`);
      for (const locator of evidence) {
        if (!evidenceExists(locator)) errors.push(`${result.testId} evidence does not exist: ${locator}`);
      }
      continue;
    }
    const evidence = Array.isArray(result.evidence) ? result.evidence : [];
    if (evidence.length === 0) errors.push(`${result.testId} ${result.status} requires evidence`);
    for (const locator of evidence) {
      if (!evidenceExists(locator)) errors.push(`${result.testId} evidence does not exist: ${locator}`);
      else if (!evidenceContains(locator, result.testId)) {
        errors.push(`${result.testId} evidence does not identify its test id: ${locator}`);
      }
    }
    if (result.status === 'Fail' && !String(result.defectId ?? '').trim()) {
      errors.push(`${result.testId} Fail requires defectId`);
    }
  }

  return errors;
}

function loadCatalog(filePath) {
  const value = parseJson(fs.readFileSync(filePath, 'utf8'));
  return Array.isArray(value) ? value : value.cases;
}

function main() {
  const args = process.argv.slice(2);
  const getArg = (name) => {
    const index = args.indexOf(name);
    if (index < 0 || index === args.length - 1) throw new Error(`Missing ${name}`);
    return path.resolve(args[index + 1]);
  };
  const catalogDir = getArg('--catalog-dir');
  const resultsFile = getArg('--results');
  const evidenceRoot = getArg('--evidence-root');
  const catalogs = Object.fromEntries(
    ['l1', 'l2', 'l3', 'l4', 'uat'].map((level) => [
      level.toUpperCase(),
      loadCatalog(path.join(catalogDir, `${level}.json`)),
    ]),
  );
  const resultsValue = parseJson(fs.readFileSync(resultsFile, 'utf8'));
  const results = Array.isArray(resultsValue) ? resultsValue : resultsValue.results;
  const errors = validateLedger({
    catalogs,
    results,
    evidenceExists: (locator) => fs.existsSync(path.resolve(evidenceRoot, locator)),
    evidenceContains: (locator, testId) => fs.readFileSync(path.resolve(evidenceRoot, locator), 'utf8').includes(testId),
  });
  if (errors.length) {
    console.error(errors.join('\n'));
    process.exitCode = 1;
  } else {
    console.log(`PASS: ${results.length} unique auditable results`);
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) main();
