import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import {
  classifyResult,
  createXlsxFixture,
  isMultipartImportCase,
  parseCatalogText,
  pollForValue,
  resourceId,
} from './l3-runner-lib.mjs';

const repoRoot = process.cwd();
const baseUrl = process.env.ELOG_BASE_URL ?? 'http://localhost:8080';
const username = process.env.ELOG_TEST_USERNAME ?? 'admin';
const password = process.env.ELOG_TEST_PASSWORD ?? 'Admin@2025';
const driverUsername = process.env.ELOG_DRIVER_USERNAME ?? 'driver01';
const driverPassword = process.env.ELOG_DRIVER_PASSWORD ?? 'Dev@2025';
const catalogPath = process.env.ELOG_L3_CATALOG ?? path.join(repoRoot, 'test-execution/catalog/l3.json');
const evidencePath = process.env.ELOG_L3_OUTPUT ?? path.join(repoRoot, 'test-execution/evidence/l3-rerun-results.json');
const ledgerPath = process.env.ELOG_L3_LEDGER ?? path.join(repoRoot, 'test-execution/results/l3.json');
const mysqlExe = process.env.ELOG_MYSQL_EXE ?? 'C:\\Program Files\\MySQL\\MySQL Server 9.7\\bin\\mysql.exe';
const mysqlHost = process.env.ELOG_MYSQL_HOST ?? '127.0.0.1';
const mysqlPort = process.env.ELOG_MYSQL_PORT ?? '3307';
const mysqlUser = process.env.ELOG_MYSQL_USER ?? 'root';
const mysqlPassword = process.env.ELOG_MYSQL_PASSWORD ?? 'root';
const mysqlDatabase = process.env.ELOG_MYSQL_DATABASE;

if (!username || !password) throw new Error('Set ELOG_TEST_USERNAME and ELOG_TEST_PASSWORD before running L3.');

const cases = parseCatalogText(fs.readFileSync(catalogPath, 'utf8'));
if (cases.length !== 130) throw new Error(`Expected 130 L3 catalog cases, found ${cases.length}`);

const runKey = `L3QA-${Date.now()}`;
const fixtureDir = fs.mkdtempSync(path.join(os.tmpdir(), 'elog-l3-run-'));
const validFixturePath = path.join(fixtureDir, `${runKey}-valid.xlsx`);
const secondFixturePath = path.join(fixtureDir, `${runKey}-split.xlsx`);
const thirdFixturePath = path.join(fixtureDir, `${runKey}-revert.xlsx`);
const driverFixturePath = path.join(fixtureDir, `${runKey}-driver.xlsx`);
const rejectFixturePath = path.join(fixtureDir, `${runKey}-reject.xlsx`);
const primaryUploadFixturePath = path.join(fixtureDir, `${runKey}-primary.xlsx`);
const oversizedFixturePath = path.join(fixtureDir, `${runKey}-oversized.xlsx`);
const safeKey = runKey.replaceAll('-', '_').toLowerCase();

function redactBody(requestPath, body) {
  if (requestPath.includes('/auth/')) return '[REDACTED authentication payload]';
  if (body instanceof FormData) return '[multipart/form-data XLSX fixture]';
  return body;
}

async function rawRequest(method, requestPath, body, token) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let requestBody;
  if (body instanceof FormData) {
    requestBody = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    requestBody = JSON.stringify(body);
  }
  const started = Date.now();
  try {
    const response = await fetch(`${baseUrl}${requestPath}`, {
      method,
      headers,
      body: ['GET', 'HEAD'].includes(method) ? undefined : requestBody,
      signal: AbortSignal.timeout(30000),
    });
    const responseText = await response.text();
    let parsed;
    try { parsed = responseText ? JSON.parse(responseText) : null; } catch { parsed = responseText; }
    return { status: response.status, body: parsed, durationMs: Date.now() - started };
  } catch (error) {
    return { status: 0, body: null, durationMs: Date.now() - started, transportError: String(error) };
  }
}

async function login(user = username, pass = password) {
  return rawRequest('POST', '/api/v1/auth/login', { username: user, password: pass });
}

function dataOf(result) { return result?.body?.data ?? result?.body; }
function contentOf(result) {
  const data = dataOf(result);
  return Array.isArray(data) ? data : (data?.content ?? data?.items ?? data?.exceptions ?? []);
}
async function get(url, token) { return rawRequest('GET', url, undefined, token); }
async function firstResource(url, token) { return contentOf(await get(url, token))[0] ?? null; }

function localDatePlus(days) {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}
async function chooseUnlockedFixtureDate(startOffsetDays = 1, excludedDates = new Set()) {
  for (let offset = startOffsetDays; offset < startOffsetDays + 90; offset += 1) {
    const candidate = localDatePlus(offset);
    if (excludedDates.has(candidate)) continue;
    const drafts = contentOf(await get(`/api/v1/trip-drafts?deliveryDate=${candidate}&page=0&size=100`, token));
    const hasLockedDraft = drafts.some(draft => draft.status && draft.status !== 'DRAFT');
    if (!hasLockedDraft) return candidate;
  }
  throw new Error('Could not find an unlocked delivery date for L3 fixtures within the next 90 days.');
}
function displayDate(isoDate) {
  const [year, month, day] = isoDate.split('-');
  return `${day}/${month}/${year}`;
}
function workbookRows(orderPrefix, stores, sku, deliveryDate) {
  return [
    ['order_ref', 'store_code', 'sku', 'quantity', 'delivery_date', 'window', 'recipient', 'phone', 'notes'],
    ...stores.map((entry, index) => {
      const store = Array.isArray(entry) ? entry[0] : entry;
      const quantity = Array.isArray(entry) ? String(entry[1]) : '1';
      return [
      `${orderPrefix}-${index + 1}`,
      store.storeCode ?? store.code,
      sku,
      quantity,
      displayDate(deliveryDate),
      '08:00-18:00',
      'L3 QA Fixture',
      '0900000000',
      runKey,
      ];
    }),
  ];
}
function multipartFixture(filePath, deliveryDate) {
  const form = new FormData();
  form.append('file', new Blob([fs.readFileSync(filePath)], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  }), path.basename(filePath));
  form.append('deliveryDate', deliveryDate);
  form.append('confirmReplace', 'false');
  return form;
}

