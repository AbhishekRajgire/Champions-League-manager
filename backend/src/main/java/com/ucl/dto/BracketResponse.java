package com.ucl.dto;

import java.util.List;

/**
 * The full knockout bracket as a tree: each round holds its matches, and each
 * match holds its two teams, scores and winner. {@code champion} is populated
 * once the final has been played.
 */
public record BracketResponse(
        String champion,
        List<RoundNode> rounds
) {
    public record RoundNode(
            String round,
            String label,
            List<KnockoutMatchResponse> matches
    ) {
    }
}
