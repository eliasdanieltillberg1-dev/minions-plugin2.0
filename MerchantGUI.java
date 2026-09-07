package com.rolleco.minions;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Builds the /merchant GUI: a border of glass panes, the rotating stock slots, and a paid refresh button. */
public class MerchantGUI {

    public static final int SIZE = 27;
    public static final int[] STOCK_SLOTS = {10, 11, 12, 13, 14};
    public static final int REFRESH_SLOT = 22;

    private final MinionsConfig cfg;
    private final MinionItemFactory itemFactory;

    public MerchantGUI(MinionsConfig cfg, MinionItemFactory itemFactory) {
        this.cfg = cfg;
        this.itemFactory = itemFactory;
    }

    public Inventory build(Player player, MerchantStock stock) {
        Inventory inv = Bukkit.createInventory(new MerchantHolder(), SIZE, Util.color("&8Minion Merchant"));

        ItemStack border = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) inv.setItem(i, border);

        List<MerchantOffer> offers = stock.slots;
        for (int i = 0; i < STOCK_SLOTS.length; i++) {
            if (i >= offers.size()) continue;
            MerchantOffer offer = offers.get(i);
            MinionType type = cfg.minionTypesById.get(offer.typeId());
            if (type == null) continue;
            ItemStack item = itemFactory.buildMinionItem(cfg, type, offer.tier());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>(meta.getLore() == null ? List.of() : meta.getLore());
                lore.add("");
                lore.add(Util.color("&8Cost &7» &e" + cfg.cost(offer.tier()) + " Credits"));
                lore.add(Util.color("&7Click to buy."));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(STOCK_SLOTS[i], item);
        }

        long remainingSeconds = Math.max(0, cfg.restockSeconds - (System.currentTimeMillis() - stock.lastRestock) / 1000L);
        ItemStack refresh = namedItem(Material.EMERALD, Util.color("&a&lRefresh Stock"));
        ItemMeta refreshMeta = refresh.getItemMeta();
        if (refreshMeta != null) {
            refreshMeta.setLore(List.of(
                    Util.color("&7Cost &7» &e" + cfg.refreshCost + " Credits"),
                    Util.color("&7Auto-restocks in &f" + remainingSeconds + "s&7."),
                    "",
                    Util.color("&7Click to reroll your stock now.")
            ));
            refresh.setItemMeta(refreshMeta);
        }
        inv.setItem(REFRESH_SLOT, refresh);

        return inv;
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
