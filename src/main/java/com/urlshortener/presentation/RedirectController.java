package com.urlshortener.presentation;

import com.urlshortener.application.UrlService;
import com.urlshortener.domain.ShortKey;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {

    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortKey}")
    public ResponseEntity<Void> redirect(@PathVariable String shortKey) {
        String longUrl = urlService.resolve(ShortKey.of(shortKey));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
