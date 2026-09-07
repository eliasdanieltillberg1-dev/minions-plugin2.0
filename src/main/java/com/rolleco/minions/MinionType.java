package com.rolleco.minions;

/**
 * One cosmetic minion type. Purely visual - every type produces
 * identical income at a given tier (see TierTable). {@code id} is the
 * STABLE key stored on items/entities (PDC) - never re-derive identity
 * from list position, so re-ordering config's minion-types block can
 * never relabel a minion a player already owns or has placed.
 */
public record MinionType(String id, String displayName, String colorCode, String texture) {

    public boolean hasTexture() {
        return texture != null && !texture.isBlank();
    }
}
