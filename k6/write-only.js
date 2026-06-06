/**
 * 쓰기 100% (POST /api/shorten) 처리량 측정 — 쓰기 경로의 순수 천장.
 *
 * 혼합(읽기90:쓰기10) 측정에선 쓰기 효과가 묻히므로, POST만 돌려
 * INSERT 1회 전환이 쓰기 경로를 얼마나 빠르게 했는지 본다.
 * 매 요청 유니크 URL이라 dedup 없이 항상 신규 INSERT.
 *
 * 실행:
 *  docker compose --profile load-test run --rm k6 run /scripts/write-only.js
 */

import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        w: { executor: 'constant-vus', vus: 200, duration: '25s' },
    },
    thresholds: {},
};

const BASE_URL = __ENV.BASE_URL || 'http://app:8080';

export default function () {
    // 매번 유니크 URL → 항상 신규 단축(INSERT). VU+iteration+time으로 충돌 회피.
    const url = `https://example.com/w/${__VU}-${__ITER}-${Date.now()}`;
    const res = http.post(`${BASE_URL}/api/shorten`, JSON.stringify({ url }),
        { headers: { 'Content-Type': 'application/json' } });
    check(res, { 'shorten 201': (r) => r.status === 201 });
}
