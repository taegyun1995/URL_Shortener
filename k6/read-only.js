import http from 'k6/http';
import { check } from 'k6';
export const options = { scenarios: { r: { executor: 'constant-vus', vus: 1000, duration: '25s' } }, thresholds: {} };
const BASE_URL = __ENV.BASE_URL || 'http://app:8080';
const N = 10000;
export default function () {
  const u = Math.random();
  const rank = Math.min(Math.max(Math.floor(Math.pow(N, u)), 1), N);
  const key = (rank + 1000000).toString(36).toUpperCase().padStart(7, '0');
  const res = http.get(`${BASE_URL}/${key}`, { redirects: 0 });
  check(res, { '302': (r) => r.status === 302 });
}
