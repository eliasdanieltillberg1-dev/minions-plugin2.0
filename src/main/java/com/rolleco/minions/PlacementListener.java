package com.rolleco.minions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Right-click a block with a minion head in hand to place it. */
public class PlacementListener implements Listener {

    private final MinionsPlugin plugin;

    public PlacementListener(MinionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!plugin.getItemFactory().isMinionItem(item)) return;

        // Always cancel: this item is a PLAYER_HEAD and would otherwise place
        // as a vanilla skull block - we handle placement ourselves instead.
        event.setCancelled(true);

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        int max = plugin.getMinionsConfig().maxActiveMinions;
        if (plugin.getMinionManager().countActive(player.getUniqueId()) >= max) {
            player.sendMessage(Util.color("&cYou can only have " + max + " minions placed at once."));
            return;
        }

        String typeId = plugin.getItemFactory().getType(item);
        int tier = plugin.getItemFactory().getTier(item);
        MinionType type = plugin.getMinionsConfig().minionTypesById.get(typeId);
        if (type == null) {
            player.sendMessage(Util.color("&cThat minion type no longer exists."));
            return;
        }

        Location placeAt = clicked.getLocation().add(0.5, 1.0, 0.5);
        plugin.getMinionManager().place(player.getUniqueId(), typeId, tier, placeAt);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(Util.color("&aPlaced your " + type.colorCode() + type.displayName() + " &aminion (Tier " + tier + ")."));
    }
}
