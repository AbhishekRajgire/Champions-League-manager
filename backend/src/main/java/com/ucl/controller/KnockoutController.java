package com.ucl.controller;

import com.ucl.dto.BracketResponse;
import com.ucl.dto.KnockoutMatchResponse;
import com.ucl.dto.ResultRequest;
import com.ucl.service.KnockoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knockout")
public class KnockoutController {

    private final KnockoutService knockoutService;

    public KnockoutController(KnockoutService knockoutService) {
        this.knockoutService = knockoutService;
    }

    @GetMapping("/bracket")
    public BracketResponse getBracket() {
        return knockoutService.getBracket();
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public BracketResponse generate(@RequestParam(defaultValue = "16") int qualifiers) {
        return knockoutService.generate(qualifiers);
    }

    @PutMapping("/{id}/result")
    @PreAuthorize("hasRole('ADMIN')")
    public KnockoutMatchResponse updateResult(@PathVariable Long id, @Valid @RequestBody ResultRequest request) {
        return knockoutService.submitResult(id, request);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        knockoutService.reset();
    }
}
