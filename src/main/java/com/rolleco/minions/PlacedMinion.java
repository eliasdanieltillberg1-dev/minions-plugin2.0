package com.rolleco.minions;

import java.util.UUID;

/**
 * One minion a player has physically placed in the world. This is the
 * persisted record (see PlacedMinionStore); displayEntityId/interactionEntityId
 * are runtime-only bookkeeping for whichever live entities currently
 * represent it (set by MinionManager after spawning, not written to disk).
 */
public class PlacedMinion {
    public final UUID id;
    public final UUID owner;
    public final String typeId;
    public final int tier;
    public final String world;
    public final double x, y, z;

    public UUID displayEntityId;
    public UUID interactionEntityId;

    public PlacedMinion(UUID id, UUID owner, String typeId, int tier, String world, double x, double y, double z) {
        this.id = id;
        this.owner = owner;
        this.typeId = typeId;
        this.tier = tier;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
