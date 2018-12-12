package com.wurmonline.server.items;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.WurmId;
import com.wurmonline.server.bodys.Body;
import com.wurmonline.server.epic.HexMap;
import mod.wurmunlimited.WurmObjectsFactory;

import java.io.IOException;

public class ItemFactory {

    public static Item createItem(int templateId, float qualityLevel, String creator) {
        return createItem(templateId, qualityLevel, (byte)0, (byte)0, -10, creator);
    }

    public static Item createItem(int templateId, float qualityLevel, byte material, byte rarity, String creator) {
        return createItem(templateId, qualityLevel, material, rarity, -10, creator);
    }

    public static Item createItem(int templateId, float ql, byte material, byte rarity, long bridgeId, String creator) {
        Item item = WurmObjectsFactory.getCurrent().createNewItem(templateId);
        item.setQualityLevel(ql);
        item.setMaterial(material);
        return item;
    }

    public static Item createInventory(long ownerId, short s, float f) {
        return WurmObjectsFactory.getCurrent().createNewItem(0);
    }

    public static Item createBodyPart(Body body, short place, int templateId, String name, float qualityLevel) throws FailedException, NoSuchTemplateException {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(templateId);
        Object toReturn = null;

        try {
            long wurmId = WurmId.getNextBodyPartId(body.getOwnerId(), (byte)place, WurmId.getType(body.getOwnerId()) == 0);
            if (template.isRecycled) {
                toReturn = Itempool.getRecycledItem(templateId, qualityLevel);
                if (toReturn != null) {
                    ((Item)toReturn).clear(-10L, "", 0.0F, 0.0F, 0.0F, 0.0F, "", name, qualityLevel, template.getMaterial(), (byte)0, -10L);
                    ((Item)toReturn).setPlace(place);
                }
            }

            if (toReturn == null) {
                toReturn = new TempItem(wurmId, place, name, template, qualityLevel, "");
            }

            return (Item)toReturn;
        } catch (IOException var9) {
            throw new FailedException(var9);
        }
    }

    public static void decay(long itemId, DbStrings dbStrings) {
        WurmObjectsFactory.getCurrent().removeItem(itemId);
        Items.removeItem(itemId);
    }

    public static String generateName(ItemTemplate template, byte material) {
        String name = template.sizeString + template.getName();
        if (template.getTemplateId() == 683) {
            name = HexMap.generateFirstName() + " " + HexMap.generateSecondName();
        }

        if (template.unique) {
            name = template.getName();
        }

        return name;
    }
}
