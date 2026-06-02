package com.urlshortener.redirect.web;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.urlshortener.redirect.application.RedirectService;
import com.urlshortener.domain.ShortKey;

@RestController
@Slf4j
public class RedirectController {

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    @GetMapping("/{shortKey}")
    public ResponseEntity<Void> redirect(@PathVariable String shortKey) {
        String longUrl = redirectService.resolve(ShortKey.of(shortKey));
        log.debug("redirect {} -> {}", shortKey, longUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                             .location(URI.create(longUrl))
                             .build();
    }
}