const loginResult = await login();
if (loginResult.status !== 200 || !loginResult.body?.data?.accessToken) {
  throw new Error(`L3 bootstrap login failed with HTTP ${loginResult.status}`);
}
const token = loginResult.body.data.accessToken;
const bootstrapRequests = [];
async function bootstrapRequest(method, requestPath, body, requestToken = token) {
  const result = await rawRequest(method, requestPath, body, requestToken);
  bootstrapRequests.push({ method, path: requestPath, status: result.status, response: result.body });
  return result;
}
function sqlString(value) {
  return String(value).replaceAll('\\', '\\\\').replaceAll("'", "''");
}
function execMysql(sql) {
  if (!mysqlDatabase || !fs.existsSync(mysqlExe)) return false;
  execFileSync(mysqlExe, [
    '-h', mysqlHost,
    '-P', mysqlPort,
    '-u', mysqlUser,
    `-p${mysqlPassword}`,
    '-D', mysqlDatabase,
    '-e', sql,
  ], { windowsHide: true, stdio: 'pipe' });
  return true;
}
function seedExpiredRefreshToken(refreshToken, userId) {
  if (!mysqlDatabase || !fs.existsSync(mysqlExe) || !userId) return null;
  const sql = `
    INSERT INTO refresh_tokens (token, user_id, expiry_date, created_at)
    VALUES ('${sqlString(refreshToken)}', ${Number(userId)}, TIMESTAMPADD(SECOND, -60, UTC_TIMESTAMP()), UTC_TIMESTAMP())
    ON DUPLICATE KEY UPDATE expiry_date = TIMESTAMPADD(SECOND, -60, UTC_TIMESTAMP());
  `;
  execMysql(sql);
  return refreshToken;
}

const allStores = contentOf(await get('/api/v1/stores?page=0&size=250', token));
const allVehicles = contentOf(await get('/api/v1/vehicles?page=0&size=100', token));
const primaryFixtureDate = process.env.ELOG_L3_DELIVERY_DATE ?? localDatePlus(0);
const lockedRouteIdsForPrimaryDate = new Set(
  contentOf(await get(`/api/v1/trip-drafts?deliveryDate=${primaryFixtureDate}&page=0&size=100`, token))
    .filter(draft => draft.status && draft.status !== 'DRAFT')
    .map(draft => draft.routeId)
    .filter(routeId => routeId != null),
);
let storeGroups = [...allStores.reduce((groups, store) => {
  const routeId = store.assignedRoute?.id;
  if (lockedRouteIdsForPrimaryDate.has(routeId)) return groups;
  if (routeId) groups.set(routeId, [...(groups.get(routeId) ?? []), store]);
  return groups;
}, new Map()).entries()].filter(([, stores]) => stores.length >= 1);
const multiStopGroups = storeGroups.filter(([, stores]) => stores.length >= 2);
const singleStopGroups = storeGroups.filter(([, stores]) => stores.length < 2);
storeGroups = [...multiStopGroups, ...singleStopGroups];
const assignmentVehicles = [];
const seenDrivers = new Set();
for (const vehicle of allVehicles) {
  if (vehicle.status === 'AVAILABLE' && vehicle.assignedDriverId && !seenDrivers.has(vehicle.assignedDriverId)) {
    assignmentVehicles.push(vehicle);
    seenDrivers.add(vehicle.assignedDriverId);
  }
}
const refs = {
  product: await firstResource('/api/v1/products?page=0&size=1', token),
  route: await firstResource('/api/v1/routes?page=0&size=1', token),
  store: allStores[0],
  vehicle: assignmentVehicles[0] ?? allVehicles[0],
  user: await firstResource('/api/v1/users?page=0&size=1', token),
  driver: { id: (assignmentVehicles[0] ?? allVehicles[0])?.assignedDriverId },
};
if (!refs.product?.sku || !(refs.store?.storeCode ?? refs.store?.code)) {
  throw new Error('L3 bootstrap requires at least one seeded product and store.');
}

if (storeGroups.length < 6 || multiStopGroups.length < 2 || assignmentVehicles.length < 6) {
  throw new Error('L3 bootstrap requires six routed store groups, two multi-stop routed groups, and six vehicles with distinct fixed drivers.');
}
const fixtureProductResult = await bootstrapRequest('POST', '/api/v1/products', {
  sku: `${runKey}-FX`,
  productName: 'L3 QA Fixture Product',
  weightKg: 5,
  lengthM: 0.5,
  widthM: 0.3,
  heightM: 0.2,
  shape: 'BOX',
  isFragile: false,
  description: runKey,
});
refs.fixtureProduct = fixtureProductResult.status === 201 ? dataOf(fixtureProductResult) : refs.product;
refs.expiredRefreshToken = seedExpiredRefreshToken(`${runKey}-expired-refresh-token`, resourceId(refs.user));
const splitAssignmentVehicles = [assignmentVehicles[4], assignmentVehicles[5]];
const secondaryFixtureDate = process.env.ELOG_L3_SECONDARY_DELIVERY_DATE
  ?? await chooseUnlockedFixtureDate(2, new Set([primaryFixtureDate]));
