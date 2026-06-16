package com.ucl.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * An immutable record of a change to tournament data — primarily who set or
 * cleared a result, and when. Written by {@link com.ucl.service.AuditService}
 * and never updated after creation.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    /** Username of the actor, or "system" when no authenticated user is present. */
    @Column(nullable = false)
    private String username;

    /** Machine-readable action, e.g. RESULT_SET, RESULT_CLEARED, KNOCKOUT_RESULT, IMPORT_TEAMS. */
    @Column(nullable = false)
    private String action;

    /** Human description of what was affected, e.g. "MD3 · Real Madrid vs Barcelona". */
    @Column(nullable = false, length = 512)
    private String target;

    /** What changed, e.g. "— → 2-1". */
    @Column(length = 512)
    private String detail;

    public AuditLog() {
    }

    public AuditLog(Instant timestamp, String username, String action, String target, String detail) {
        this.timestamp = timestamp;
        this.username = username;
        this.action = action;
        this.target = target;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getTarget() {
        return target;
    }

    public String getDetail() {
        return detail;
    }
}
