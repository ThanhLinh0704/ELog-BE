import fs from 'node:fs';
import path from 'node:path';

const baseUrl = process.env.ELOG_BASE_URL ?? 'http://localhost:8080';
const username = process.env.ELOG_TEST_USERNAME;
const password = process.env.ELOG_TEST_PASSWORD;
const catalogPath = process.env.ELOG_L3_CATALOG ?? 'D:/Elog/AuditWork/report5-redesign/catalog/l3.json';
const outputPath = process.env.ELOG_L3_OUTPUT ?? 'test-execution/evidence/l3-results.json';

if (!username || !password) {
  throw new Error('Set ELOG_TEST_USERNAME and ELOG_TEST_PASSWORD before running L3.');
}

const catalogJson = JSON.parse(fs.readFileSync(catalogPath, 'utf8').replace(/^\uFEFF/, ''));
const cases = catalogJson.cases ?? catalogJson;
const runKey = `qa${Date.now()}`;

async function rawRequest(method, requestPath, body, token, contentType = 'application/json') {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let requestBody;
  if (body !== undefined) {
    if (contentType === 'application/json') {
      headers['Content-Type'] = contentType;
      requestBody = JSON.stringify(body);
    } else {
      requestBody = body;
    }
  }
  const started = Date.now();
  try {
    const response = await fetch(`${baseUrl}${requestPath}`, {
      method,
      headers,
      body: ['GET', 'HEAD'].includes(method) ? undefined : requestBody,
      signal: AbortSignal.timeout(30000),
    });
    const text = await response.text();
    let parsed;
    try { parsed = text ? JSON.parse(text) : null; } catch { parsed = text; }
    return { status: response.status, body: parsed, durationMs: Date.now() - started };
  } catch (error) {
    return { status: 0, body: null, durationMs: Date.now() - started, transportError: String(error) };
  }
}

async function login(user = username, pass = password) {
  return rawRequest('POST', '/api/v1/auth/login', { username: user, password: pass });
}

const loginResult = await login();
if (loginResult.status !== 200 || !loginResult.body?.data?.accessToken) {
  throw new Error(`L3 bootstrap login failed with HTTP ${loginResult.status}`);
}
const token = loginResult.body.data.accessToken;

function dataOf(result) { return result?.body?.data ?? result?.body; }
function contentOf(result) {
  const data = dataOf(result);
  return Array.isArray(data) ? data : (data?.content ?? data?.items ?? []);
}

async function firstResource(url) {
  const result = await rawRequest('GET', url, undefined, token);
  return contentOf(result)[0] ?? null;
}

const refs = {
  product: await firstResource('/api/v1/products?page=0&size=1'),
  route: await firstResource('/api/v1/routes?page=0&size=1'),
  store: await firstResource('/api/v1/stores?page=0&size=1'),
  vehicle: await firstResource('/api/v1/vehicles?page=0&size=1'),
  user: await firstResource('/api/v1/users?page=0&size=1'),
  driver: await firstResource('/api/v1/drivers?page=0&size=1'),
  importBatch: await firstResource('/api/v1/imports?page=0&size=1'),
};
refs.deliveryDate = refs.importBatch?.deliveryDate ?? new Date().toISOString().slice(0, 10);
refs.tripDraft = await firstResource(`/api/v1/trip-drafts?deliveryDate=${refs.deliveryDate}&page=0&size=1`);
const tripsResult = await rawRequest('GET', `/api/v1/trips?tripDraftId=${refs.tripDraft?.id ?? 1}`, undefined, token);
refs.trip = contentOf(tripsResult)[0] ?? dataOf(tripsResult)?.[0] ?? null;

function resourceId(resource, fallback = 1) {
  return resource?.id ?? resource?.userId ?? resource?.routeId ?? resource?.storeId ?? resource?.vehicleId ?? fallback;
}