const fixtureDates = [
  primaryFixtureDate,
  primaryFixtureDate,
  primaryFixtureDate,
  primaryFixtureDate,
  primaryFixtureDate,
  secondaryFixtureDate,
];
const fixtureSpecs = [
  [validFixturePath, [
    ...storeGroups[0][1].slice(0, 2).map(store => [store, '1']),
    // Split fixture: two stops on one route, each stop fits a 10m³ truck
    // individually (200 * 0.03m³ = 6m³), but the route total exceeds one
    // truck (12m³), forcing a real TWO_VEHICLE recommendation.
    ...storeGroups[1][1].slice(0, 2).map(store => [store, '200']),
    ...storeGroups.slice(2, 5).flatMap(([, stores]) => stores.slice(0, 1).map(store => [store, '1'])),
  ], `${runKey}-A`],
  [primaryUploadFixturePath, storeGroups[5][1].slice(0, 1).map(store => [store, '1']), `${runKey}-D`],
];
fixtureSpecs.forEach(([fixturePath, stores, orderPrefix], index) => {
  createXlsxFixture(fixturePath, workbookRows(orderPrefix, stores, refs.fixtureProduct.sku, fixtureDates[index]));
});
const storeCode = refs.store.storeCode ?? refs.store.code;
const oversizedRows = [['order_ref', 'store_code', 'sku', 'quantity', 'delivery_date', 'window', 'recipient', 'phone', 'notes']];
for (let index = 0; index < 5001; index += 1) {
  oversizedRows.push([`${runKey}-OVER-${index}`, storeCode, refs.fixtureProduct.sku, '1', displayDate(fixtureDates[3]), '', '', '', '']);
}
createXlsxFixture(oversizedFixturePath, oversizedRows);

const imported = await bootstrapRequest('POST', '/api/v1/imports', multipartFixture(validFixturePath, fixtureDates[0]));
if (imported.status !== 201) throw new Error(`Fixture import failed with HTTP ${imported.status}`);

// The production auto-pipeline is async and can observe the import before its
// transaction commits.  Wait until the orders are visible, then invoke the
// public consolidation API so fixture creation is deterministic and still
// exercises the real HTTP/service path.
const importedBatchId = resourceId(dataOf(imported));
const importedOrdersVisible = await pollForValue(async () => {
  const orders = contentOf(await get(`/api/v1/imports/${importedBatchId}/orders?page=0&size=100`, token));
  return orders.length > 0 ? orders : null;
});
if (!importedOrdersVisible) throw new Error('Fixture import orders did not become visible after commit.');
const autoConsolidated = await pollForValue(async () => {
  const drafts = contentOf(await get(`/api/v1/trip-drafts?deliveryDate=${fixtureDates[0]}&page=0&size=100`, token));
  return drafts.length > 0 ? drafts : null;
}, { attempts: 10, delayMs: 500 });
if (!autoConsolidated) {
  const consolidated = await bootstrapRequest('POST', '/api/v1/trip-drafts/consolidate', {
    deliveryDate: fixtureDates[0],
  });
  if (consolidated.status !== 200) {
    throw new Error(`Fixture consolidation failed with HTTP ${consolidated.status}`);
  }
}

refs.deliveryDate = fixtureDates[0];
async function draftForRoute(routeId) {
  const drafts = contentOf(await get(`/api/v1/trip-drafts?deliveryDate=${fixtureDates[0]}&page=0&size=100`, token));
  return drafts.find(draft => draft.routeId === routeId) ?? null;
}
refs.tripDraft = await pollForValue(() => draftForRoute(storeGroups[0][0]));
refs.splitDraft = await pollForValue(() => draftForRoute(storeGroups[1][0]));
refs.revertDraft = await pollForValue(() => draftForRoute(storeGroups[2][0]));
refs.driverDraft = await pollForValue(() => draftForRoute(storeGroups[3][0]));
refs.rejectDraft = await pollForValue(() => draftForRoute(storeGroups[4][0]));
refs.importBatch = await firstResource('/api/v1/imports?page=0&size=1', token);
if (!refs.tripDraft || !refs.splitDraft || !refs.revertDraft || !refs.driverDraft || !refs.rejectDraft) {
  throw new Error('Fixture imports did not produce the five required trip drafts.');
}

async function hydrateDraftRefs(draft = refs.tripDraft) {
  const detail = dataOf(await get(`/api/v1/trip-drafts/${resourceId(draft)}`, token));
  const activeStop = detail?.stops?.find(stop => stop.isActive) ?? detail?.stops?.[0];
  refs.tripDraftStop = activeStop;
  if (activeStop) {
    refs.orderItem = await firstResource(
      `/api/v1/trip-drafts/${resourceId(draft)}/stops/${activeStop.tripDraftStopId ?? activeStop.id}/order-items`, token,
    );
  }
}
await hydrateDraftRefs();
const splitDetail = dataOf(await get(`/api/v1/trip-drafts/${resourceId(refs.splitDraft)}`, token));
refs.splitStops = splitDetail?.stops?.filter(stop => stop.isActive) ?? [];
const revertDetail = dataOf(await get(`/api/v1/trip-drafts/${resourceId(refs.revertDraft)}`, token));
const revertActiveStop = revertDetail?.stops?.find(stop => stop.isActive) ?? revertDetail?.stops?.[0];
if (revertActiveStop) {
  refs.revertOrderItem = await firstResource(
    `/api/v1/trip-drafts/${resourceId(refs.revertDraft)}/stops/${revertActiveStop.tripDraftStopId ?? revertActiveStop.id}/order-items`, token,
  );
}
refs.routeDetail = dataOf(await get(`/api/v1/routes/${resourceId(refs.route)}`, token));
refs.routeStopIds = refs.routeDetail?.stops?.map(stop => stop.id) ?? [];

