package com.ucl.controller;

import com.ucl.dto.TeamRequest;
import com.ucl.dto.TeamResponse;
import com.ucl.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<TeamResponse> getAll() {
        return teamService.getAllTeams();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(@Valid @RequestBody TeamRequest request) {
        return teamService.createTeam(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeamResponse update(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return teamService.updateTeam(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        teamService.deleteTeam(id);
    }

    @PostMapping("/load-sample")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TeamResponse> loadSample() {
        return teamService.loadSampleTeams();
    }
}
