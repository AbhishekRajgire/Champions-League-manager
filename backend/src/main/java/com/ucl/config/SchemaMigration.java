package com.ucl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lightweight, idempotent schema touch-ups that Hibernate's {@code ddl-auto=update}
 * won't perform on its own (it never alters existing column types).
 *
 * <p>Specifically: the {@code users.role} column was first created as a MySQL
 * {@code ENUM('ADMIN','USER')}. Adding the new {@code MODERATOR} role then fails
 * with "Data truncated for column 'role'", so we widen it to a plain VARCHAR that
 * accepts any role name. Runs before {@link DataSeeder} (which inserts users).
 */
@Component
@Order(0)
public class SchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL");
            log.info("Schema migration: users.role ensured VARCHAR(20)");
        } catch (Exception ex) {
            // Table may not exist yet on a fresh DB (Hibernate creates it as VARCHAR
            // anyway), or the column is already widened — safe to ignore.
            log.warn("Schema migration for users.role skipped: {}", ex.getMessage());
        }
    }
}
