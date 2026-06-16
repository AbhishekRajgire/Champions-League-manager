package com.ucl.controller;

import com.ucl.dto.ImportResult;
import com.ucl.service.ImportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * Admin-only bulk data tools: CSV import of teams / results and CSV export of
 * the standings table and knockout bracket.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
public class ImportExportController {

    private final ImportExportService service;

    public ImportExportController(ImportExportService service) {
        this.service = service;
    }

    @PostMapping(value = "/import/teams", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importTeams(@RequestParam("file") MultipartFile file) {
        return service.importTeams(file);
    }

    @PostMapping(value = "/import/results", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importResults(@RequestParam("file") MultipartFile file) {
        return service.importResults(file);
    }

    @GetMapping("/export/standings")
    public ResponseEntity<byte[]> exportStandings() {
        return csv("standings.csv", service.exportStandingsCsv());
    }

    @GetMapping("/export/bracket")
    public ResponseEntity<byte[]> exportBracket() {
        return csv("bracket.csv", service.exportBracketCsv());
    }

    private static ResponseEntity<byte[]> csv(String filename, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
