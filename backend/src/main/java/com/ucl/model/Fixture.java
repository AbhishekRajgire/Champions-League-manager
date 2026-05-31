package com.ucl.model;

import jakarta.persistence.*;

/**
 * A single league-phase match between two teams.
 * The home team's pot and away team's pot are stored for quick reference.
 */
@Entity
@Table(name = "fixtures")
public class Fixture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    /** League-phase round / matchday (1..N). */
    @Column(nullable = false)
    private int matchday;

    private Integer homeScore;

    private Integer awayScore;

    @Column(nullable = false)
    private boolean played = false;

    public Fixture() {
    }

    public Fixture(Team homeTeam, Team awayTeam, int matchday) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.matchday = matchday;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getMatchday() {
        return matchday;
    }

    public void setMatchday(int matchday) {
        this.matchday = matchday;
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
}
