package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PlaceNpcMenu implements ModAction, ActionPerformer, BehaviourProvider {
    private static final Logger logger = Logger.getLogger(PlaceNpcMenu.class.getName());
    private static final List<ActionEntry> actionEntries = new ArrayList<>();
    private static final Map<String, NpcMenuEntry> npcs = new HashMap<>();
    private static final short actionId = (short)ModActions.getNextActionId();
    private static PlaceNpcMenu menu = null;

    private PlaceNpcMenu() {}

    public static PlaceNpcMenu registerAction() {
        if (menu == null) {
            menu = new PlaceNpcMenu();
            ActionEntry actionEntry = new ActionEntry(actionId, "Place Npc", "placing npcs");
            ModActions.registerAction(actionEntry);
            ModActions.registerAction(menu);
        }

        return menu;
    }

    static void addNpcAction(NpcMenuEntry entry) {
        npcs.put(entry.getName(), entry);
        if (actionEntries.size() == 0)
            actionEntries.add(null);
        actionEntries.set(0, new ActionEntry((short)-npcs.size(),
                "Place Npc",
                "placing npcs",
                ItemBehaviour.emptyIntArr));
        actionEntries.add(new ActionEntry(actionId, entry.getName(), "placing " + entry.getName()));
    }

    static List<ActionEntry> getBehaviours(Creature performer, Item item) {
        if (item.isWand() && performer.getPower() >= 2) {
            return actionEntries;
        }

        return null;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, int tilex, int tiley, boolean onSurface, int tile) {
        return getBehaviours(performer, item);
    }

    // Seems to be for inside mines.  What is the point of onSurface in other methods then?
    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, int tilex, int tiley, boolean onSurface, int tile, int dir) {
        return getBehaviours(performer, item);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, boolean onSurface, Floor floor) {
        return getBehaviours(performer, item);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item item, boolean onSurface, BridgePart bridgePart) {
        return getBehaviours(performer, item);
    }

    private boolean doAction(Action action, short num, Creature performer, Item source, VolaTile tile, int floorLevel) {
        if (num == actionId && source.isWand() && performer.getPower() >= 2) {
            NpcMenuEntry entry = npcs.get(action.getActionString());

            if (entry == null)
                return true;

            return entry.doAction(action, num, performer, source, tile, floorLevel);
        }

        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int floorLevel, int tile, short num, float counter) {
        VolaTile volaTile = Zones.getOrCreateTile(tilex, tiley, onSurface);
        if (volaTile == null) {
            performer.getCommunicator().sendAlertServerMessage("You could not be located.");
            logger.warning("Could not find or create tile (" + tile + ") at " + tilex + " - " + tiley + " surfaced=" + onSurface);
            return true;
        }

        if (!onSurface)
            floorLevel = 0;

        return doAction(action, num, performer, source, volaTile, floorLevel);
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, boolean onSurface, Floor floor, int encodedTile, short num, float counter) {
        return doAction(action, num, performer, source, floor.getTile(), floor.getFloorLevel());
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
        return doAction(action, num, performer, source, bridgePart.getTile(), bridgePart.getFloorLevel());
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
