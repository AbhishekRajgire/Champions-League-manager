package com.ucl.model;

/**
 * The single-elimination knockout rounds, ordered from first to last.
 *
 * <p>The enum order is the bracket order, so {@link #next()} walks towards the
 * {@link #FINAL} and {@code ordinal()} can be used to sort rounds for display.
 * Each round carries how many matches it contains, from which the number of
 * teams entering that round is derived ({@code matchCount * 2}).
 */
public enum KnockoutRound {
    ROUND_OF_16("Round of 16", 8),
    QUARTER_FINAL("Quarter-Final", 4),
    SEMI_FINAL("Semi-Final", 2),
    FINAL("Final", 1);

    private final String label;
    private final int matchCount;

    KnockoutRound(String label, int matchCount) {
        this.label = label;
        this.matchCount = matchCount;
    }

    public String getLabel() {
        return label;
    }

    public int getMatchCount() {
        return matchCount;
    }

    /** Number of teams that enter this round. */
    public int getTeamCount() {
        return matchCount * 2;
    }

    /** The following round, or {@code null} if this is the {@link #FINAL}. */
    public KnockoutRound next() {
        return this == FINAL ? null : values()[ordinal() + 1];
    }

    /** The round in which exactly {@code teamCount} teams take part. */
    public static KnockoutRound forTeamCount(int teamCount) {
        for (KnockoutRound r : values()) {
            if (r.getTeamCount() == teamCount) {
                return r;
            }
        }
        throw new IllegalArgumentException("No knockout round for " + teamCount + " teams");
    }
}
