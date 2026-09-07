package com.rolleco.minions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * YAML-backed persistence for every currently-placed minion
 * (placed-minions.yml). This is the source of truth for what's placed
 * where - MinionManager keeps its own in-memory copy for live entity
 * bookkeeping, but mirrors every add/remove back here immediately so a
 * server restart (or /reload) can respawn everything exactly as it was.
 */
public class PlacedMinionStore {

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, PlacedMinion> minions = new HashMap<>();

    public PlacedMinionStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "placed-minions.yml");
    }

    public void load() {
        minions.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("minions");
        if (section == null) return;
        for (String idStr : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(idStr);
            if (s == null) continue;
            try {
                UUID id = UUID.fromString(idStr);
                UUID owner = UUID.fromString(s.getString("owner"));
                String type = s.getString("type");
                int tier = s.getInt("tier");
                String world = s.getString("world");
                double x = s.getDouble("x");
                double y = s.getDouble("y");
                double z = s.getDouble("z");
                minions.put(id, new PlacedMinion(id, owner, type, tier, world, x, y, z));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Minions] Skipping malformed placed-minions.yml entry '" + idStr + "'", e);
            }
        }
    }

    public void saveAll() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlacedMinion m : minions.values()) {
            String base = "minions." + m.id + ".";
            yaml.set(base + "owner", m.owner.toString());
            yaml.set(base + "type", m.typeId);
            yaml.set(base + "tier", m.tier);
            yaml.set(base + "world", m.world);
            yaml.set(base + "x", m.x);
            yaml.set(base + "y", m.y);
            yaml.set(base + "z", m.z);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Minions] Failed to save placed-minions.yml", e);
        }
    }

    public void add(PlacedMinion minion) {
        minions.put(minion.id, minion);
        saveAll();
    }

    public void remove(UUID id) {
        minions.remove(id);
        saveAll();
    }

    public Collection<PlacedMinion> getAll() {
        return new ArrayList<>(minions.values());
    }
}
