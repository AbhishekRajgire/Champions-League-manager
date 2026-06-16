package com.ucl.controller;

import com.ucl.dto.AuditLogResponse;
import com.ucl.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLogResponse> recent() {
        return auditService.recent();
    }

    /** A moderator (or admin) can see the changes they themselves made. */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public List<AuditLogResponse> mine() {
        return auditService.recentForCurrentUser();
    }
}
