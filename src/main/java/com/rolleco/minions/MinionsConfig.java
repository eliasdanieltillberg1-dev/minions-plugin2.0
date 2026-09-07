package com.rolleco.minions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and holds everything from config.yml: settings, the 30-tier
 * income/cost/odds tables, the 6 rarity bands, and the minion type
 * registry (in insertion order, matching config.yml's order - order
 * only matters for "pick a random type", never for identity - see
 * MinionType's header comment).
 */
public class MinionsConfig {

    public int incomeIntervalSeconds;
    public int restockSeconds;
    public int refreshCost;
    public int minStockQuantity;
    public int maxStockQuantity;
    public int stockSlots;
    public int maxActiveMinions;
    public String merchantPermission;

    public final int[] tierIncome = new int[31]; // 1-indexed, [0] unused
    public final int[] tierCost = new int[31];
    public final int[] tierOdds = new int[31];

    public final List<RarityBand> rarityBands = new ArrayList<>();
    public final List<MinionType> minionTypes = new ArrayList<>();
    public final Map<String, MinionType> minionTypesById = new LinkedHashMap<>();

    public MinionsConfig(FileConfiguration cfg) {
        reload(cfg);
    }

    /**
     * Re-reads everything from config.yml into this SAME instance (clearing
     * the lists/maps first) rather than building a new object - every other
     * class (MinionManager, MerchantStockService, MerchantGUI...) is handed
     * this instance once at startup and keeps that reference, so /minionsreload
     * only actually takes effect if we mutate in place instead of swapping it.
     */
    public void reload(FileConfiguration cfg) {
        rarityBands.clear();
        minionTypes.clear();
        minionTypesById.clear();

        incomeIntervalSeconds = cfg.getInt("income-interval-seconds", 60);
        restockSeconds = cfg.getInt("restock-seconds", 300);
        refreshCost = cfg.getInt("refresh-cost", 10);
        minStockQuantity = cfg.getInt("min-stock-quantity", 1);
        maxStockQuantity = cfg.getInt("max-stock-quantity", 5);
        stockSlots = cfg.getInt("stock-slots", 5);
        maxActiveMinions = cfg.getInt("max-active-minions", 10);
        merchantPermission = cfg.getString("merchant-permission", "rolleco.admin");

        List<Integer> income = cfg.getIntegerList("tier-income");
        List<Integer> cost = cfg.getIntegerList("tier-cost");
        List<Integer> odds = cfg.getIntegerList("tier-odds");
        for (int tier = 1; tier <= 30; tier++) {
            tierIncome[tier] = income.size() >= tier ? income.get(tier - 1) : 0;
            tierCost[tier] = cost.size() >= tier ? cost.get(tier - 1) : 0;
            tierOdds[tier] = odds.size() >= tier ? odds.get(tier - 1) : 1;
        }

        ConfigurationSection bandsSection = cfg.getConfigurationSection("rarity-bands");
        if (bandsSection != null) {
            for (String key : bandsSection.getKeys(false)) {
                ConfigurationSection band = bandsSection.getConfigurationSection(key);
                if (band == null) continue;
                rarityBands.add(new RarityBand(
                        key,
                        band.getInt("max-tier", 30),
                        band.getString("color", "&f"),
                        band.getDouble("scale", 1.0)
                ));
            }
            rarityBands.sort((a, b) -> Integer.compare(a.maxTier(), b.maxTier()));
        }

        ConfigurationSection typesSection = cfg.getConfigurationSection("minion-types");
        if (typesSection != null) {
            for (String id : typesSection.getKeys(false)) {
                ConfigurationSection t = typesSection.getConfigurationSection(id);
                if (t == null) continue;
                MinionType type = new MinionType(
                        id,
                        t.getString("name", id),
                        t.getString("color", "&f"),
                        t.getString("texture", "")
                );
                minionTypes.add(type);
                minionTypesById.put(id, type);
            }
        }
    }

    public RarityBand bandForTier(int tier) {
        for (RarityBand band : rarityBands) {
            if (tier <= band.maxTier()) {
                return band;
            }
        }
        // fall back to the last (rarest) band rather than throwing, in
        // case config.yml's rarity-bands don't actually reach tier 30
        return rarityBands.isEmpty() ? new RarityBand("COMMON", 30, "&f", 1.0)
                : rarityBands.get(rarityBands.size() - 1);
    }

    public int income(int tier) {
        return tierIncome[clamp(tier)];
    }

    public int cost(int tier) {
        return tierCost[clamp(tier)];
    }

    public int odds(int tier) {
        return tierOdds[clamp(tier)];
    }

    private int clamp(int tier) {
        if (tier < 1) return 1;
        if (tier > 30) return 30;
        return tier;
    }
}
