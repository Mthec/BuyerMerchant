package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.zones.VolaTile;

public interface NpcMenuEntry {
    String getName();

    boolean doAction(Action action, short num, Creature performer, Item source, VolaTile tile, int floorLevel);
}