// A real disabled, namespaced account is required to reach the documented 403 branch.
const disabledUsername = `${safeKey}_disabled`;
const disabledPassword = 'L3Qa@2026';
const disabledCreate = await bootstrapRequest('POST', '/api/v1/users', {
  username: disabledUsername,
  password: disabledPassword,
  fullName: 'L3 QA Disabled Fixture',
  email: `${disabledUsername}@example.test`,
  roles: ['DISPATCHER'],
});
const disabledUserId = resourceId(dataOf(disabledCreate));
if (disabledCreate.status === 201 && disabledUserId) {
  await bootstrapRequest('PATCH', `/api/v1/users/${disabledUserId}/status`, { isActive: false });
}

// Precondition the split/revert drafts without changing production code.
for (const draft of [refs.splitDraft, refs.revertDraft]) {
  await bootstrapRequest('POST', `/api/v1/trip-drafts/${resourceId(draft)}/recalculate-eta`, { plannedDepartureTime: '12:00:00' });
  await bootstrapRequest('POST', `/api/v1/trip-drafts/${resourceId(draft)}/confirm`);
}
await bootstrapRequest('POST', `/api/v1/trip-drafts/${resourceId(refs.splitDraft)}/validate-capacity`);
for (const [draft, vehicle] of [[refs.driverDraft, assignmentVehicles[2]], [refs.rejectDraft, assignmentVehicles[3]]]) {
  await bootstrapRequest('POST', `/api/v1/trip-drafts/${resourceId(draft)}/recalculate-eta`, { plannedDepartureTime: '12:00:00' });
  await bootstrapRequest('POST', `/api/v1/trip-drafts/${resourceId(draft)}/confirm`);
  await bootstrapRequest('POST', `/api/v1/trip-drafts/${resourceId(draft)}/validate-capacity`);
  const assigned = await bootstrapRequest('POST', `/api/v1/trip-drafts/${resourceId(draft)}/assign`, {
    vehicleId: resourceId(vehicle), driverId: vehicle.assignedDriverId,
  });
  const trip = dataOf(assigned);
  if (draft === refs.driverDraft) refs.driverTrip = trip;
  else refs.rejectTrip = trip;
}
for (const trip of [refs.driverTrip, refs.rejectTrip]) {
  if (trip) {
    const dispatched = await bootstrapRequest('POST', `/api/v1/trips/${resourceId(trip)}/dispatch`);
    if (trip === refs.driverTrip) refs.driverTrip = dataOf(dispatched) ?? refs.driverTrip;
    else refs.rejectTrip = dataOf(dispatched) ?? refs.rejectTrip;
  }
}
if (refs.driverTrip) {
  refs.driverExecution = refs.driverTrip.executionId ? { executionId: refs.driverTrip.executionId } : refs.driverExecution;
  refs.driverTripStop = refs.driverTrip.tripStops?.[0] ?? refs.driverTripStop;
  refs.driverOrder = await firstResource(`/api/v1/trip-drafts/${resourceId(refs.driverDraft)}/stops/${dataOf(await get(`/api/v1/trip-drafts/${resourceId(refs.driverDraft)}`, token))?.stops?.find(stop => stop.isActive)?.tripDraftStopId}/order-items`, token);
}
if (refs.rejectTrip) {
  refs.rejectTripStop = refs.rejectTrip.tripStops?.[0] ?? refs.rejectTripStop;
}

// Execute driver-owned contracts with the exact driver attached to the isolated fixture trips.
const provinceResult = await get('/api/v1/addresses/provinces', token);
refs.province = contentOf(provinceResult)[0] ?? dataOf(provinceResult)?.[0] ?? { code: '01' };
const provinceCode = refs.province.code ?? refs.province.provinceCode ?? '01';
const districtResult = await get(`/api/v1/addresses/provinces/${provinceCode}/districts`, token);
refs.district = contentOf(districtResult)[0] ?? dataOf(districtResult)?.[0] ?? { code: '001' };
const districtCode = refs.district.code ?? refs.district.districtCode ?? '001';
const wardResult = await get(`/api/v1/addresses/districts/${districtCode}/wards`, token);
refs.ward = contentOf(wardResult)[0] ?? dataOf(wardResult)?.[0] ?? { code: '00001' };
refs.adminRole = contentOf(await get('/api/v1/roles', token))
  .find(role => role.name === 'SYSTEM_ADMIN') ?? { id: 1 };
refs.permissionIds = contentOf(await get('/api/v1/permissions', token))
  .map(permission => resourceId(permission))
  .filter(id => id != null);

function caseDraft(testCase) {
  if (testCase.id === 'L3-PLANNING-066') return refs.splitDraft;
  if (testCase.id === 'L3-PLANNING-081') return refs.revertDraft;
  return refs.tripDraft;
}

function driverIdOfTrip(trip, fallback) {
  return trip?.driver?.userId
    ?? trip?.driver?.id
    ?? trip?.driverId
    ?? trip?.assignedDriverId
    ?? fallback;
}

