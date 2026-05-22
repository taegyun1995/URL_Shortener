/**
 * 1단계 한계점 발견용 램프업.
 *
 * VU 50 → 100 → 200 → 400 → 800 단계별 증가.
 * 각 단계 30초 유지 후 다음으로.
 * 어디서 에러/Latency 급증하는지 관찰.
 *
 * 실행:
 *  docker compose --profile load-test run --rm k6 run /scripts/ramp-up.js
 */

import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '15s', target: 50 },    // warmup
        { duration: '30s', target: 50 },
        { duration: '15s', target: 100 },
        { duration: '30s', target: 100 },
        { duration: '15s', target: 200 },
        { duration: '30s', target: 200 },
        { duration: '15s', target: 400 },
        { duration: '30s', target: 400 },
        { duration: '15s', target: 800 },
        { duration: '30s', target: 800 },
        { duration: '15s', target: 0 },     // ramp down
    ],
    // 임계점 발견이 목적이라 threshold abort 없이 끝까지 실행
    thresholds: {},
};

const BASE_URL = __ENV.BASE_URL || 'http://app:8080';
const READ_RATIO = 0.9;
const SEED_COUNT = 10000;
const SEED_OFFSET = 1000000;

export default function () {
    if (Math.random() < READ_RATIO) {
        doRedirect();
    } else {
        doShorten();
    }
}

function doRedirect() {
    const id = Math.floor(Math.random() * SEED_COUNT) + 1;
    const shortKey = (id + SEED_OFFSET).toString(36).toUpperCase().padStart(7, '0');
    const res = http.get(`${BASE_URL}/${shortKey}`, { redirects: 0 });
    check(res, { 'redirect 302': (r) => r.status === 302 });
}

function doShorten() {
    const url = `https://example.com/runtime/${Date.now()}-${Math.random()}`;
    const res = http.post(
        `${BASE_URL}/api/shorten`,
        JSON.stringify({ url }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, { 'shorten 201': (r) => r.status === 201 });
}
