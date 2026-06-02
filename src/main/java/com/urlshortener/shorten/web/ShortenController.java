package com.urlshortener.shorten.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urlshortener.domain.ShortKey;
import com.urlshortener.shorten.application.ShortenService;

@RestController
@RequestMapping("/api")
@Slf4j
public class ShortenController {

    private final ShortenService shortenService;
    private final String     shortUrlHost;

    public ShortenController(
            ShortenService shortenService,
            @Value("${app.short-url-host}") String shortUrlHost
    ) {
        this.shortenService = shortenService;
        this.shortUrlHost = shortUrlHost;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortKey key = shortenService.shorten(request.url());
        String shortUrl = shortUrlHost + "/" + key.value();
        log.debug("shorten {} -> {}", request.url(), key);
        return ResponseEntity.status(201).body(new ShortenResponse(shortUrl));
    }

    public record ShortenRequest(@NotBlank String url) {}

    public record ShortenResponse(String shortUrl) {}
}
