package com.ucl.model;

import jakarta.persistence.*;

/**
 * A single match in the knockout (single-elimination) phase.
 *
 * <p>Matches are positioned by their {@link #round} and {@link #slot} (0-based
 * within the round). The winner of {@code slot} advances to {@code slot / 2} of
 * the next round, as the home team when {@code slot} is even and the away team
 * when it is odd — which keeps the bracket tree consistent.
 *
 * <p>Teams may be {@code null} for rounds beyond the first until their feeder
 * matches have been decided. {@link #seed} values mirror the league-phase
 * qualifying position (1 = top seed) and are carried forward with the winner.
 */
@Entity
@Table(name = "knockout_matches",
        uniqueConstraints = @UniqueConstraint(columnNames = {"round", "slot"}))
public class KnockoutMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KnockoutRound round;

    /** Position of this match within its round (0-based). */
    @Column(nullable = false)
    private int slot;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    /** League-phase seed of the home team (1 = top seed), or null if undecided. */
    private Integer homeSeed;

    /** League-phase seed of the away team (1 = top seed), or null if undecided. */
    private Integer awaySeed;

    private Integer homeScore;

    private Integer awayScore;

    @Column(nullable = false)
    private boolean played = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winner_team_id")
    private Team winner;

    public KnockoutMatch() {
    }

    public KnockoutMatch(KnockoutRound round, int slot) {
        this.round = round;
        this.slot = slot;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KnockoutRound getRound() {
        return round;
    }

    public void setRound(KnockoutRound round) {
        this.round = round;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(Team homeTeam) {
        this.homeTeam = homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(Team awayTeam) {
        this.awayTeam = awayTeam;
    }

    public Integer getHomeSeed() {
        return homeSeed;
    }

    public void setHomeSeed(Integer homeSeed) {
        this.homeSeed = homeSeed;
    }

    public Integer getAwaySeed() {
        return awaySeed;
    }

    public void setAwaySeed(Integer awaySeed) {
        this.awaySeed = awaySeed;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    public boolean isPlayed() {
        return played;
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    public Team getWinner() {
        return winner;
    }

    public void setWinner(Team winner) {
        this.winner = winner;
    }
}