const provinceResult = await rawRequest('GET', '/api/v1/addresses/provinces', undefined, token);
refs.province = contentOf(provinceResult)[0] ?? dataOf(provinceResult)?.[0] ?? { code: '01' };
const provinceCode = refs.province.code ?? refs.province.provinceCode ?? '01';
const districtResult = await rawRequest('GET', `/api/v1/addresses/provinces/${provinceCode}/districts`, undefined, token);
refs.district = contentOf(districtResult)[0] ?? dataOf(districtResult)?.[0] ?? { code: '001' };
const districtCode = refs.district.code ?? refs.district.districtCode ?? '001';
const wardResult = await rawRequest('GET', `/api/v1/addresses/districts/${districtCode}/wards`, undefined, token);
refs.ward = contentOf(wardResult)[0] ?? dataOf(wardResult)?.[0] ?? { code: '00001' };

function materializePath(template) {
  const values = {
    districtCode,
    provinceCode,
    sku: refs.product?.sku ?? 'SP001',
    batchId: resourceId(refs.importBatch),
    executionId: refs.trip?.executionId ?? refs.trip?.tripExecutionId ?? 1,
    orderId: 1,
    stopId: 1,
    tripId: resourceId(refs.trip),
    id: template.includes('/products/') ? resourceId(refs.product)
      : template.includes('/routes/') ? resourceId(refs.route)
      : template.includes('/stores/') ? resourceId(refs.store)
      : template.includes('/vehicles/') ? resourceId(refs.vehicle)
      : template.includes('/users/') ? resourceId(refs.user)
      : template.includes('/drivers/') ? resourceId(refs.driver)
      : template.includes('/trip-drafts/') ? resourceId(refs.tripDraft)
      : template.includes('/trips/') ? resourceId(refs.trip)
      : 1,
  };
  let p = template.replace(/\{([^}]+)\}/g, (_, key) => encodeURIComponent(values[key] ?? 1));
  const query = [];
  if (template === '/api/v1/fleet/capacity-check') query.push(`date=${refs.deliveryDate}`);
  if (template === '/api/v1/drivers/available') query.push(`date=${refs.deliveryDate}`);
  if (template === '/api/v1/trip-drafts') query.push(`deliveryDate=${refs.deliveryDate}`);
  if (template === '/api/v1/trips') query.push(`tripDraftId=${resourceId(refs.tripDraft)}`);
  if (template === '/api/v1/trips/my-trips') query.push(`date=${refs.deliveryDate}`);
  if (template.endsWith('/calendar')) query.push(`month=${refs.deliveryDate.slice(0, 7)}`);
  if (template.endsWith('/eligible-vehicles-for-stops')) query.push('stopIds=1');
  if (template.endsWith('/amend')) query.push('amendmentReason=QA%20contract%20verification');
  if (query.length) p += `${p.includes('?') ? '&' : '?'}${query.join('&')}`;
  return p;
}

