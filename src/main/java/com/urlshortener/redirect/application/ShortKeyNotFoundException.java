package com.urlshortener.redirect.application;

import com.urlshortener.domain.ShortKey;
import com.urlshortener.web.ApiException;
import org.springframework.http.HttpStatus;

public class ShortKeyNotFoundException extends ApiException {

    public ShortKeyNotFoundException(ShortKey shortKey) {
        super("ShortKey not found: " + (shortKey == null ? "(null)" : shortKey.value()));
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String code() {
        return "NOT_FOUND";
    }

    @Override
    public String clientMessage() {
        return getMessage();
    }
}
