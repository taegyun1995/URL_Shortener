/**
 * 1단계 베이스라인 부하 테스트.
 *
 * 시나리오:
 *  - VU 50, 30초 지속
 *  - 읽기 90% (GET /{shortKey}) : 쓰기 10% (POST /api/shorten)
 *  - 시드된 10,000건의 short_key 풀에서 랜덤 조회
 *
 * 실행:
 *  docker compose --profile load-test run --rm k6 run /scripts/baseline.js
 */

import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 50,
    duration: '30s',
    thresholds: {
        // 베이스라인은 임계치 발견이 목적이라 느슨하게.
        'http_req_failed': ['rate<0.01'],          // 에러율 1% 미만
        'http_req_duration': ['p(95)<2000'],       // p95 < 2초 (병목 시 자연스럽게 깨짐)
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://app:8080';
const READ_RATIO = 0.9;
const SEED_COUNT = 10000;
const SEED_OFFSET = 1000000; // seed.sql과 동일하게 맞춤

export default function () {
    if (Math.random() < READ_RATIO) {
        doRedirect();
    } else {
        doShorten();
    }
}

function doRedirect() {
    const id = Math.floor(Math.random() * SEED_COUNT) + 1;
    // MySQL CONV()는 대문자 반환 → JS toString(36)은 소문자 → 일치시켜야 함
    const shortKey = (id + SEED_OFFSET).toString(36).toUpperCase().padStart(7, '0');
    const res = http.get(`${BASE_URL}/${shortKey}`, { redirects: 0 });
    check(res, {
        'redirect 302': (r) => r.status === 302,
    });
}

function doShorten() {
    const url = `https://example.com/runtime/${Date.now()}-${Math.random()}`;
    const res = http.post(
        `${BASE_URL}/api/shorten`,
        JSON.stringify({ url }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, {
        'shorten 201': (r) => r.status === 201,
    });
}
