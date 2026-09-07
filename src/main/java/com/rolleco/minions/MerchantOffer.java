package com.rolleco.minions;

/** One rolled merchant stock slot: a minion type at a specific tier, offered for its config-defined Credits cost. */
public record MerchantOffer(String typeId, int tier) {
}
