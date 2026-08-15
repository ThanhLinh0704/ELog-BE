#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');

const projectRoot = path.resolve(process.argv[2]);
const planBatch = JSON.parse(fs.readFileSync(path.resolve(process.argv[3]), 'utf8')).batches[1];
const input = JSON.parse(fs.readFileSync(path.resolve(process.argv[4]), 'utf8'));
const extraction = JSON.parse(fs.readFileSync(path.resolve(process.argv[5]), 'utf8'));
const graphPath = path.resolve(process.argv[6]);
const batchOne = JSON.parse(fs.readFileSync(path.join(projectRoot, '.understand-anything', 'intermediate', 'batch-1.json'), 'utf8'));
const stat = fs.statSync(graphPath);
const graph = JSON.parse(fs.readFileSync(graphPath, 'utf8'));
const errors = [];
const batchPaths = new Set(input.batchFiles.map((file) => file.path));
const batchOneIds = new Set(batchOne.nodes.map((node) => node.id));
const validComplexity = new Set(['simple', 'moderate', 'complex']);
const validTypes = new Set(['table', 'document']);
const edgeWeights = new Map([['migrates', 0.7], ['depends_on', 0.6], ['documents', 0.5]]);
const entryId = 'file:src/main/java/com/elog/ElogApplication.java';

if (!stat.isFile() || stat.size === 0) errors.push('batch-2.json thiếu hoặc rỗng.');
if (JSON.stringify(Object.keys(graph)) !== JSON.stringify(['nodes', 'edges'])) errors.push('Top-level phải chỉ có nodes và edges.');
if (JSON.stringify(input.batchFiles) !== JSON.stringify(planBatch.files)) errors.push('batchFiles không còn nguyên vẹn so với batch plan.');
if (JSON.stringify(input.batchImportData) !== JSON.stringify(planBatch.importData)) errors.push('batchImportData không còn nguyên vẹn so với batch plan.');
if (input.batchFiles.length !== 25 || Object.values(input.batchImportData).flat().length !== 0) errors.push('Input không đúng 25 files/0 imports.');
if (extraction.scriptCompleted !== true || extraction.filesAnalyzed !== 25) errors.push('Extractor không hoàn tất đủ 25 files.');
if (!Array.isArray(extraction.filesSkipped) || extraction.filesSkipped.length !== 0) errors.push('Extractor có file bị skip.');

const nodeIds = new Set();
const nodeTypeCounts = {};
for (const node of graph.nodes || []) {
  if (nodeIds.has(node.id)) errors.push(`Node ID trùng: ${node.id}`);
  nodeIds.add(node.id);
  nodeTypeCounts[node.type] = (nodeTypeCounts[node.type] || 0) + 1;
  if (!validTypes.has(node.type)) errors.push(`Node type ngoài scope batch: ${node.id}`);
  for (const field of ['id', 'type', 'name', 'summary', 'filePath', 'complexity']) {
    if (typeof node[field] !== 'string' || !node[field].trim()) errors.push(`Node thiếu ${field}: ${node.id}`);
  }
  if (!batchPaths.has(node.filePath)) errors.push(`Node có filePath ngoài batch: ${node.id}`);
  if (!fs.existsSync(path.join(projectRoot, ...node.filePath.split('/')))) errors.push(`filePath không tồn tại: ${node.filePath}`);
  if (!Array.isArray(node.tags) || node.tags.length < 3 || node.tags.length > 5) errors.push(`Node không có 3-5 tags: ${node.id}`);
  for (const tag of node.tags || []) {
    if (typeof tag !== 'string' || !/^[\p{Ll}\p{Lo}\p{M}\d]+(?:-[\p{Ll}\p{Lo}\p{M}\d]+)*$/u.test(tag)) errors.push(`Tag sai định dạng: ${node.id} -> ${tag}`);
  }
  if (!validComplexity.has(node.complexity)) errors.push(`Complexity không hợp lệ: ${node.id}`);
  if (!/[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i.test(node.summary)) errors.push(`Summary chưa thể hiện tiếng Việt tự nhiên: ${node.id}`);
  if (node.type === 'table' && !node.id.startsWith(`table:${node.filePath}:`)) errors.push(`Sai table ID convention: ${node.id}`);
  if (node.type === 'document' && node.id !== `document:${node.filePath}`) errors.push(`Sai document ID convention: ${node.id}`);
}

const expectedParentIds = new Set(input.batchFiles.map((file) => {
  if (file.fileCategory === 'docs') return `document:${file.path}`;
  return file.path.includes('/db/migration/') ? `table:${file.path}:migration` : `table:${file.path}:database-setup`;
}));
for (const id of expectedParentIds) if (!nodeIds.has(id)) errors.push(`Thiếu file-level node: ${id}`);
if (expectedParentIds.size !== 25) errors.push('Không xác định được đúng 25 parent IDs.');
const actualParentCount = graph.nodes.filter((node) => expectedParentIds.has(node.id)).length;
if (actualParentCount !== 25) errors.push(`Không có đúng 25 file-level nodes: ${actualParentCount}.`);

for (const result of extraction.results) {
  for (const definition of result.definitions || []) {
    if (definition.kind === 'table' && !nodeIds.has(`table:${result.path}:${definition.name}`)) errors.push(`Thiếu table subnode: ${result.path}:${definition.name}`);
  }
}

const edgeKeys = new Set();
const edgeTypeCounts = {};
for (const edge of graph.edges || []) {
  const key = `${edge.source}|${edge.target}|${edge.type}`;
  if (edgeKeys.has(key)) errors.push(`Edge trùng: ${key}`);
  edgeKeys.add(key);
  edgeTypeCounts[edge.type] = (edgeTypeCounts[edge.type] || 0) + 1;
  if (!nodeIds.has(edge.source)) errors.push(`Edge source ngoài output: ${key}`);
  const knownTarget = nodeIds.has(edge.target) || batchOneIds.has(edge.target) || (edge.target === entryId && fs.existsSync(path.join(projectRoot, 'src/main/java/com/elog/ElogApplication.java')));
  if (!knownTarget) errors.push(`Edge target không tồn tại/không được biết: ${key}`);
  if (edge.source === edge.target) errors.push(`Edge tự tham chiếu: ${key}`);
  if (!edgeWeights.has(edge.type)) errors.push(`Edge type không hợp lệ: ${key}`);
  if (edgeWeights.has(edge.type) && edge.weight !== edgeWeights.get(edge.type)) errors.push(`Sai edge weight: ${key}`);
  if (edge.direction !== 'forward') errors.push(`Sai edge direction: ${key}`);
}
if ((edgeTypeCounts.imports || 0) !== 0) errors.push('Có imports edge dù expected imports = 0.');
if (!edgeKeys.has(`document:CLAUDE.md|${entryId}|documents`)) errors.push('Thiếu documents edge từ CLAUDE.md đến Spring Boot entry point.');

if (errors.length) {
  process.stderr.write(`${JSON.stringify({ valid: false, errors }, null, 2)}\n`);
  process.exit(1);
}
process.stdout.write(`${JSON.stringify({
  valid: true,
  outputBytes: stat.size,
  filesAnalyzed: extraction.filesAnalyzed,
  filesSkipped: extraction.filesSkipped,
  fileLevelNodes: actualParentCount,
  nodeTypeCounts,
  totalNodes: graph.nodes.length,
  edgeTypeCounts,
  totalEdges: graph.edges.length,
  importEdgesExpected: 0,
  importEdgesActual: edgeTypeCounts.imports || 0,
}, null, 2)}\n`);
