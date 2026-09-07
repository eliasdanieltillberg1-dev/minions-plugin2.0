package com.rolleco.minions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Per-player rotating merchant stock. Each player has their OWN stock,
 * never shared - matches the original request ("every player should
 * have their own personal stock and it should not be shared"). Stock
 * auto-restocks after config.restockSeconds (checked lazily whenever a
 * player opens /merchant), or instantly via the paid refresh button.
 * Persisted to merchant-stock.yml so a restart doesn't hand everyone a
 * brand new roll.
 */
public class MerchantStockService {

    private final Plugin plugin;
    private final MinionsConfig cfg;
    private final File file;
    private final Map<UUID, MerchantStock> stocks = new HashMap<>();

    public MerchantStockService(Plugin plugin, MinionsConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.file = new File(plugin.getDataFolder(), "merchant-stock.yml");
    }

    public void load() {
        stocks.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) return;
        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = section.getConfigurationSection(uuidStr);
                if (playerSection == null) continue;
                long lastRestock = playerSection.getLong("lastRestock", 0L);
                List<MerchantOffer> slots = new ArrayList<>();
                ConfigurationSection slotsSection = playerSection.getConfigurationSection("slots");
                if (slotsSection != null) {
                    for (String key : slotsSection.getKeys(false)) {
                        ConfigurationSection s = slotsSection.getConfigurationSection(key);
                        if (s == null) continue;
                        slots.add(new MerchantOffer(s.getString("type"), s.getInt("tier")));
                    }
                }
                stocks.put(uuid, new MerchantStock(slots, lastRestock));
            } catch (IllegalArgumentException ignored) {
                // malformed uuid key, skip
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, MerchantStock> entry : stocks.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            yaml.set(base + "lastRestock", entry.getValue().lastRestock);
            List<MerchantOffer> slots = entry.getValue().slots;
            for (int i = 0; i < slots.size(); i++) {
                yaml.set(base + "slots." + i + ".type", slots.get(i).typeId());
                yaml.set(base + "slots." + i + ".tier", slots.get(i).tier());
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Minions] Failed to save merchant-stock.yml", e);
        }
    }

    /** Returns this player's current stock, rolling a fresh one if they've never had one or their timer expired. */
    public MerchantStock getStock(Player player) {
        UUID uuid = player.getUniqueId();
        MerchantStock stock = stocks.get(uuid);
        long now = System.currentTimeMillis();
        if (stock == null) {
            stock = rollNewStock(now);
            stocks.put(uuid, stock);
            return stock;
        }
        long ageSeconds = (now - stock.lastRestock) / 1000L;
        if (ageSeconds >= cfg.restockSeconds) {
            stock.slots = rollNewStock(now).slots;
            stock.lastRestock = now;
        }
        return stock;
    }

    /** Forces an immediate reroll (used by the paid /merchant refresh). */
    public MerchantStock forceRefresh(Player player) {
        MerchantStock fresh = rollNewStock(System.currentTimeMillis());
        stocks.put(player.getUniqueId(), fresh);
        return fresh;
    }

    private MerchantStock rollNewStock(long now) {
        List<MerchantOffer> slots = new ArrayList<>();
        for (int i = 0; i < cfg.stockSlots; i++) {
            slots.add(new MerchantOffer(rollType(), rollTier()));
        }
        return new MerchantStock(slots, now);
    }

    private String rollType() {
        if (cfg.minionTypes.isEmpty()) return "FARMER";
        int index = ThreadLocalRandom.current().nextInt(cfg.minionTypes.size());
        return cfg.minionTypes.get(index).id();
    }

    /**
     * Weighted tier roll: checks rarest-to-common (matching the codebase's
     * established "independent rarest-first checks with fallback" idiom -
     * see b3_rollElite / mob talisman rolls - rather than a single
     * normalized distribution). Tier N has a 1-in-odds(N) chance; if every
     * rarer roll misses, falls back to tier 1 (50%).
     */
    private int rollTier() {
        for (int tier = 30; tier >= 2; tier--) {
            int odds = Math.max(1, cfg.odds(tier));
            if (ThreadLocalRandom.current().nextInt(odds) == 0) {
                return tier;
            }
        }
        return 1;
    }
}
