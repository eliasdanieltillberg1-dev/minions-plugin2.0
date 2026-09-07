package com.rolleco.minions;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Marker holder so GuiListener can recognize a /merchant inventory without relying on its title text. */
public class MerchantHolder implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null; // unused - Bukkit requires an override, but nothing calls this back
    }
}
