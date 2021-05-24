package com.wurmonline.server.behaviours;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.NoSuchTemplateException;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CopyPriceListAction {
    private static final Logger logger = Logger.getLogger(CopyPriceListAction.class.getName());
    static final List<ActionEntry> actionEntries = new ArrayList<>();
    public static int contractTemplateId;

    static List<ActionEntry> getBehavioursFor(Creature performer, Item contract, Creature buyer) {
        if (contract.getTemplateId() == contractTemplateId && isBuyer(buyer) && contract.getData() != buyer.getWurmId()) {
            Shop shop = buyer.getShop();
            if (shop != null && performer.getWurmId() == shop.getOwnerId()) {
                List<ActionEntry> toReturn = new ArrayList<>();
                toReturn.add(new ActionEntry((short)-2, "Copy Price List", "copying price list", ItemBehaviour.emptyIntArr));
                toReturn.addAll(actionEntries);
                return toReturn;
            }
        }
        return null;
    }

    static boolean action(Creature performer, long source, Creature buyer, boolean copyToTarget) {
        Item contract = Items.getItemOptional(source).orElse(null);
        if (contract != null && contract.getTemplateId() == contractTemplateId && isBuyer(buyer) && contract.getData() != buyer.getWurmId()) {
            Shop shop = buyer.getShop();
            if (shop == null) {
                performer.getCommunicator().sendNormalServerMessage(buyer.getName() + " does not have a shop.");
                logger.warning("Buyer confirmed with isBuyer but does not have a shop.");
                return true;
            }
            if (performer.getWurmId() != shop.getOwnerId()) {
                performer.getCommunicator().sendNormalServerMessage("You need to be the owner of both buyers to copy a price list.");
                return true;
            }

            Creature contractBuyer = Creatures.getInstance().getCreatureOrNull(contract.getData());
            if (contractBuyer != null) {
                Shop otherShop = contractBuyer.getShop();
                if (otherShop == null || performer.getWurmId() != otherShop.getOwnerId()) {
                    performer.getCommunicator().sendNormalServerMessage("You need to be the owner of both buyers copy a price list.");
                    return true;
                }

                Creature fromBuyer;
                Creature toBuyer;
                if (copyToTarget) {
                    fromBuyer = contractBuyer;
                    toBuyer = buyer;
                } else {
                    fromBuyer = buyer;
                    toBuyer = contractBuyer;
                }

                for (Item toReplace : toBuyer.getInventory().getItems()) {
                    if (PriceList.isPriceList(toReplace)) {
                        for (Item toCopy : fromBuyer.getInventory().getItemsAsArray()) {
                            if (PriceList.isPriceList(toCopy)) {
                                try {
                                    Item newPriceList = PriceList.copy(toCopy);
                                    if (newPriceList == null)
                                        throw new FailedException("Price List was not copied, got null instead.");
                                    toBuyer.getInventory().insertItem(newPriceList, true);
                                } catch (NoSuchTemplateException | FailedException e) {
                                    performer.getCommunicator().sendNormalServerMessage("The buyer looks confused and cannot read the handwriting.");
                                    logger.warning("Price List was not copied.  Reason follows:");
                                    e.printStackTrace();
                                    return true;
                                }
                                Items.destroyItem(toReplace.getWurmId());
                                performer.getCommunicator().sendNormalServerMessage(toBuyer.getName() + " successfully copied " + fromBuyer.getName() + "'s price list.");
                                return true;
                            }
                        }
                    }
                }
                logger.warning(contractBuyer.getName() + " may be missing a Price List.");
            }
            performer.getCommunicator().sendNormalServerMessage(buyer.getName() + " does not know who you want " +
                                                    (buyer.getSex() == (byte)0 ? "him" : "her") + " to talk to.");
            return true;
        }
        return false;
    }

    static boolean isBuyer(Creature maybeBuyer) {
        return maybeBuyer.getName().startsWith("Buyer_") && maybeBuyer.getTemplate().id == CreatureTemplateIds.SALESMAN_CID;
    }
}
