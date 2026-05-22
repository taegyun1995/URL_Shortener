package com.urlshortener.presentation;

import com.urlshortener.application.UrlService;
import com.urlshortener.domain.ShortKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ShortenController {

    private final UrlService urlService;
    private final String shortUrlHost;

    public ShortenController(UrlService urlService,
                             @Value("${app.short-url-host}") String shortUrlHost) {
        this.urlService = urlService;
        this.shortUrlHost = shortUrlHost;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortKey key = urlService.shorten(request.url());
        String shortUrl = shortUrlHost + "/" + key.value();
        return ResponseEntity.status(201).body(new ShortenResponse(shortUrl));
    }

    public record ShortenRequest(@NotBlank String url) {}

    public record ShortenResponse(String shortUrl) {}
}
