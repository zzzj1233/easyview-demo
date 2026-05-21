import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  vus: 20,
  duration: '30m',
  thresholds: {
    http_req_failed:   ['rate<0.05'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:28080';

export default function () {
  const id = Math.floor(Math.random() * 5) + 1;
  const res = http.get(`${BASE}/api/order/${id}`);
  check(res, { '200': (r) => r.status === 200 });
  sleep(0.3);
}