function materializePath(testCase) {
  const template = testCase.path;
  const draft = caseDraft(testCase);
  const values = {
    districtCode,
    provinceCode,
    sku: refs.createdProduct?.sku ?? refs.product?.sku,
    batchId: resourceId(refs.importBatch),
    executionId: resourceId(refs.driverExecution ?? refs.execution ?? refs.trip),
    orderId: template.includes('/trip-drafts/')
      ? resourceId(refs.orderItem)
      : resourceId(refs.driverOrder ?? refs.orderItem),
    stopId: template.includes('/routes/')
      ? resourceId(refs.routeStop)
      : testCase.id === 'L3-EXECUTION-105'
      ? resourceId(refs.rejectTripStop)
      : (testCase.path.includes('/driver/trips/') ? resourceId(refs.driverTripStop) : (refs.tripDraftStop?.tripDraftStopId ?? resourceId(refs.tripStop ?? refs.routeStop))),
    tripId: resourceId(
      template.includes('/driver/trips/') || testCase.id === 'L3-EXECUTION-099'
        ? refs.driverTrip
        : refs.trip,
    ),
    id: template.includes('/products/') ? resourceId(refs.product)
      : template.includes('/routes/') ? resourceId(
        (template.includes('/stops') || template.endsWith('/routes/{id}')) ? (refs.createdRoute ?? refs.route) : refs.route,
      )
      : template.includes('/stores/') ? resourceId(refs.store)
      : template.includes('/vehicles/') ? resourceId(refs.vehicle)
      : template.includes('/users/') ? resourceId(refs.createdUser ?? refs.user)
      : template.includes('/roles/') ? resourceId(refs.adminRole, 1)
      : template.includes('/drivers/') ? resourceId(refs.driver)
      : template.includes('/exceptions/') ? resourceId(refs.exception)
      : template.includes('/trip-outcomes/') ? resourceId(refs.outcome)
      : template.includes('/trip-executions/') ? resourceId(refs.execution ?? refs.trip)
      : template.includes('/trip-stops/') ? resourceId(testCase.id === 'L3-EXECUTION-105' ? refs.rejectTripStop : refs.tripStop)
      : template.includes('/trip-drafts/') ? resourceId(draft)
      : template.includes('/trips/') ? resourceId(testCase.id === 'L3-EXECUTION-130' ? refs.splitTrip : refs.trip)
      : 1,
  };
  let requestPath = template.replace(/\{([^}]+)\}/g, (_, key) => encodeURIComponent(values[key] ?? 1));
  const query = [];
  if (template === '/api/v1/fleet/capacity-check') query.push(`date=${refs.deliveryDate}`);
  if (template === '/api/v1/drivers/available') query.push(`date=${refs.deliveryDate}`);
  if (template === '/api/v1/trip-drafts') query.push(`deliveryDate=${refs.deliveryDate}`);
  if (template === '/api/v1/trips') query.push(`tripDraftId=${resourceId(refs.tripDraft)}`);
  if (template === '/api/v1/trips/my-trips') query.push(`date=${refs.deliveryDate}`);
  if (template.endsWith('/calendar')) query.push(`month=${refs.deliveryDate.slice(0, 7)}`);
  if (template.endsWith('/eligible-vehicles-for-stops')) query.push(`stopIds=${refs.tripDraftStop?.tripDraftStopId ?? 1}`);
  if (template.endsWith('/amend')) query.push('amendmentReason=L3%20QA%20contract%20verification');
  if (query.length) requestPath += `${requestPath.includes('?') ? '&' : '?'}${query.join('&')}`;
  return requestPath;
}

