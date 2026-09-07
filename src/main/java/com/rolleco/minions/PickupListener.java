package com.rolleco.minions;

import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/** Shift-right-click a placed minion to pick it back up; also protects every placed minion's entities from all damage. */
public class PickupListener implements Listener {

    private final MinionsPlugin plugin;

    public PickupListener(MinionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) return;
        String idStr = interaction.getPersistentDataContainer().get(plugin.getMinionManager().minionIdKey, PersistentDataType.STRING);
        if (idStr == null) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (!player.isSneaking()) {
            player.sendMessage(Util.color("&7Shift-right-click a minion to pick it back up."));
            return;
        }

        UUID minionId;
        try {
            minionId = UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        PlacedMinion minion = plugin.getMinionManager().getById(minionId);
        if (minion == null) return;

        if (!minion.owner.equals(player.getUniqueId()) && !player.hasPermission(plugin.getMinionsConfig().merchantPermission)) {
            player.sendMessage(Util.color("&cThat isn't your minion."));
            return;
        }

        ItemStack item = plugin.getMinionManager().pickup(minion);
        if (item == null) return;

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
        player.sendMessage(Util.color("&aPicked up your minion."));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        String idStr = event.getEntity().getPersistentDataContainer().get(plugin.getMinionManager().minionIdKey, PersistentDataType.STRING);
        if (idStr != null) {
            event.setCancelled(true);
        }
    }
}
