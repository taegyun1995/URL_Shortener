package com.urlshortener.presentation;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.urlshortener.application.UrlService;
import com.urlshortener.domain.ShortKey;

@RestController
@Slf4j
public class RedirectController {

    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortKey}")
    public ResponseEntity<Void> redirect(@PathVariable String shortKey) {
        String longUrl = urlService.resolve(ShortKey.of(shortKey));
        log.debug("redirect {} -> {}", shortKey, longUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                             .location(URI.create(longUrl))
                             .build();
    }
}
