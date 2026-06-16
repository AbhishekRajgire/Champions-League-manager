package com.ucl.service;

import com.ucl.dto.AuditLogResponse;
import com.ucl.model.AuditLog;
import com.ucl.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Records an audit trail of data changes (who / what / when) and exposes it for
 * the admin tools view. Result-changing services call {@link #log} after a
 * successful change.
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void log(String action, String target, String detail) {
        repository.save(new AuditLog(Instant.now(), currentUsername(), action, target, detail));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> recent() {
        return repository.findTop200ByOrderByTimestampDescIdDesc().stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    /** Audit entries made by the currently authenticated user only. */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> recentForCurrentUser() {
        return repository.findTop200ByUsernameOrderByTimestampDescIdDesc(currentUsername()).stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    /** The authenticated username, or "system" if there is no real user in context. */
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "system";
        }
        return auth.getName();
    }
}
