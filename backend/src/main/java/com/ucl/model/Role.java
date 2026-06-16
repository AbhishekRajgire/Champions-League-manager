package com.ucl.model;

public enum Role {
    /** Full control: teams, draws, import/export, user management, results. */
    ADMIN,
    /** Match official: may enter and clear results only. */
    MODERATOR,
    /** Regular fan: read-only browsing plus personal predictions. */
    USER
}
