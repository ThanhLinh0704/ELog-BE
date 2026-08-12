import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// NFR-PERF03: k6 Endurance Driver Arrive (L3-PRF-03)
// 30 VUs for 1 hour endurance, zero memory leak, db connection pool usage < 70%

export const errorRate = new Rate('errors');
export const arriveTrend = new Trend('arrive_duration');

export const options = {
  stages: [
    { duration: '2m', target: 30 },   // Ramp up to 30 VUs
    { duration: '56m', target: 30 },  // Sustained endurance 30 VUs
    { duration: '2m', target: 0 },    // Ramp down
  ],
  thresholds: {
    arrive_duration: ['p(95)<1000'],
    errors: ['rate<0.005'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export default function () {
  const url = `${BASE_URL}/driver/trips/stops/10/arrive`;
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(url, JSON.stringify({ notes: 'Arrived on time' }), params);
  
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  arriveTrend.add(res.timings.duration);
  sleep(2);
}