function vehicleBody() {
  return { vehicleCode: `${runKey}-V`.slice(0, 50), plateNumber: `L3QA-${String(Date.now()).slice(-6)}`, vehicleType: 'TRUCK', vehicleClass: '1.25T', payloadKg: 3000, grossVehicleWeightKg: 5000, requiredLicense: 'B', maxVolumeM3: 10, cargoLengthMm: 3000, cargoWidthMm: 1700, cargoHeightMm: 1700, averageSpeedKmh: 38, costPerKm: 12000, status: 'AVAILABLE', description: runKey };
}
function jsonBody(testCase) {
  const p = testCase.path;
  if (p === '/api/v1/auth/login') return { username, password };
  if (p.includes('/auth/refresh') || p.includes('/auth/logout')) return { refreshToken: loginResult.body.data.refreshToken };
  if (p === '/api/v1/products') return { sku: `${runKey}-P`, productName: 'L3 QA API Product', weightKg: 5, lengthM: 0.5, widthM: 0.3, heightM: 0.2, shape: 'BOX', isFragile: false, description: runKey };
  if (p.match(/^\/api\/v1\/products\/\{id\}$/)) return { sku: refs.product?.sku, productName: 'L3 QA Product update', weightKg: 5, lengthM: 0.5, widthM: 0.3, heightM: 0.2, shape: 'BOX', isFragile: false, description: runKey };
  if (p.endsWith('/products/{id}/status')) return { isActive: true };
  if (p === '/api/v1/routes') return { code: `${runKey}-R`.slice(0, 30), name: 'L3 QA API Route', description: runKey };
  if (p.match(/^\/api\/v1\/routes\/\{id\}$/)) return { name: 'L3 QA Route updated', description: runKey };
  if (p.endsWith('/routes/{id}/status')) return { isActive: true };
  if (p.endsWith('/routes/{id}/stops')) return { storeId: resourceId(refs.createdStore ?? refs.store) };
  if (p.endsWith('/stops/reorder')) return { orderedStopIds: refs.routeStopIds };
  if (p === '/api/v1/stores') return { storeCode: `${runKey}-S`.slice(0, 30), storeName: 'L3 QA API Store', provinceCode, districtCode, wardCode: refs.ward.code ?? refs.ward.wardCode, addressDetail: runKey, latitude: 21.03, longitude: 105.85, allowedDeliveryHours: '08:00-17:00', maxAllowedVehicleWeight: 5000 };
  if (p.match(/^\/api\/v1\/stores\/\{id\}$/)) return { storeName: 'L3 QA Store updated', provinceCode, districtCode, wardCode: refs.ward.code ?? refs.ward.wardCode, addressDetail: runKey, latitude: 21.03, longitude: 105.85, allowedDeliveryHours: '08:00-17:00', maxAllowedVehicleWeight: 5000 };
  if (p.endsWith('/stores/{id}/status') || p.endsWith('/vehicles/{id}/status') || p.endsWith('/users/{id}/status')) return { isActive: true };
  if (p === '/api/v1/vehicles') return vehicleBody();
  if (p.match(/^\/api\/v1\/vehicles\/\{id\}$/)) return { vehicleType: 'TRUCK', vehicleClass: '1.25T', payloadKg: 3000, grossVehicleWeightKg: 5000, requiredLicense: 'B', maxVolumeM3: 10, cargoLengthMm: 3000, cargoWidthMm: 1700, cargoHeightMm: 1700, averageSpeedKmh: 38, costPerKm: 12000, status: 'AVAILABLE', description: runKey };
  if (p === '/api/v1/users') return { username: `${safeKey}_user`, password: 'L3Qa@2026', fullName: 'L3 QA API User', email: `${safeKey}@example.test`, roles: ['DISPATCHER'] };
  if (p.match(/^\/api\/v1\/users\/\{id\}$/)) return { fullName: 'L3 QA User updated', email: `${safeKey}_updated@example.test` };
  if (p.endsWith('/users/{id}/roles')) return { roles: ['DISPATCHER'] };
  if (p.endsWith('/roles/{id}/permissions')) return { permissionIds: refs.permissionIds };
  if (p.endsWith('/drivers/{id}/status')) return { status: 'ACTIVE', reasonNote: runKey };
  if (p.endsWith('/exceptions/{id}/resolve')) return { resolutionNotes: runKey };
  if (p.endsWith('/adjust-departure-time')) return { newDepartureTime: '09:00:00' };
  if (p.endsWith('/assign')) return { vehicleId: resourceId(refs.vehicle), driverId: resourceId(refs.driver) };
  if (p.endsWith('/assign-split')) {
    return {
      assignments: refs.splitStops.map((stop, index) => {
        const vehicle = splitAssignmentVehicles[index % splitAssignmentVehicles.length];
        return { vehicleId: resourceId(vehicle), driverId: vehicle.assignedDriverId, stopIds: [stop.tripDraftStopId] };
      }),
    };
  }
  if (p.endsWith('/settle-delay')) return { reason: runKey };
  if (p.endsWith('/recalculate-eta')) return { plannedDepartureTime: '09:00:00' };
  if (p === '/api/v1/trip-drafts/consolidate') {
    return { deliveryDate: testCase.id === 'L3-PLANNING-087' ? fixtureDates[5] : refs.deliveryDate };
  }
  if (p.endsWith('/trip-drafts/{id}/stops/{stopId}')) return { isActive: true, overrideNote: runKey };
  if (p.endsWith('/trip-executions/{id}/admin-override')) return { action: 'FORCE_COMPLETE_AND_RETURN', reason: runKey, defaultPendingOrderStatus: 'FAILED' };
  if (p.endsWith('/assignment')) return { vehicleId: resourceId(refs.vehicle), driverId: resourceId(refs.driver) };
  if (p.endsWith('/orders/{orderId}/result')) return { status: 'DELIVERED' };
  if (p.endsWith('/trip-stops/{id}/reject')) return { rejectionType: 'STORE_CLOSED', description: runKey };
  return undefined;
}

// Put state-changing contracts into a valid business sequence. IDs not listed retain catalog order.
const rank = new Map([
  ['L3-MASTERDATA-054', 1], ['L3-MASTERDATA-056', 2], ['L3-MASTERDATA-055', 3],
  ['L3-PLANNING-083', 10], ['L3-PLANNING-064', 11], ['L3-PLANNING-079', 12],
  ['L3-PLANNING-076', 13], ['L3-PLANNING-077', 14], ['L3-PLANNING-078', 15],
  ['L3-PLANNING-067', 20], ['L3-PLANNING-085', 21],
  ['L3-PLANNING-068', 22], ['L3-PLANNING-069', 23],
  ['L3-PLANNING-128', 24], ['L3-PLANNING-081', 25],
  ['L3-PLANNING-086', 26], ['L3-PLANNING-071', 27],
  ['L3-PLANNING-073', 28], ['L3-PLANNING-074', 29], ['L3-PLANNING-065', 30],
  ['L3-PLANNING-066', 31],
  ['L3-EXECUTION-094', 40], ['L3-EXECUTION-099', 41], ['L3-EXECUTION-095', 42],
  ['L3-EXECUTION-096', 43], ['L3-EXECUTION-129', 44], ['L3-EXECUTION-130', 45],
  ['L3-EXECUTION-097', 46], ['L3-EXECUTION-098', 47], ['L3-EXECUTION-103', 48],
  ['L3-EXECUTION-104', 49], ['L3-EXECUTION-105', 50], ['L3-EXECUTION-011', 51],
  ['L3-EXECUTION-013', 52], ['L3-EXECUTION-012', 53], ['L3-EXECUTION-009', 54],
  ['L3-EXECUTION-008', 55], ['L3-EXECUTION-010', 56], ['L3-EXECUTION-088', 57],
  ['L3-MONITORING-021', 60], ['L3-MONITORING-022', 61], ['L3-MONITORING-091', 62],
  ['L3-MONITORING-092', 63], ['L3-IDENTITY-047', 100],
]);
const indexedCases = cases.map((testCase, index) => ({ testCase, index }));
const orderedCases = indexedCases.sort((a, b) => {
  const aRank = rank.has(a.testCase.id) ? 1000 + rank.get(a.testCase.id) : a.index;
  const bRank = rank.has(b.testCase.id) ? 1000 + rank.get(b.testCase.id) : b.index;
  return aRank - bRank;
}).map(item => item.testCase);

