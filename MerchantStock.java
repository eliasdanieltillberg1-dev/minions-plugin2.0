package com.rolleco.minions;

import java.util.List;

/** One player's personal merchant stock - never shared with anyone else. */
public class MerchantStock {
    public List<MerchantOffer> slots;
    public long lastRestock;

    public MerchantStock(List<MerchantOffer> slots, long lastRestock) {
        this.slots = slots;
        this.lastRestock = lastRestock;
    }
}
