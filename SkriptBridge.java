package com.rolleco.minions;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reflection bridge into Skript's own global variable storage, so this
 * plugin can share the exact same Credits/Money balances as RollEco.sk
 * instead of keeping a separate economy. This is the standard technique
 * for an external plugin to read/write Skript variables without adding
 * Skript's jar as a compile-time dependency (skript-mirror/skript-reflect
 * aren't required - Variables#getVariable/setVariable are public static
 * methods on Skript's own API).
 *
 * Fails safe: if Skript isn't installed, or its internal class/method
 * signatures ever change, the constructor logs one warning and every
 * subsequent read returns 0 / every write becomes a no-op, rather than
 * throwing and breaking the rest of the plugin.
 */
public class SkriptBridge {
    private final Logger logger;
    private Method getVariable;
    private Method setVariable;
    private boolean available = false;

    public SkriptBridge(Plugin plugin) {
        this.logger = plugin.getLogger();
        try {
            Class<?> variablesClass = Class.forName("ch.njol.skript.variables.Variables");
            getVariable = variablesClass.getMethod("getVariable", String.class, org.bukkit.event.Event.class, boolean.class);
            setVariable = variablesClass.getMethod("setVariable", String.class, Object.class, org.bukkit.event.Event.class, boolean.class);
            available = true;
        } catch (Throwable t) {
            logger.warning("[Minions] Could not hook into Skript's variable storage (Skript not installed, or its internal API changed). "
                    + "Credits/Money reads will return 0 and writes will be ignored until this is fixed. Error: " + t);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public double getCredits(UUID uuid) {
        return getNumber("geco::credits::" + uuid);
    }

    public void setCredits(UUID uuid, double value) {
        setNumber("geco::credits::" + uuid, value);
    }

    public double getMoney(UUID uuid) {
        return getNumber("geco::money::" + uuid);
    }

    public void addMoney(UUID uuid, double amount) {
        setNumber("geco::money::" + uuid, getMoney(uuid) + amount);
    }

    private double getNumber(String name) {
        if (!available) return 0;
        try {
            Object value = getVariable.invoke(null, name, null, false);
            if (value instanceof Number n) return n.doubleValue();
            return 0;
        } catch (Throwable t) {
            logger.warning("[Minions] Failed to read Skript variable {" + name + "}: " + t);
            return 0;
        }
    }

    private void setNumber(String name, double value) {
        if (!available) return;
        try {
            setVariable.invoke(null, name, value, null, false);
        } catch (Throwable t) {
            logger.warning("[Minions] Failed to write Skript variable {" + name + "}: " + t);
        }
    }
}
