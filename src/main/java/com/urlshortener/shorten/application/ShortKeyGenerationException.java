package com.urlshortener.shorten.application;

import com.urlshortener.web.ApiException;
import org.springframework.http.HttpStatus;

/**
 * 랜덤 short_key가 최대 재시도 횟수만큼 연속으로 충돌해 키 발급에 실패했을 때 발생.
 * 키공간(62^7 ≈ 3.5조)을 생각하면 정상 상황에선 거의 일어나지 않는다.
 * 발생한다면 키공간 고갈 또는 무언가 비정상이라는 신호다.
 */
public class ShortKeyGenerationException extends ApiException {

    public ShortKeyGenerationException(String longUrl, int attempts) {
        super("short key generation failed after " + attempts + " attempts for: " + longUrl);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @Override
    public String code() {
        return "SHORT_KEY_GENERATION_FAILED";
    }

    @Override
    public String clientMessage() {
        return "단축 키 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.";
    }
}
