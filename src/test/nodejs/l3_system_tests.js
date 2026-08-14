const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

async function helpers() {
  return import('../../../test-execution/scripts/l3-runner-lib.mjs');
}

describe('L3 catalog parsing', () => {
  it('parses a BOM-prefixed catalog and returns its cases', async () => {
    const { parseCatalogText } = await helpers();
    const cases = parseCatalogText('\uFEFF{"cases":[{"id":"L3-AUTH-001"}]}');
    assert.deepEqual(cases, [{ id: 'L3-AUTH-001' }]);
  });
});

describe('L3 request classification', () => {
  it('treats only POST /imports as a multipart upload', async () => {
    const { isMultipartImportCase } = await helpers();
    assert.equal(isMultipartImportCase({ httpMethod: 'GET', path: '/api/v1/imports' }), false);
    assert.equal(isMultipartImportCase({ httpMethod: 'POST', path: '/api/v1/imports' }), true);
  });

  it('fails a risk result when its error code does not match', async () => {
    const { classifyResult } = await helpers();
    const result = classifyResult(
      {
        expectedStatus: 409,
        expectedResponse: "{success:false,error:{code:'TRIP_LOCKED'}}",
        errorCode: 'TRIP_LOCKED',
        negative: true,
      },
      { status: 409, body: { success: false, error: { code: 'TRIP_NOT_FOUND' } } },
    );
    assert.equal(result.status, 'Fail');
    assert.match(result.assertion, /TRIP_LOCKED/);
  });

  it('keeps an executed HTTP mismatch as Fail instead of Not Run', async () => {
    const { classifyResult } = await helpers();
    const result = classifyResult(
      { expectedStatus: 200, expectedResponse: 'ApiResponse' },
      { status: 404, body: { success: false, error: { code: 'RESOURCE_NOT_FOUND' } } },
    );
    assert.equal(result.status, 'Fail');
    assert.match(result.assertion, /received 404/);
  });

  it('uses Not Run only for transport failures', async () => {
    const { classifyResult } = await helpers();
    const result = classifyResult(
      { expectedStatus: 200, expectedResponse: 'ApiResponse' },
      { status: 0, transportError: 'fetch failed' },
    );
    assert.deepEqual(result, { status: 'Not Run', assertion: 'fetch failed' });
  });
});

describe('L3 fixture helpers', () => {
  it('polls until an asynchronously created fixture becomes available', async () => {
    const { pollForValue } = await helpers();
    assert.equal(typeof pollForValue, 'function');
    const values = [null, null, { id: 73 }];
    const result = await pollForValue(async () => values.shift(), { attempts: 3, delayMs: 0 });
    assert.deepEqual(result, { id: 73 });
  });

  it('extracts endpoint-specific IDs instead of falling back to 1', async () => {
    const { resourceId } = await helpers();
    assert.equal(typeof resourceId, 'function');
    assert.equal(resourceId({ batchId: 41 }), 41);
    assert.equal(resourceId({ tripId: 52 }), 52);
    assert.equal(resourceId({ executionId: 63 }), 63);
    assert.equal(resourceId({ orderId: 74 }), 74);
    assert.equal(resourceId({ tripStopId: 85 }), 85);
    assert.equal(resourceId({ stopId: 86 }), 86);
    assert.equal(resourceId({ exceptionId: 96, tripId: 52 }), 96);
    assert.equal(resourceId({ outcomeId: 107, tripId: 52 }), 107);
  });

  it('builds an XLSX worksheet with nine columns and escaped literal values', async () => {
    const { buildWorksheetXml } = await helpers();
    assert.equal(typeof buildWorksheetXml, 'function');
    const xml = buildWorksheetXml([
      ['order_ref', 'store_code', 'sku', 'quantity', 'delivery_date', 'window', 'recipient', 'phone', 'notes'],
      ['L3QA-001', 'ST-001', 'SKU-001', '1', '14/08/2026', '08:00-12:00', 'QA & Test', '0900000000', '<fixture>'],
    ]);
    assert.match(xml, /<dimension ref="A1:I2"/);
    assert.match(xml, /QA &amp; Test/);
    assert.match(xml, /&lt;fixture&gt;/);
    assert.equal((xml.match(/<row r=/g) ?? []).length, 2);
  });

  it('creates a real XLSX zip containing the worksheet parts', async () => {
    const { createXlsxFixture, listXlsxEntries } = await helpers();
    assert.equal(typeof createXlsxFixture, 'function');
    const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'elog-l3-test-'));
    try {
      const fixturePath = path.join(tempDir, 'fixture.xlsx');
      createXlsxFixture(fixturePath, [
        ['order_ref', 'store_code', 'sku', 'quantity', 'delivery_date', 'window', 'recipient', 'phone', 'notes'],
      ]);
      assert.deepEqual([...fs.readFileSync(fixturePath).subarray(0, 2)], [0x50, 0x4b]);
      const entries = listXlsxEntries(fixturePath);
      assert.equal(entries.some(entry => /xl\/worksheets\/sheet1\.xml$/.test(entry)), true);
      assert.equal(entries.some(entry => /META-INF\/MANIFEST\.MF$/.test(entry)), false);
    } finally {
      assert.equal(path.resolve(tempDir).startsWith(path.resolve(os.tmpdir())), true);
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });
});
