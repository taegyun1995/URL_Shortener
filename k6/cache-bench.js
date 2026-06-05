/**
 * L2(Redis) DB 방어 효과 측정 — 공정 비교용.
 *
 * 86만 시드 키를 랜덤 조회한다(L1 1만은 ~1.2%만 담아 대부분 L1 miss → L2/DB로 내려감).
 * 시간 고정(duration)이 아니라 **요청 수 고정**(shared-iterations)으로 돌려,
 * L2 on/off 간 처리량 차이가 DB 쿼리 절대수를 오염시키지 않게 한다.
 * GET 100%. 외부에서 MySQL Com_select 증가량을 비교한다.
 *
 * 실행:
 *  docker compose --profile load-test run --rm k6 run /scripts/cache-bench.js
 */

import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        fixed: {
            executor: 'shared-iterations',
            vus: 50,
            iterations: 200000,   // 양쪽 동일하게 정확히 20만 요청
            maxDuration: '120s',
        },
    },
    thresholds: {},
};

const BASE_URL = __ENV.BASE_URL || 'http://app:8080';
const SEED_COUNT = 800000;     // 시드된 키 풀 (L1 1만보다 훨씬 큼 → L1 miss 빈발)
const SEED_OFFSET = 1000000;

export default function () {
    const id = Math.floor(Math.random() * SEED_COUNT) + 1;
    const shortKey = (id + SEED_OFFSET).toString(36).toUpperCase().padStart(7, '0');
    const res = http.get(`${BASE_URL}/${shortKey}`, { redirects: 0 });
    check(res, { 'redirect 302': (r) => r.status === 302 });
}
