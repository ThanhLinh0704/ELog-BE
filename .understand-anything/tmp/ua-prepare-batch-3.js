#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');

try {
  const projectRoot = path.resolve(process.argv[2]);
  const plan = JSON.parse(fs.readFileSync(path.resolve(process.argv[3]), 'utf8'));
  const outputPath = path.resolve(process.argv[4]);
  const batch = plan.batches && plan.batches[2];
  if (!batch || batch.index !== 3) throw new Error('Không tìm thấy batch 3 tại batches[2].');
  if (!Array.isArray(batch.files) || batch.files.length !== 25) throw new Error('Batch 3 không có đúng 25 files.');
  if (!batch.importData || Object.keys(batch.importData).length !== 25) throw new Error('Batch 3 không có đúng 25 importData keys.');
  const output = { projectRoot, batchFiles: batch.files, batchImportData: batch.importData };
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(output, null, 2)}\n`, 'utf8');
  process.exit(0);
} catch (error) {
  process.stderr.write(`${error && error.stack ? error.stack : error}\n`);
  process.exit(1);
}
