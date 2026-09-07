package com.rolleco.minions;

/** One rarity band (a contiguous range of tiers sharing a color + display scale). */
public record RarityBand(String name, int maxTier, String colorCode, double scale) {
}
