package com.wurmonline.server.items;

import com.wurmonline.server.creatures.Creature;

public class ItemsPackageFactory {

    public static Item getTempItem(long id) {
        Item item = new TempItem();
        item.id = id;
        item.setPosX(1);
        return item;
    }

    public static void removeItem(Creature creature, Item item) {
        creature.getInventory().removeItem(item);
    }

    public static void removeItemFrom(Item parent, Item item) {
        parent.removeItem(item);
    }
}
