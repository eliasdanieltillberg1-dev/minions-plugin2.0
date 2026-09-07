package com.rolleco.minions;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Handles clicks inside the /merchant GUI: buying a stock slot, or paying to refresh the whole stock. */
public class GuiListener implements Listener {

    private final MinionsPlugin plugin;

    public GuiListener(MinionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MerchantHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();

        if (slot == MerchantGUI.REFRESH_SLOT) {
            handleRefresh(player);
            return;
        }

        for (int i = 0; i < MerchantGUI.STOCK_SLOTS.length; i++) {
            if (MerchantGUI.STOCK_SLOTS[i] == slot) {
                handleBuy(player, i);
                return;
            }
        }
    }

    private void handleRefresh(Player player) {
        SkriptBridge bridge = plugin.getSkriptBridge();
        MinionsConfig cfg = plugin.getMinionsConfig();
        double credits = bridge.getCredits(player.getUniqueId());
        if (credits < cfg.refreshCost) {
            player.sendMessage(Util.color("&cYou need " + cfg.refreshCost + " Credits to refresh your stock."));
            return;
        }
        bridge.setCredits(player.getUniqueId(), credits - cfg.refreshCost);
        MerchantStock fresh = plugin.getStockService().forceRefresh(player);
        player.sendMessage(Util.color("&aYour stock has been refreshed for " + cfg.refreshCost + " Credits."));
        player.openInventory(plugin.getMerchantGUI().build(player, fresh));
    }

    private void handleBuy(Player player, int slotIndex) {
        MinionsConfig cfg = plugin.getMinionsConfig();
        MerchantStock stock = plugin.getStockService().getStock(player);
        if (slotIndex >= stock.slots.size()) return;
        MerchantOffer offer = stock.slots.get(slotIndex);
        MinionType type = cfg.minionTypesById.get(offer.typeId());
        if (type == null) return;

        SkriptBridge bridge = plugin.getSkriptBridge();
        int cost = cfg.cost(offer.tier());
        double credits = bridge.getCredits(player.getUniqueId());
        if (credits < cost) {
            player.sendMessage(Util.color("&cYou need " + cost + " Credits to buy this minion."));
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Util.color("&cYour inventory is full."));
            return;
        }

        bridge.setCredits(player.getUniqueId(), credits - cost);
        ItemStack item = plugin.getItemFactory().buildMinionItem(cfg, type, offer.tier());
        player.getInventory().addItem(item);
        player.sendMessage(Util.color("&aBought a " + type.colorCode() + type.displayName() + " &aminion (Tier " + offer.tier() + ") for " + cost + " Credits."));
    }
}
