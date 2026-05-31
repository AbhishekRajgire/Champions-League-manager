package com.ucl.model;

import jakarta.persistence.*;

@Entity
@Table(name = "teams", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Country / association the club belongs to, e.g. "England", "Spain". */
    @Column(nullable = false)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Pot pot;

    /** Optional crest / logo URL for display. */
    @Column(length = 512)
    private String logoUrl;

    public Team() {
    }

    public Team(String name, String country, Pot pot) {
        this.name = name;
        this.country = country;
        this.pot = pot;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Pot getPot() {
        return pot;
    }

    public void setPot(Pot pot) {
        this.pot = pot;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
