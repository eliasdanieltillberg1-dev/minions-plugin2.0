package com.rolleco.minions;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the PLAYER_HEAD ItemStacks minions are represented as (both the
 * item you hold/buy and the item shown on the placed ItemDisplay), applies
 * a custom texture via a PlayerProfile when one is configured, and tags
 * every item with its type+tier via PersistentDataContainer so it can be
 * identified later without parsing lore text.
 */
public class MinionItemFactory {

    private final Plugin plugin;
    private final NamespacedKey typeKey;
    private final NamespacedKey tierKey;

    public MinionItemFactory(Plugin plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "minion_type");
        this.tierKey = new NamespacedKey(plugin, "minion_tier");
    }

    /** A plain head wearing this type's texture (or a default Steve head if none is configured). */
    public ItemStack buildHead(MinionType type) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            if (type.hasTexture()) {
                try {
                    PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(("minion:" + type.id()).getBytes()), "Minion");
                    profile.setProperty(new ProfileProperty("textures", type.texture()));
                    meta.setOwnerProfile(profile);
                } catch (Throwable t) {
                    plugin.getLogger().warning("[Minions] Invalid texture value for minion type '" + type.id() + "' - falling back to a default head. Error: " + t);
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** The full item: head + custom name/lore + PDC type/tier tags. */
    public ItemStack buildMinionItem(MinionsConfig cfg, MinionType type, int tier) {
        ItemStack item = buildHead(type);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        RarityBand band = cfg.bandForTier(tier);
        String color = Util.color(type.colorCode());
        String rarityColor = Util.color(band.colorCode());

        meta.setDisplayName(color + type.displayName() + " Minion " + Util.color("&8[") + rarityColor + band.name() + Util.color("&8]"));

        List<String> lore = new ArrayList<>();
        lore.add(Util.color("&7Placeable minion"));
        lore.add("");
        lore.add(Util.color("&8Tier &7» &f" + tier + "&7/30"));
        lore.add(Util.color("&8Produces &7» &a" + cfg.income(tier) + " Money &7/ " + cfg.incomeIntervalSeconds + "s"));
        lore.add("");
        lore.add(Util.color("&7Right-click a block to place it."));
        lore.add(Util.color("&7Shift-right-click a placed minion"));
        lore.add(Util.color("&7to pick it back up."));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.id());
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isMinionItem(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING);
    }

    public String getType(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
    }

    public int getTier(ItemStack item) {
        Integer tier = item.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
        return tier == null ? 1 : tier;
    }

    public NamespacedKey typeKey() { return typeKey; }
    public NamespacedKey tierKey() { return tierKey; }
}
