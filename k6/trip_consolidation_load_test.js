import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// NFR-PERF01: k6 Load Test Consolidate (L3-PRF-01)
// 50 VUs for 5 minutes, p95 < 2000ms, error rate < 0.1%

export const errorRate = new Rate('errors');
export const consolidateTrend = new Trend('consolidate_duration');

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp up to 50 VUs
    { duration: '4m', target: 50 },   // Stay at 50 VUs
    { duration: '30s', target: 0 },   // Ramp down
  ],
  thresholds: {
    consolidate_duration: ['p(95)<2000'],
    errors: ['rate<0.001'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const TOKEN = __ENV.DISPATCHER_TOKEN || 'mock-dispatcher-jwt-token';

export default function () {
  const url = `${BASE_URL}/trip-drafts/consolidate`;
  const payload = JSON.stringify({ deliveryDate: '2026-08-20' });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${TOKEN}`,
    },
  };

  const res = http.post(url, payload, params);
  
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'has valid response body': (r) => r.body && r.body.includes('success'),
  });

  errorRate.add(!success);
  consolidateTrend.add(res.timings.duration);
  sleep(1);
}
