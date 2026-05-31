package com.ucl.repository;

import com.ucl.model.Pot;
import com.ucl.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByPot(Pot pot);

    boolean existsByNameIgnoreCase(String name);

    long countByPot(Pot pot);
}