function jsonBody(testCase) {
  const p = testCase.path;
  if (p === '/api/v1/auth/login') return { username, password };
  if (p.includes('/auth/refresh') || p.includes('/auth/logout')) return { refreshToken: loginResult.body.data.refreshToken };
  if (p === '/api/v1/products') return { sku: `QA-${runKey}`, productName: 'QA API Product', weightKg: 5, lengthM: 0.5, widthM: 0.3, heightM: 0.2, shape: 'BOX', isFragile: false };
  if (p.match(/^\/api\/v1\/products\/\{id\}$/)) return { sku: refs.product?.sku ?? 'SP001', productName: 'QA API Product update', weightKg: 5, lengthM: 0.5, widthM: 0.3, heightM: 0.2, shape: 'BOX', isFragile: false };
  if (p.endsWith('/products/{id}/status')) return { isActive: true };
  if (p === '/api/v1/routes') return { code: `QA-${runKey}`.slice(0, 30), name: 'QA API Route', description: 'Automated contract verification' };
  if (p.match(/^\/api\/v1\/routes\/\{id\}$/)) return { name: 'QA Route updated', description: 'Automated contract verification' };
  if (p.endsWith('/routes/{id}/status')) return { isActive: true };
  if (p.endsWith('/routes/{id}/stops')) return { storeId: resourceId(refs.store) };
  if (p.endsWith('/stops/reorder')) return { orderedStopIds: [1] };
  if (p === '/api/v1/stores') return { storeCode: `QA-${runKey}`.slice(0, 30), storeName: 'QA API Store', provinceCode, districtCode, wardCode: refs.ward.code ?? refs.ward.wardCode, addressDetail: 'QA address', latitude: 21.03, longitude: 105.85, allowedDeliveryHours: '08:00-17:00', maxAllowedVehicleWeight: 5000 };
  if (p.match(/^\/api\/v1\/stores\/\{id\}$/)) return { storeName: 'QA Store updated', provinceCode, districtCode, wardCode: refs.ward.code ?? refs.ward.wardCode, addressDetail: 'QA address', latitude: 21.03, longitude: 105.85, allowedDeliveryHours: '08:00-17:00', maxAllowedVehicleWeight: 5000 };
  if (p.endsWith('/stores/{id}/status') || p.endsWith('/vehicles/{id}/status') || p.endsWith('/users/{id}/status')) return { isActive: true };
  if (p === '/api/v1/vehicles') return { vehicleCode: `QA-${runKey}`.slice(0, 30), plateNumber: `QA-${String(Date.now()).slice(-6)}`, vehicleType: 'TRUCK', vehicleClass: '1.25T', payloadKg: 3000, grossVehicleWeightKg: 5000, requiredLicense: 'B', maxVolumeM3: 10, cargoLengthMm: 3000, cargoWidthMm: 1700, cargoHeightMm: 1700, averageSpeedKmh: 38, costPerKm: 12000, status: 'AVAILABLE', description: 'QA API vehicle' };
  if (p.match(/^\/api\/v1\/vehicles\/\{id\}$/)) return { vehicleType: 'TRUCK', vehicleClass: '1.25T', payloadKg: 3000, grossVehicleWeightKg: 5000, requiredLicense: 'B', maxVolumeM3: 10, cargoLengthMm: 3000, cargoWidthMm: 1700, cargoHeightMm: 1700, averageSpeedKmh: 38, costPerKm: 12000, status: 'AVAILABLE', description: 'QA update' };
  if (p === '/api/v1/users') return { username: `qa_${runKey}`, password: 'QaTest@2026', fullName: 'QA API User', email: `qa_${runKey}@example.test`, roles: ['DISPATCHER'] };
  if (p.match(/^\/api\/v1\/users\/\{id\}$/)) return { fullName: 'QA User updated', email: `qa_update_${runKey}@example.test` };
  if (p.endsWith('/users/{id}/roles')) return { roles: ['DISPATCHER'] };
  if (p.endsWith('/roles/{id}/permissions')) return { permissionIds: [1] };
  if (p.endsWith('/drivers/{id}/status')) return { status: 'ACTIVE', reasonNote: 'QA verification' };
  if (p.endsWith('/exceptions/{id}/resolve')) return { resolutionNotes: 'QA verification' };
  if (p.endsWith('/adjust-departure-time')) return { plannedDepartureTime: '08:30:00' };
  if (p.endsWith('/assign')) return { vehicleId: resourceId(refs.vehicle), driverId: resourceId(refs.driver) };
  if (p.endsWith('/assign-split')) return { assignments: [{ vehicleId: resourceId(refs.vehicle), driverId: resourceId(refs.driver), stopIds: [1] }] };
  if (p.endsWith('/settle-delay')) return { action: 'KEEP', note: 'QA verification' };
  if (p.endsWith('/recalculate-eta')) return { plannedDepartureTime: '08:30:00' };
  if (p === '/api/v1/trip-drafts/consolidate') return { deliveryDate: refs.deliveryDate };
  if (p.endsWith('/trip-drafts/{id}/stops/{stopId}')) return { isActive: true, overrideNote: 'QA verification' };
  if (p.endsWith('/trip-executions/{id}/admin-override')) return { action: 'COMPLETE', reason: 'QA verification', defaultPendingOrderStatus: 'FAILED' };
  if (p.endsWith('/assignment')) return { vehicleId: resourceId(refs.vehicle), driverId: resourceId(refs.driver) };
  if (p.endsWith('/orders/{orderId}/result')) return { status: 'DELIVERED' };
  if (p.endsWith('/trip-stops/{id}/reject')) return { rejectionType: 'STORE_CLOSED', description: 'QA verification' };
  return undefined;
}

