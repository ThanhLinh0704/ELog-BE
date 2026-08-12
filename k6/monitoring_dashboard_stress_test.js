import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// NFR-PERF02: k6 Stress Test Active Dashboard (L3-PRF-02)
// 100 VUs for 10 minutes, p95 < 500ms, throughput > 500 req/sec

export const errorRate = new Rate('errors');
export const dashboardTrend = new Trend('dashboard_duration');

export const options = {
  stages: [
    { duration: '1m', target: 100 },  // Ramp up to 100 VUs
    { duration: '8m', target: 100 },  // Stress load 100 VUs
    { duration: '1m', target: 0 },    // Ramp down
  ],
  thresholds: {
    dashboard_duration: ['p(95)<500'],
    errors: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export default function () {
  const url = `${BASE_URL}/monitoring/active-trips`;
  const params = {
    headers: {
      'Accept': 'application/json',
    },
  };

  const res = http.get(url, params);
  
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(!success);
  dashboardTrend.add(res.timings.duration);
  sleep(0.1); // High frequency stress requests
}
