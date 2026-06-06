/**
 * L2(Redis) DB 방어 효과 측정 — 지프(Zipf) 분포 + 고정 요청수.
 *
 * 균등 랜덤(86만 키 동일 확률)은 캐시 worst-case라 hit rate가 바닥(~10%)이다.
 * 실서비스 트래픽은 소수 인기 URL에 집중(파레토/지프)되므로 그걸 재현한다.
 * 요청 수 고정(shared-iterations)으로 L2 on/off 간 처리량 차이가 DB 쿼리 절대수를
 * 오염시키지 않게 한다. 외부에서 MySQL Com_select 증가량을 비교한다.
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
            iterations: 200000,
            maxDuration: '120s',
        },
    },
    thresholds: {},
};

const BASE_URL = __ENV.BASE_URL || 'http://app:8080';
const SEED_COUNT = 800000;
const SEED_OFFSET = 1000000;
const ZIPF_N = 10000;        // 인기 키 풀 = seed 10K 전체 (404 없음).
                             // L1을 1000으로 낮춰 측정하면 ~9000키가 L2 구간이 된다.

// 지수형 역변환: rank = N^u → u가 0~1 균등이어도 rank는 작은 값(인기 키)에 강하게 집중.
// 예) N=5만일 때 상위 10개가 전체의 ~20%, 상위 1000개가 ~70%를 차지하는 현실적 지프.
function zipfRank() {
    const u = Math.random();
    const rank = Math.floor(Math.pow(ZIPF_N, u));
    return Math.min(Math.max(rank, 1), ZIPF_N);
}

export default function () {
    // 인기 키는 상위 ZIPF_N(=1만)에 집중. id가 작을수록 자주 조회됨.
    const id = zipfRank();
    const shortKey = (id + SEED_OFFSET).toString(36).toUpperCase().padStart(7, '0');
    const res = http.get(`${BASE_URL}/${shortKey}`, { redirects: 0 });
    check(res, { 'redirect 302': (r) => r.status === 302 });
}