const driverTokens = new Map();
async function tokenForDriver(driverId) {
  const driverName = `driver${String(driverId - 5).padStart(2, '0')}`;
  if (!driverTokens.has(driverName)) {
    const driverLogin = await login(driverName, driverPassword);
    driverTokens.set(driverName, driverLogin.body?.data?.accessToken ?? null);
    bootstrapRequests.push({ method: 'POST', path: '/api/v1/auth/login', status: driverLogin.status, response: `[REDACTED ${driverName} authentication response]` });
  }
  return driverTokens.get(driverName);
}
const results = [];
try {
  for (const testCase of orderedCases) {
    const caseSession = await login();
    let caseToken = caseSession.body?.data?.accessToken ?? token;
    let requestPath = materializePath(testCase);
    let body = jsonBody(testCase);

    if (testCase.path === '/api/v1/auth/refresh' || testCase.path === '/api/v1/auth/logout') {
      body = { refreshToken: caseSession.body?.data?.refreshToken };
    }
    if (testCase.id === 'L3-AUTH-119') body = { username: `${runKey}-unknown`, password: 'Wrong@123' };
    if (testCase.id === 'L3-AUTH-120') body = { username: disabledUsername, password: disabledPassword };
    if (testCase.id === 'L3-AUTH-121') body = { refreshToken: `unknown-${runKey}` };
    if (testCase.id === 'L3-AUTH-122') body = { refreshToken: refs.expiredRefreshToken ?? 'L3QA_EXPIRED_REFRESH_TOKEN' };
    if (testCase.id === 'L3-MASTERDATA-123') body = { ...vehicleBody(), vehicleCode: `${runKey}-VD`.slice(0, 50), plateNumber: refs.createdVehicle?.plateNumber };
    if (testCase.id === 'L3-MASTERDATA-124') body = { ...jsonBody({ path: '/api/v1/products' }), sku: refs.createdProduct?.sku ?? refs.product?.sku };
    if (testCase.id === 'L3-MASTERDATA-125') body = { ...jsonBody({ path: '/api/v1/stores' }), storeCode: refs.createdStore?.storeCode ?? refs.store?.storeCode };
    if (testCase.id === 'L3-MASTERDATA-126') body = { ...jsonBody({ path: '/api/v1/routes' }), code: refs.createdRoute?.code ?? refs.route?.code };
    if (testCase.id === 'L3-IDENTITY-110') {
      requestPath = `/api/v1/users/${resourceId(refs.createdUser)}/roles`;
    }

    if (isMultipartImportCase(testCase)) {
      const fixture = testCase.id === 'L3-IMPORT-127' ? oversizedFixturePath : primaryUploadFixturePath;
      body = multipartFixture(fixture, fixtureDates[3]);
    }

    const driverCase = testCase.path.startsWith('/api/v1/driver/')
      || testCase.path.startsWith('/api/v1/trip-stops/')
      || testCase.path === '/api/v1/trips/my-trips'
      || testCase.path === '/api/v1/trips/my-trips/calendar'
      || testCase.id === 'L3-EXECUTION-097'
      || testCase.id === 'L3-EXECUTION-099'
      || testCase.id === 'L3-EXECUTION-098'
      || testCase.id === 'L3-EXECUTION-130';
    if (driverCase) {
      const driverId = testCase.path.startsWith('/api/v1/driver/') || testCase.id === 'L3-EXECUTION-099'
        ? driverIdOfTrip(refs.driverTrip, assignmentVehicles[2].assignedDriverId)
        : testCase.id === 'L3-EXECUTION-105'
          ? driverIdOfTrip(refs.rejectTrip, assignmentVehicles[3].assignedDriverId)
          : testCase.id === 'L3-EXECUTION-130'
            ? driverIdOfTrip(refs.splitTrip, splitAssignmentVehicles[0].assignedDriverId)
            : driverIdOfTrip(refs.trip, assignmentVehicles[0].assignedDriverId);
      caseToken = await tokenForDriver(driverId) ?? caseToken;
    }

    if (testCase.id === 'L3-EXECUTION-105' && refs.rejectTrip && refs.rejectTripStop) {
      await bootstrapRequest('POST', `/api/v1/trips/${resourceId(refs.rejectTrip)}/start`, undefined, caseToken);
      await bootstrapRequest('POST', `/api/v1/trip-stops/${resourceId(refs.rejectTripStop)}/arrive`, undefined, caseToken);
    }
    if (testCase.id === 'L3-EXECUTION-103' && refs.tripStop) {
      execMysql(`UPDATE trip_stops SET planned_eta = TIMESTAMPADD(MINUTE, 5, NOW()) WHERE trip_stop_id = ${Number(resourceId(refs.tripStop))};`);
    }

    const actual = await rawRequest(testCase.httpMethod, requestPath, body, caseToken);
    const classified = classifyResult(testCase, actual);
    const responseExcerpt = testCase.path.includes('/auth/')
      ? '[REDACTED authentication response body]'
      : (JSON.stringify(actual.body)?.slice(0, 2000) ?? '');
    results.push({
      testId: testCase.id,
      level: 'L3',
      title: testCase.title,
      method: testCase.httpMethod,
      path: requestPath,
      requestBody: redactBody(requestPath, body),
      expectedStatus: Number(testCase.expectedStatus),
      expectedErrorCode: testCase.errorCode === 'N/A' ? null : testCase.errorCode,
      actualStatus: actual.status,
      actualErrorCode: actual.body?.error?.code ?? null,
      status: classified.status,
      assertion: classified.assertion,
      reason: classified.status === 'Not Run' ? classified.assertion : undefined,
      defectId: classified.status === 'Fail' ? `L3-DEF-${testCase.id}` : undefined,
      durationMs: actual.durationMs,
      responseExcerpt,
      evidence: [path.basename(evidencePath)],
    });

    const returned = dataOf(actual);
    if (testCase.id === 'L3-IMPORT-027' && actual.status === 201) refs.importBatch = returned;
    if (testCase.id === 'L3-MASTERDATA-040' && actual.status === 201) refs.createdProduct = returned;
    if (testCase.id === 'L3-MASTERDATA-049' && actual.status === 201) refs.createdRoute = returned;
    if (testCase.id === 'L3-MASTERDATA-054' && actual.status === 201) {
      refs.routeStop = returned;
      refs.routeStopIds = [resourceId(returned)];
    }
    if (testCase.id === 'L3-MASTERDATA-055' && actual.status === 200 && refs.routeStop) refs.routeStopIds = refs.routeStopIds.filter(id => id !== resourceId(refs.routeStop));
    if (testCase.id === 'L3-MASTERDATA-058' && actual.status === 201) refs.createdStore = returned;
    if (testCase.id === 'L3-MASTERDATA-113' && actual.status === 201) refs.createdVehicle = returned;
    if (testCase.id === 'L3-IDENTITY-107' && actual.status === 201) refs.createdUser = returned;
    if (testCase.id === 'L3-PLANNING-065' && actual.status === 201) refs.trip = returned;
    if (testCase.id === 'L3-PLANNING-066' && actual.status === 201) refs.splitTrip = returned?.trips?.[0] ?? returned?.tripResponses?.[0];

    if (testCase.id === 'L3-PLANNING-065' || testCase.id === 'L3-PLANNING-066') {
      const draft = testCase.id.endsWith('066') ? refs.splitDraft : refs.tripDraft;
      if (testCase.id.endsWith('066') && refs.splitTrip) {
        refs.splitTrip = { ...refs.splitTrip, driver: { userId: splitAssignmentVehicles[0].assignedDriverId } };
      }
      if (testCase.id.endsWith('065') && refs.trip) refs.tripStop = refs.trip.tripStops?.[0] ?? refs.tripStop;
    }
    if (testCase.id === 'L3-EXECUTION-095' && actual.status === 200) {
      refs.trip = returned;
      refs.tripStop = returned?.tripStops?.[0] ?? refs.tripStop;
      refs.execution = returned?.executionId ? { executionId: returned.executionId } : refs.execution;
    }
    if (testCase.id === 'L3-EXECUTION-011' && actual.status === 200) {
      refs.driverExecution = returned?.executionId ? { executionId: returned.executionId } : refs.driverExecution;
      refs.driverTripStop = returned?.stops?.[0] ?? refs.driverTripStop;
    }
    if (testCase.id === 'L3-EXECUTION-103' && actual.status === 200 && returned?.exceptionId) {
      refs.exception = { exceptionId: returned.exceptionId };
    }
    if (testCase.id === 'L3-EXECUTION-105' && actual.status === 201) {
      refs.exception = returned;
    }
    if (rank.get(testCase.id) >= 47 && rank.get(testCase.id) <= 56) {
      refs.exception = await firstResource('/api/v1/exceptions?page=0&size=1', token) ?? refs.exception;
      refs.outcome = await firstResource('/api/v1/trip-outcomes?page=0&size=1', token) ?? refs.outcome;
    }
  }
} finally {
  const resolvedFixtureDir = path.resolve(fixtureDir);
  if (!resolvedFixtureDir.startsWith(path.resolve(os.tmpdir()))) {
    throw new Error(`Refusing to clean unexpected fixture path: ${resolvedFixtureDir}`);
  }
  fs.rmSync(resolvedFixtureDir, { recursive: true, force: true });
}

