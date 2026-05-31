package com.ucl.repository;

import com.ucl.model.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    List<Fixture> findAllByOrderByMatchdayAscIdAsc();

    List<Fixture> findByMatchdayOrderByIdAsc(int matchday);

    long countByPlayedTrue();
}
