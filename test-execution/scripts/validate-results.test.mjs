import assert from 'node:assert/strict';
import test from 'node:test';

import { parseJson } from './validate-results.mjs';
import { validateLedger } from './validate-results.mjs';

const catalogs = {
  L1: [{ id: 'L1-A' }, { id: 'L1-B' }],
  L2: [{ id: 'L2-A' }],
};

const evidenceExists = (path) => path === 'evidence/ok.xml';

test('accepts exactly one auditable result for every catalog id', () => {
  const results = [
    { testId: 'L1-A', level: 'L1', status: 'Pass', evidence: ['evidence/ok.xml'] },
    { testId: 'L1-B', level: 'L1', status: 'Fail', evidence: ['evidence/ok.xml'], defectId: 'BUG-1' },
    { testId: 'L2-A', level: 'L2', status: 'Not Run', reason: 'Stakeholder approval is unavailable.', evidence: ['evidence/ok.xml'] },
  ];

  assert.deepEqual(validateLedger({ catalogs, results, evidenceExists }), []);
});

test('rejects missing, extra and duplicate ids', () => {
  const results = [
    { testId: 'L1-A', level: 'L1', status: 'Pass', evidence: ['evidence/ok.xml'] },
    { testId: 'L1-A', level: 'L1', status: 'Pass', evidence: ['evidence/ok.xml'] },
    { testId: 'L9-X', level: 'L9', status: 'Pass', evidence: ['evidence/ok.xml'] },
  ];

  const errors = validateLedger({ catalogs, results, evidenceExists });
  assert(errors.some((error) => error.includes('duplicate result id L1-A')));
  assert(errors.some((error) => error.includes('missing result id L1-B')));
  assert(errors.some((error) => error.includes('missing result id L2-A')));
  assert(errors.some((error) => error.includes('extra result id L9-X')));
});

test('rejects pass without existing evidence and fail without a defect', () => {
  const results = [
    { testId: 'L1-A', level: 'L1', status: 'Pass', evidence: ['evidence/missing.xml'] },
    { testId: 'L1-B', level: 'L1', status: 'Fail', evidence: ['evidence/ok.xml'] },
    { testId: 'L2-A', level: 'L2', status: 'Not Run', reason: '' },
  ];

  const errors = validateLedger({ catalogs, results, evidenceExists });
  assert(errors.some((error) => error.includes('L1-A evidence does not exist')));
  assert(errors.some((error) => error.includes('L1-B Fail requires defectId')));
  assert(errors.some((error) => error.includes('L2-A Not Run requires reason')));
});

test('rejects generic suite evidence that does not identify the test id', () => {
  const results = [
    { testId: 'L1-A', level: 'L1', status: 'Pass', evidence: ['evidence/ok.xml'] },
    { testId: 'L1-B', level: 'L1', status: 'Not Run', reason: 'No matching assertion.' },
    { testId: 'L2-A', level: 'L2', status: 'Not Run', reason: 'No matching assertion.' },
  ];

  const errors = validateLedger({
    catalogs,
    results,
    evidenceExists,
    evidenceContains: () => false,
  });
  assert(errors.some((error) => error.includes('L1-A evidence does not identify its test id')));
});

test('rejects a Not Run result that lacks an attempted-execution artifact', () => {
  const results = [
    { testId: 'L1-A', level: 'L1', status: 'Not Run', reason: 'Dependency unavailable.', evidence: [] },
    { testId: 'L1-B', level: 'L1', status: 'Not Run', reason: 'Dependency unavailable.', evidence: ['evidence/ok.xml'] },
    { testId: 'L2-A', level: 'L2', status: 'Not Run', reason: 'Dependency unavailable.', evidence: ['evidence/ok.xml'] },
  ];
  const errors = validateLedger({ catalogs, results, evidenceExists });
  assert(errors.some((error) => error.includes('L1-A Not Run requires attempted-execution evidence')));
});

test('parses PowerShell UTF-8 JSON with a BOM', () => {
  assert.deepEqual(parseJson('\uFEFF{"cases":[]}'), { cases: [] });
});
