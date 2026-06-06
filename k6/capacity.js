/**
 * 한계 동시성 측정 — VU를 1000까지 단계적으로 올려 "몇 명까지 버티나"를 찾는다.
 *
 * 각 단계를 충분히 유지(40s)하며 실패율·p95가 무너지는 지점을 관찰한다.
 * 읽기 90% : 쓰기 10%, 지프 분포(현실적 hot key 집중).
 *
 * thresholds로 SLO를 정의 — 깨지면 어느 VU에서 깨졌는지 단계별 요약으로 판단.
 *
 * 실행:
 *  docker compose --profile load-test run --rm k6 run /scripts/capacity.js
 */

import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '15s', target: 100 },
        { duration: '40s', target: 100 },
        { duration: '15s', target: 300 },
        { duration: '40s', target: 300 },
        { duration: '15s', target: 500 },
        { duration: '40s', target: 500 },
        { duration: '15s', target: 800 },
        { duration: '40s', target: 800 },
        { duration: '15s', target: 1000 },
        { duration: '40s', target: 1000 },
        { duration: '15s', target: 0 },
    ],
    thresholds: {
        // SLO: 실패율 1% 미만, p95 200ms 미만. 깨지면 한계 초과 신호.
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<200'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://app:8080';
const READ_RATIO = 0.9;
const SEED_OFFSET = 1000000;
const ZIPF_N = 10000;

function zipfRank() {
    const u = Math.random();
    const rank = Math.floor(Math.pow(ZIPF_N, u));
    return Math.min(Math.max(rank, 1), ZIPF_N);
}

export default function () {
    if (Math.random() < READ_RATIO) {
        const id = zipfRank();
        const key = (id + SEED_OFFSET).toString(36).toUpperCase().padStart(7, '0');
        const res = http.get(`${BASE_URL}/${key}`, { redirects: 0 });
        check(res, { 'redirect 302': (r) => r.status === 302 });
    } else {
        const url = `https://example.com/runtime/${Date.now()}-${Math.random()}`;
        const res = http.post(`${BASE_URL}/api/shorten`, JSON.stringify({ url }),
            { headers: { 'Content-Type': 'application/json' } });
        check(res, { 'shorten 201': (r) => r.status === 201 });
    }
}