// Preserve authoritative catalog order in the published evidence.
const resultById = new Map(results.map(result => [result.testId, result]));
const catalogOrderedResults = cases.map(testCase => resultById.get(testCase.id));
const summary = catalogOrderedResults.reduce((acc, result) => {
  acc[result.status] = (acc[result.status] ?? 0) + 1;
  return acc;
}, { Pass: 0, Fail: 0, 'Not Run': 0 });
const output = {
  generatedAt: new Date().toISOString(),
  baseUrl,
  catalog: catalogPath,
  total: catalogOrderedResults.length,
  summary,
  runNamespace: runKey,
  bootstrap: {
    loginStatus: loginResult.status,
    fixtureDates,
    requests: bootstrapRequests,
    referenceIds: Object.fromEntries(Object.entries(refs).map(([key, value]) => [key, Array.isArray(value) ? value : (typeof value === 'object' ? resourceId(value, null) : value)])),
    cleanup: `Removed temporary XLSX directory ${fixtureDir}; database cleanup is performed by the execution harness against its dedicated elog_l3_qa_* schema.`,
  },
  results: catalogOrderedResults,
};
fs.mkdirSync(path.dirname(evidencePath), { recursive: true });
fs.writeFileSync(evidencePath, JSON.stringify(output, null, 2));
fs.mkdirSync(path.dirname(ledgerPath), { recursive: true });
fs.writeFileSync(ledgerPath, JSON.stringify({
  generatedAt: output.generatedAt,
  level: 'L3',
  total: output.total,
  summary,
  results: catalogOrderedResults.map(result => ({
    testId: result.testId,
    level: result.level,
    status: result.status,
    assertion: result.assertion,
    reason: result.reason,
    defectId: result.defectId,
    evidence: result.evidence,
  })),
}, null, 2));
console.log(JSON.stringify({ total: catalogOrderedResults.length, summary, evidencePath, ledgerPath }, null, 2));
if (catalogOrderedResults.length !== 130 || catalogOrderedResults.some(result => !result)) process.exitCode = 2;
