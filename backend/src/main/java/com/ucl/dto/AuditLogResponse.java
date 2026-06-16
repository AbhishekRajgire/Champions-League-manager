package com.ucl.dto;

import com.ucl.model.AuditLog;

public record AuditLogResponse(
        Long id,
        String timestamp,
        String username,
        String action,
        String target,
        String detail
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getTimestamp().toString(),
                a.getUsername(),
                a.getAction(),
                a.getTarget(),
                a.getDetail());
    }
}
