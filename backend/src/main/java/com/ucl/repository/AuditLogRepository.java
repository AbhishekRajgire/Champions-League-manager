package com.ucl.repository;

import com.ucl.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Most recent entries first, capped to keep the response bounded. */
    List<AuditLog> findTop200ByOrderByTimestampDescIdDesc();

    /** Most recent entries for a single actor (for the moderator's own view). */
    List<AuditLog> findTop200ByUsernameOrderByTimestampDescIdDesc(String username);
}
