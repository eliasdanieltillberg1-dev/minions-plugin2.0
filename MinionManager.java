package com.rolleco.minions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Owns every currently-placed minion: spawning/despawning its paired
 * ItemDisplay (the visible, scalable head) + Interaction (the invisible
 * click hitbox - pure Display entities have no interaction hitbox of
 * their own), the click-to-minion lookup, the per-owner active-count
 * cap, and the income payout heartbeat.
 */
public class MinionManager {

    private final Plugin plugin;
    private final MinionsConfig cfg;
    private final MinionItemFactory itemFactory;
    private final SkriptBridge bridge;
    private final PlacedMinionStore store;
    public final NamespacedKey minionIdKey;

    private final Map<UUID, PlacedMinion> byId = new HashMap<>();
    private final Map<UUID, UUID> entityToMinionId = new HashMap<>();
    private final Map<UUID, List<UUID>> byOwner = new HashMap<>();

    public MinionManager(Plugin plugin, MinionsConfig cfg, MinionItemFactory itemFactory, SkriptBridge bridge, PlacedMinionStore store) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.itemFactory = itemFactory;
        this.bridge = bridge;
        this.store = store;
        this.minionIdKey = new NamespacedKey(plugin, "minion_id");
    }

    public int countActive(UUID owner) {
        List<UUID> list = byOwner.get(owner);
        return list == null ? 0 : list.size();
    }

    public PlacedMinion getById(UUID minionId) {
        return byId.get(minionId);
    }

    /** Places a brand new minion at a location and spawns its entities. Caller must already have checked the active-minion cap and consumed the item. */
    public PlacedMinion place(UUID owner, String typeId, int tier, Location location) {
        UUID id = UUID.randomUUID();
        PlacedMinion minion = new PlacedMinion(id, owner, typeId, tier,
                location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
        register(minion);
        spawnEntities(minion);
        store.add(minion);
        return minion;
    }

    /** Removes a minion's entities and its record entirely, returning a fresh copy of its item (for giving back to the player). */
    public ItemStack pickup(PlacedMinion minion) {
        despawnEntities(minion);
        unregister(minion);
        store.remove(minion.id);
        MinionType type = cfg.minionTypesById.get(minion.typeId);
        if (type == null) return null;
        return itemFactory.buildMinionItem(cfg, type, minion.tier);
    }

    private void register(PlacedMinion minion) {
        byId.put(minion.id, minion);
        byOwner.computeIfAbsent(minion.owner, k -> new ArrayList<>()).add(minion.id);
    }

    private void unregister(PlacedMinion minion) {
        byId.remove(minion.id);
        List<UUID> list = byOwner.get(minion.owner);
        if (list != null) {
            list.remove(minion.id);
            if (list.isEmpty()) byOwner.remove(minion.owner);
        }
        if (minion.displayEntityId != null) entityToMinionId.remove(minion.displayEntityId);
        if (minion.interactionEntityId != null) entityToMinionId.remove(minion.interactionEntityId);
    }

    private void spawnEntities(PlacedMinion minion) {
        World world = Bukkit.getWorld(minion.world);
        if (world == null) {
            plugin.getLogger().warning("[Minions] Could not spawn minion " + minion.id + ": world '" + minion.world + "' is not loaded.");
            return;
        }
        Location loc = new Location(world, minion.x, minion.y, minion.z);
        MinionType type = cfg.minionTypesById.get(minion.typeId);
        RarityBand band = cfg.bandForTier(minion.tier);
        float scale = (float) band.scale();

        ItemStack displayItem = type != null ? itemFactory.buildMinionItem(cfg, type, minion.tier) : new ItemStack(Material.PLAYER_HEAD);

        ItemDisplay display = world.spawn(loc.clone().add(0, 0.05, 0), ItemDisplay.class, e -> {
            e.setItemStack(displayItem);
            e.setBillboard(Display.Billboard.CENTER);
            Transformation t = new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf());
            e.setTransformation(t);
            e.setPersistent(true);
            e.getPersistentDataContainer().set(minionIdKey, PersistentDataType.STRING, minion.id.toString());
        });

        float hitboxWidth = 0.6f + scale * 0.5f;
        float hitboxHeight = 0.8f + scale * 1.0f;
        Interaction interaction = world.spawn(loc.clone().add(0, hitboxHeight / 2.0, 0), Interaction.class, e -> {
            e.setInteractionWidth(hitboxWidth);
            e.setInteractionHeight(hitboxHeight);
            e.setResponsive(true);
            e.setPersistent(true);
            e.getPersistentDataContainer().set(minionIdKey, PersistentDataType.STRING, minion.id.toString());
        });

        minion.displayEntityId = display.getUniqueId();
        minion.interactionEntityId = interaction.getUniqueId();
        entityToMinionId.put(minion.displayEntityId, minion.id);
        entityToMinionId.put(minion.interactionEntityId, minion.id);
    }

    private void despawnEntities(PlacedMinion minion) {
        removeEntity(minion.displayEntityId);
        removeEntity(minion.interactionEntityId);
    }

    private void removeEntity(UUID entityUuid) {
        if (entityUuid == null) return;
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity != null) entity.remove();
    }

    /** Called once on plugin enable: loads persisted minions and respawns their entities. */
    public void loadAllFromStore() {
        for (PlacedMinion minion : store.getAll()) {
            register(minion);
            spawnEntities(minion);
        }
    }

    /** Called once on plugin disable: removes every live entity (they get respawned from disk on the next enable), then persists. */
    public void despawnAllAndSave() {
        for (PlacedMinion minion : new ArrayList<>(byId.values())) {
            despawnEntities(minion);
        }
        store.saveAll();
    }

    /**
     * Defensive cleanup for the unclean-shutdown edge case (crash, kill -9):
     * removes any already-loaded entity tagged with our minion-id key that
     * has no matching record in the store, so a crash can't leave behind
     * duplicate/orphaned heads once the plugin restarts and respawns
     * everything fresh from disk. Call this BEFORE loadAllFromStore().
     * Only catches entities in already-loaded chunks - harmless either way
     * since a normal stop/start never even reaches this path.
     */
    public void cleanupOrphanedEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof ItemDisplay) && !(entity instanceof Interaction)) continue;
                String idStr = entity.getPersistentDataContainer().get(minionIdKey, PersistentDataType.STRING);
                if (idStr == null) continue;
                try {
                    UUID minionId = UUID.fromString(idStr);
                    if (!byId.containsKey(minionId)) {
                        entity.remove();
                    }
                } catch (IllegalArgumentException ignored) {
                    entity.remove();
                }
            }
        }
    }

    /** Starts the repeating income payout task: every placed minion pays its owner via the Skript bridge. */
    public void startIncomeTask() {
        long periodTicks = Math.max(20L, cfg.incomeIntervalSeconds * 20L);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlacedMinion minion : byId.values()) {
                    int amount = cfg.income(minion.tier);
                    if (amount <= 0) continue;
                    try {
                        bridge.addMoney(minion.owner, amount);
                    } catch (Throwable t) {
                        plugin.getLogger().log(Level.WARNING, "[Minions] Failed to pay minion income to " + minion.owner, t);
                    }
                }
            }
        }.runTaskTimer(plugin, periodTicks, periodTicks);
    }
}
