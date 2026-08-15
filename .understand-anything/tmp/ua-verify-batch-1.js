#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');

const projectRoot = path.resolve(process.argv[2]);
const input = JSON.parse(fs.readFileSync(path.resolve(process.argv[3]), 'utf8'));
const extraction = JSON.parse(fs.readFileSync(path.resolve(process.argv[4]), 'utf8'));
const outputPath = path.resolve(process.argv[5]);
const outputStat = fs.statSync(outputPath);
if (!outputStat.isFile() || outputStat.size === 0) throw new Error('batch-1.json thiếu hoặc rỗng.');
const graph = JSON.parse(fs.readFileSync(outputPath, 'utf8'));
const errors = [];
const batchPaths = new Set(input.batchFiles.map((file) => file.path));
const allowedTypes = new Set(['file', 'function', 'class', 'config', 'document', 'service', 'table', 'endpoint', 'pipeline', 'schema', 'resource']);
const allowedComplexity = new Set(['simple', 'moderate', 'complex']);
const allowedEdges = new Map([['migrates', 0.7], ['depends_on', 0.6]]);

if (JSON.stringify(Object.keys(graph)) !== JSON.stringify(['nodes', 'edges'])) errors.push('Top-level JSON phải chỉ có nodes và edges.');
if (!Array.isArray(graph.nodes) || !Array.isArray(graph.edges)) errors.push('nodes/edges phải là array.');
if (input.batchFiles.length !== 25) errors.push(`Input không có đúng 25 files: ${input.batchFiles.length}.`);
if (extraction.scriptCompleted !== true || extraction.filesAnalyzed !== 25) errors.push('Extractor không hoàn tất đủ 25 files.');
if (!Array.isArray(extraction.filesSkipped) || extraction.filesSkipped.length !== 0) errors.push('Extractor có file bị skip.');
if (Object.values(input.batchImportData).flat().length !== 0) errors.push('Input batchImportData không có tổng imports bằng 0.');

const ids = new Set();
const nodeTypeCounts = {};
for (const node of graph.nodes || []) {
  if (!node || typeof node !== 'object') {
    errors.push('Có node không phải object.');
    continue;
  }
  if (ids.has(node.id)) errors.push(`Node ID trùng: ${node.id}`);
  ids.add(node.id);
  nodeTypeCounts[node.type] = (nodeTypeCounts[node.type] || 0) + 1;
  if (!allowedTypes.has(node.type)) errors.push(`Node type không hợp lệ: ${node.id}`);
  for (const field of ['id', 'type', 'name', 'summary', 'complexity']) {
    if (typeof node[field] !== 'string' || !node[field].trim()) errors.push(`Node thiếu ${field}: ${node.id}`);
  }
  if (!Array.isArray(node.tags) || node.tags.length < 3 || node.tags.length > 5) errors.push(`Tags phải có 3-5 phần tử: ${node.id}`);
  for (const tag of node.tags || []) {
    if (typeof tag !== 'string' || !/^[\p{Ll}\p{Lo}\p{M}\d]+(?:-[\p{Ll}\p{Lo}\p{M}\d]+)*$/u.test(tag)) {
      errors.push(`Tag không lowercase-hyphenated: ${node.id} -> ${tag}`);
    }
  }
  if (!allowedComplexity.has(node.complexity)) errors.push(`Complexity không hợp lệ: ${node.id}`);
  if (node.languageNotes !== undefined && (typeof node.languageNotes !== 'string' || !node.languageNotes.trim())) errors.push(`languageNotes không hợp lệ: ${node.id}`);
  if (node.type === 'table') {
    if (typeof node.filePath !== 'string' || !batchPaths.has(node.filePath)) errors.push(`Table node có filePath ngoài batch: ${node.id}`);
    if (node.filePath && !fs.existsSync(path.join(projectRoot, ...node.filePath.split('/')))) errors.push(`filePath không tồn tại: ${node.filePath}`);
    if (node.filePath && !node.id.startsWith(`table:${node.filePath}:`)) errors.push(`Table node sai ID convention: ${node.id}`);
  }
}

const migrationNodes = graph.nodes.filter((node) => node.type === 'table' && node.id.endsWith(':migration'));
if (migrationNodes.length !== 25) errors.push(`Không có đúng 25 migration nodes: ${migrationNodes.length}.`);
for (const file of input.batchFiles) {
  const expectedId = `table:${file.path}:migration`;
  if (!ids.has(expectedId)) errors.push(`Thiếu migration node: ${file.path}`);
}
for (const result of extraction.results) {
  for (const definition of result.definitions || []) {
    if (definition.kind === 'table' && !ids.has(`table:${result.path}:${definition.name}`)) {
      errors.push(`Thiếu table definition đáng kể: ${result.path}:${definition.name}`);
    }
  }
}

const edgeKeys = new Set();
const edgeTypeCounts = {};
for (const edge of graph.edges || []) {
  const key = `${edge.source}|${edge.target}|${edge.type}`;
  if (edgeKeys.has(key)) errors.push(`Edge trùng: ${key}`);
  edgeKeys.add(key);
  edgeTypeCounts[edge.type] = (edgeTypeCounts[edge.type] || 0) + 1;
  if (!ids.has(edge.source) || !ids.has(edge.target)) errors.push(`Edge dangling: ${key}`);
  if (edge.source === edge.target) errors.push(`Edge tự tham chiếu: ${key}`);
  if (!allowedEdges.has(edge.type)) errors.push(`Edge type không hợp lệ trong batch: ${edge.type}`);
  if (allowedEdges.has(edge.type) && edge.weight !== allowedEdges.get(edge.type)) errors.push(`Sai weight: ${key}`);
  if (edge.direction !== 'forward') errors.push(`Sai direction: ${key}`);
}
if ((edgeTypeCounts.imports || 0) !== 0) errors.push('Phát sinh imports edge dù batchImportData có tổng 0.');
for (const migrationNode of migrationNodes) {
  if (!graph.edges.some((edge) => edge.source === migrationNode.id)) errors.push(`Migration node không có semantic edge: ${migrationNode.id}`);
}

if (errors.length) {
  process.stderr.write(`${JSON.stringify({ valid: false, errors }, null, 2)}\n`);
  process.exit(1);
}
process.stdout.write(`${JSON.stringify({
  valid: true,
  outputBytes: outputStat.size,
  filesAnalyzed: extraction.filesAnalyzed,
  filesSkipped: extraction.filesSkipped,
  migrationNodes: migrationNodes.length,
  nodeTypeCounts,
  totalNodes: graph.nodes.length,
  edgeTypeCounts,
  totalEdges: graph.edges.length,
  importEdgesExpected: 0,
  importEdgesActual: edgeTypeCounts.imports || 0,
}, null, 2)}\n`);