function assertContract(testCase, actual) {
  if (actual.status !== Number(testCase.expectedStatus)) {
    return { pass: false, assertion: `Expected HTTP ${testCase.expectedStatus}, received ${actual.status}` };
  }
  if (String(testCase.expectedResponse ?? '').includes('ApiResponse') && typeof actual.body?.success !== 'boolean') {
    return { pass: false, assertion: 'HTTP status matched but ApiResponse.success is missing' };
  }
  return { pass: true, assertion: `HTTP ${actual.status} and basic response envelope matched` };
}

const results = [];
// Run the permission-mutating contract last to prevent cross-case contamination.
const orderedCases = [
  ...cases.filter(testCase => testCase.id !== 'L3-IDENTITY-047'),
  ...cases.filter(testCase => testCase.id === 'L3-IDENTITY-047'),
];
for (const testCase of orderedCases) {
  // Use a fresh authenticated session per case so logout/refresh cases cannot
  // leak token state into the next independent contract check.
  const caseSession = await login();
  let actual;
  let requestPath = materializePath(testCase.path);
  let body = jsonBody(testCase);
  let caseToken = caseSession.body?.data?.accessToken ?? token;

  if (testCase.path === '/api/v1/auth/refresh' || testCase.path === '/api/v1/auth/logout') {
    body = { refreshToken: caseSession.body?.data?.refreshToken };
  }

  if (testCase.id === 'L3-AUTH-119') body = { username: '__unknown_qa_user__', password: 'Wrong@123' };
  if (testCase.id === 'L3-AUTH-120') body = { username: '__disabled_qa_user__', password: 'Wrong@123' };
  if (testCase.id === 'L3-AUTH-121' || testCase.id === 'L3-AUTH-122') body = { refreshToken: `unknown-${runKey}` };
  if (testCase.id === 'L3-MASTERDATA-123') body = { ...jsonBody({ path: '/api/v1/vehicles' }), vehicleCode: `QAD-${runKey}`.slice(0, 30), plateNumber: refs.vehicle?.plateNumber };
  if (testCase.id === 'L3-MASTERDATA-124') body = { ...jsonBody({ path: '/api/v1/products' }), sku: refs.product?.sku };
  if (testCase.id === 'L3-MASTERDATA-125') body = { ...jsonBody({ path: '/api/v1/stores' }), storeCode: refs.store?.storeCode ?? refs.store?.code };
  if (testCase.id === 'L3-MASTERDATA-126') body = { ...jsonBody({ path: '/api/v1/routes' }), code: refs.route?.code };

  if (testCase.path === '/api/v1/imports') {
    actual = { status: 0, body: null, durationMs: 0, transportError: 'Multipart workbook fixture is not available in this runner' };
  } else {
    actual = await rawRequest(testCase.httpMethod, requestPath, body, caseToken);
  }
  const check = actual.status === 0
    ? { pass: false, assertion: actual.transportError }
    : assertContract(testCase, actual);
  results.push({
    testId: testCase.id,
    level: 'L3',
    title: testCase.title,
    method: testCase.httpMethod,
    path: requestPath,
    expectedStatus: Number(testCase.expectedStatus),
    actualStatus: actual.status,
    status: actual.status === 0 ? 'Not Run' : (check.pass ? 'Pass' : 'Fail'),
    assertion: check.assertion,
    durationMs: actual.durationMs,
    responseExcerpt: JSON.stringify(actual.body)?.slice(0, 1200) ?? '',
  });
}

const summary = results.reduce((acc, result) => {
  acc[result.status] = (acc[result.status] ?? 0) + 1;
  return acc;
}, { Pass: 0, Fail: 0, 'Not Run': 0 });
const output = {
  generatedAt: new Date().toISOString(),
  baseUrl,
  catalog: catalogPath,
  total: results.length,
  summary,
  bootstrap: {
    loginStatus: loginResult.status,
    referenceIds: Object.fromEntries(Object.entries(refs).map(([key, value]) => [key, typeof value === 'object' ? resourceId(value, null) : value])),
  },
  results,
};
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, JSON.stringify(output, null, 2));
console.log(JSON.stringify({ total: results.length, summary, outputPath }, null, 2));
if (results.length !== 130) process.exitCode = 2;
