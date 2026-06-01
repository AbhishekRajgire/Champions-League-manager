package com.ucl.repository;

import com.ucl.model.KnockoutMatch;
import com.ucl.model.KnockoutRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnockoutMatchRepository extends JpaRepository<KnockoutMatch, Long> {

    Optional<KnockoutMatch> findByRoundAndSlot(KnockoutRound round, int slot);

    List<KnockoutMatch> findByRoundOrderBySlotAsc(KnockoutRound round);
}
