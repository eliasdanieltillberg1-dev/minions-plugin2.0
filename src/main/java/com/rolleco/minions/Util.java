package com.rolleco.minions;

import org.bukkit.ChatColor;

public final class Util {
    private Util() {}

    /** Converts "&a"-style codes (same convention the Skript scripts use) to real color codes. */
    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }
}
