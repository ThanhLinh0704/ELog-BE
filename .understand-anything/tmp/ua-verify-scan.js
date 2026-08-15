#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');

const projectRoot = path.resolve(process.argv[2]);
const intermediatePath = path.resolve(process.argv[3]);
const finalPath = path.resolve(process.argv[4]);
const intermediate = JSON.parse(fs.readFileSync(intermediatePath, 'utf8'));
const result = JSON.parse(fs.readFileSync(finalPath, 'utf8'));
const errors = [];
const expectedFields = ['name', 'description', 'languages', 'frameworks', 'files', 'totalFiles', 'filteredByIgnore', 'estimatedComplexity', 'importMap'];
const actualFields = Object.keys(result);

if (JSON.stringify(actualFields) !== JSON.stringify(expectedFields)) {
  errors.push(`Top-level fields không đúng: ${actualFields.join(', ')}`);
}
for (const forbidden of ['scriptCompleted', 'rawDescription', 'readmeHead']) {
  if (Object.prototype.hasOwnProperty.call(result, forbidden)) errors.push(`Còn field trung gian: ${forbidden}`);
}
for (const field of ['name', 'languages', 'frameworks', 'files', 'totalFiles', 'filteredByIgnore', 'estimatedComplexity', 'importMap']) {
  if (JSON.stringify(result[field]) !== JSON.stringify(intermediate[field])) {
    errors.push(`Field cấu trúc bị thay đổi trong Phase 2: ${field}`);
  }
}
if (result.totalFiles !== result.files.length) errors.push('totalFiles không khớp files.length.');
if (!['small', 'moderate', 'large', 'very-large'].includes(result.estimatedComplexity)) errors.push('estimatedComplexity không hợp lệ.');
if (!Number.isInteger(result.filteredByIgnore) || result.filteredByIgnore < 0) errors.push('filteredByIgnore không hợp lệ.');
if (result.totalFiles > 100 && !result.description.includes('hơn 100 tệp nguồn')) errors.push('Thiếu ghi chú cho dự án trên 100 files.');

const allowedCategories = new Set(['code', 'config', 'docs', 'infra', 'data', 'script', 'markup']);
const paths = result.files.map((file) => file.path);
const pathSet = new Set(paths);
if (pathSet.size !== paths.length) errors.push('Danh sách files có path trùng lặp.');
for (let index = 1; index < paths.length; index += 1) {
  if (paths[index - 1].localeCompare(paths[index]) > 0) {
    errors.push(`Danh sách files chưa sort tại ${paths[index]}.`);
    break;
  }
}
for (const file of result.files) {
  if (path.isAbsolute(file.path) || file.path.split('/').includes('..')) errors.push(`Path không phải project-relative an toàn: ${file.path}`);
  const absolutePath = path.join(projectRoot, ...file.path.split('/'));
  if (!fs.existsSync(absolutePath) || !fs.statSync(absolutePath).isFile()) errors.push(`Path không tồn tại: ${file.path}`);
  if (typeof file.language !== 'string' || !file.language) errors.push(`Language không hợp lệ: ${file.path}`);
  if (!Number.isInteger(file.sizeLines) || file.sizeLines < 0) errors.push(`sizeLines không hợp lệ: ${file.path}`);
  if (!allowedCategories.has(file.fileCategory)) errors.push(`fileCategory không hợp lệ: ${file.path}`);
}

const importKeys = Object.keys(result.importMap);
if (importKeys.length !== paths.length || importKeys.some((key) => !pathSet.has(key)) || paths.some((filePath) => !Object.prototype.hasOwnProperty.call(result.importMap, filePath))) {
  errors.push('importMap không có đúng một key cho mỗi file.');
}
for (const file of result.files) {
  const targets = result.importMap[file.path];
  if (!Array.isArray(targets)) {
    errors.push(`Import list không phải array: ${file.path}`);
    continue;
  }
  if (file.fileCategory !== 'code' && targets.length > 0) errors.push(`Non-code file có import: ${file.path}`);
  for (const target of targets) if (!pathSet.has(target)) errors.push(`Import target không thuộc inventory: ${file.path} -> ${target}`);
}

const expectedLanguages = [...new Set(result.files.map((file) => file.language))].sort((a, b) => a.localeCompare(b));
if (JSON.stringify(result.languages) !== JSON.stringify(expectedLanguages)) errors.push('languages không khớp inventory hoặc chưa sort.');

const categories = result.files.reduce((summary, file) => {
  summary[file.fileCategory] = (summary[file.fileCategory] || 0) + 1;
  return summary;
}, {});
const resolvedImports = Object.values(result.importMap).reduce((count, targets) => count + targets.length, 0);

if (errors.length > 0) {
  process.stderr.write(`${JSON.stringify({ errors }, null, 2)}\n`);
  process.exit(1);
}
process.stdout.write(`${JSON.stringify({
  valid: true,
  name: result.name,
  totalFiles: result.totalFiles,
  categories,
  languages: result.languages,
  frameworks: result.frameworks,
  estimatedComplexity: result.estimatedComplexity,
  filteredByIgnore: result.filteredByIgnore,
  importMapKeys: importKeys.length,
  resolvedImports,
  outputPath: finalPath,
}, null, 2)}\n`);
