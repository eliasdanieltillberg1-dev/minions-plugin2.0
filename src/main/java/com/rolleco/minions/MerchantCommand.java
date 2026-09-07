package com.rolleco.minions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /merchant - op only per the original request (plugin.yml also gates it on the rolleco.admin permission). */
public class MerchantCommand implements CommandExecutor {

    private final MinionsPlugin plugin;

    public MerchantCommand(MinionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Util.color("&cOnly players can use this command."));
            return true;
        }
        if (!player.hasPermission(plugin.getMinionsConfig().merchantPermission)) {
            player.sendMessage(Util.color("&cYou don't have permission to use this."));
            return true;
        }
        MerchantStock stock = plugin.getStockService().getStock(player);
        player.openInventory(plugin.getMerchantGUI().build(player, stock));
        return true;
    }
}
