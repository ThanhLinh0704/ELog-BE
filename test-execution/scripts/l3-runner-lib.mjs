export function parseCatalogText(text) {
  const parsed = JSON.parse(text.replace(/^\uFEFF/, ''));
  return parsed.cases ?? parsed;
}

export function isMultipartImportCase(testCase) {
  return testCase.httpMethod === 'POST' && testCase.path === '/api/v1/imports';
}

export function resourceId(resource, fallback = null) {
  return resource?.id
    ?? resource?.batchId
    ?? resource?.exceptionId
    ?? resource?.outcomeId
    ?? resource?.tripId
    ?? resource?.executionId
    ?? resource?.orderId
    ?? resource?.tripStopId
    ?? resource?.stopId
    ?? resource?.tripDraftStopId
    ?? resource?.userId
    ?? resource?.routeId
    ?? resource?.storeId
    ?? resource?.vehicleId
    ?? fallback;
}

export async function pollForValue(fetchValue, { attempts = 30, delayMs = 1000 } = {}) {
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    const value = await fetchValue();
    if (value) return value;
    if (attempt < attempts - 1 && delayMs > 0) {
      await new Promise(resolve => setTimeout(resolve, delayMs));
    }
  }
  return null;
}

function xmlEscape(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;');
}

function columnName(index) {
  let result = '';
  for (let value = index + 1; value > 0; value = Math.floor((value - 1) / 26)) {
    result = String.fromCharCode(65 + ((value - 1) % 26)) + result;
  }
  return result;
}

export function buildWorksheetXml(rows) {
  const width = Math.max(1, ...rows.map(row => row.length));
  const height = Math.max(1, rows.length);
  const rowXml = rows.map((row, rowIndex) => {
    const cells = row.map((value, columnIndex) => {
      const ref = `${columnName(columnIndex)}${rowIndex + 1}`;
      return `<c r="${ref}" t="inlineStr"><is><t>${xmlEscape(value)}</t></is></c>`;
    }).join('');
    return `<row r="${rowIndex + 1}">${cells}</row>`;
  }).join('');
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><dimension ref="A1:${columnName(width - 1)}${height}"/><sheetData>${rowXml}</sheetData></worksheet>`;
}

function jarExecutable() {
  if (process.env.JAVA_HOME) {
    return path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'jar.exe' : 'jar');
  }
  const settings = spawnSync('java', ['-XshowSettings:properties', '-version'], { encoding: 'utf8' });
  const output = `${settings.stdout ?? ''}\n${settings.stderr ?? ''}`;
  const javaHome = output.match(/^\s*java\.home\s*=\s*(.+)$/m)?.[1]?.trim();
  if (!javaHome) throw new Error('Cannot resolve the JDK jar executable');
  return path.join(javaHome, 'bin', process.platform === 'win32' ? 'jar.exe' : 'jar');
}

export function listXlsxEntries(xlsxPath) {
  const result = spawnSync(jarExecutable(), ['--list', '--file', path.resolve(xlsxPath)], { encoding: 'utf8' });
  if (result.status !== 0) {
    throw new Error(`Failed to list XLSX fixture: ${result.error?.message ?? result.stderr ?? result.stdout}`);
  }
  return result.stdout.split(/\r?\n/).filter(Boolean);
}

export function createXlsxFixture(outputPath, rows) {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'elog-l3-xlsx-'));
  try {
    const parts = {
      '[Content_Types].xml': '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>',
      '_rels/.rels': '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>',
      'xl/workbook.xml': '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Orders" sheetId="1" r:id="rId1"/></sheets></workbook>',
      'xl/_rels/workbook.xml.rels': '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>',
      'xl/worksheets/sheet1.xml': buildWorksheetXml(rows),
    };
    for (const [relativePath, content] of Object.entries(parts)) {
      const fullPath = path.join(tempDir, ...relativePath.split('/'));
      fs.mkdirSync(path.dirname(fullPath), { recursive: true });
      fs.writeFileSync(fullPath, content);
    }
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });
    const result = spawnSync(jarExecutable(), ['--create', '--no-manifest', '--file', path.resolve(outputPath), '-C', tempDir, '.'], {
      encoding: 'utf8',
    });
    if (result.status !== 0) {
      throw new Error(`Failed to create XLSX fixture: ${result.error?.message ?? result.stderr ?? result.stdout}`);
    }
    return outputPath;
  } finally {
    const resolvedTemp = path.resolve(tempDir);
    if (!resolvedTemp.startsWith(path.resolve(os.tmpdir()))) {
      throw new Error(`Refusing to clean unexpected XLSX temp path: ${resolvedTemp}`);
    }
    fs.rmSync(resolvedTemp, { recursive: true, force: true });
  }
}

export function classifyResult(testCase, actual) {
  if (actual.status === 0) {
    return { status: 'Not Run', assertion: actual.transportError };
  }
  if (actual.status !== Number(testCase.expectedStatus)) {
    return {
      status: 'Fail',
      assertion: `Expected HTTP ${testCase.expectedStatus}, received ${actual.status}`,
    };
  }
  if (String(testCase.expectedResponse ?? '').includes('ApiResponse') && typeof actual.body?.success !== 'boolean') {
    return { status: 'Fail', assertion: 'HTTP status matched but ApiResponse.success is missing' };
  }
  if (testCase.errorCode && testCase.errorCode !== 'N/A') {
    const actualErrorCode = actual.body?.error?.code;
    if (actualErrorCode !== testCase.errorCode) {
      return {
        status: 'Fail',
        assertion: `Expected error code ${testCase.errorCode}, received ${actualErrorCode ?? 'none'}`,
      };
    }
  }
  return { status: 'Pass', assertion: `HTTP ${actual.status} and basic response envelope matched` };
}
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
