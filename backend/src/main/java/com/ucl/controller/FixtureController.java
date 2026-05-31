package com.ucl.controller;

import com.ucl.dto.FixtureResponse;
import com.ucl.dto.GenerationResponse;
import com.ucl.dto.ResultRequest;
import com.ucl.service.FixtureGenerationService;
import com.ucl.service.FixtureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fixtures")
public class FixtureController {

    private final FixtureService fixtureService;
    private final FixtureGenerationService generationService;

    public FixtureController(FixtureService fixtureService, FixtureGenerationService generationService) {
        this.fixtureService = fixtureService;
        this.generationService = generationService;
    }

    @GetMapping
    public List<FixtureResponse> getAll() {
        return fixtureService.getAllFixtures();
    }

    @GetMapping("/by-matchday")
    public Map<Integer, List<FixtureResponse>> getByMatchday() {
        return fixtureService.getFixturesByMatchday();
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public GenerationResponse generate() {
        return generationService.generate();
    }

    @PutMapping("/{id}/result")
    @PreAuthorize("hasRole('ADMIN')")
    public FixtureResponse updateResult(@PathVariable Long id, @Valid @RequestBody ResultRequest request) {
        return fixtureService.updateResult(id, request);
    }

    @PatchMapping("/{id}/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public FixtureResponse clearResult(@PathVariable Long id) {
        return fixtureService.clearResult(id);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        fixtureService.resetAllFixtures();
    }
}
