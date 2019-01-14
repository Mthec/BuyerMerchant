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
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class CopyPriceListAction implements ModAction, ActionPerformer, BehaviourProvider {
    private static final Logger logger = Logger.getLogger(CopyPriceListAction.class.getName());
    private final short actionId;
    private final ActionEntry actionEntry;
    private final int contractTemplateId;

    public CopyPriceListAction(int contractTemplateId) {
        this.contractTemplateId = contractTemplateId;
        actionId = (short)ModActions.getNextActionId();

        actionEntry = new ActionEntryBuilder(actionId, "Copy Price List", "copying price list",
                new int[] { ActionTypes.ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM,
                            ActionTypes.ACTION_TYPE_QUICK
                }).build();
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item contract, Creature buyer) {
        if (contract.getTemplateId() == contractTemplateId && isBuyer(buyer) && contract.getData() != buyer.getWurmId()) {
            Shop shop = buyer.getShop();
            if (shop != null && performer.getWurmId() == shop.getOwnerId()) {
                return Collections.singletonList(actionEntry);
            }
        }
        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item contract, Creature buyer, short num, float counter) {
        if (num == actionId) {
            if (contract.getTemplateId() == contractTemplateId && isBuyer(buyer) && contract.getData() != buyer.getWurmId()) {
                Shop shop = buyer.getShop();
                if (shop == null) {
                    performer.getCommunicator().sendNormalServerMessage(buyer.getName() + " does not have a shop.");
                    logger.warning("Buyer confirmed with isBuyer but does not have a shop.");
                    return true;
                }
                if (performer.getWurmId() != shop.getOwnerId()) {
                    performer.getCommunicator().sendNormalServerMessage("You need to be the owner of both buyers copy a price list.");
                    return true;
                }

                Creature otherBuyer = Creatures.getInstance().getCreatureOrNull(contract.getData());
                if (otherBuyer != null) {
                    Shop otherShop = otherBuyer.getShop();
                    if (otherShop == null || performer.getWurmId() != otherShop.getOwnerId()) {
                        performer.getCommunicator().sendNormalServerMessage("You need to be the owner of both buyers copy a price list.");
                        return true;
                    }

                    for (Item toReplace : otherBuyer.getInventory().getItems()) {
                        if (PriceList.isPriceList(toReplace)) {
                            for (Item toCopy : buyer.getInventory().getItemsAsArray()) {
                                if (PriceList.isPriceList(toCopy)) {
                                    try {
                                        Item newPriceList = PriceList.copy(toCopy);
                                        if (newPriceList == null)
                                            throw new FailedException("Price List was not copied, got null instead.");
                                        otherBuyer.getInventory().insertItem(newPriceList, true);
                                    } catch (NoSuchTemplateException | FailedException e) {
                                        performer.getCommunicator().sendNormalServerMessage("The buyer looks confused and cannot read the handwriting.");
                                        logger.warning("Price List was not copied.  Reason follows:");
                                        e.printStackTrace();
                                        return true;
                                    }
                                    Items.destroyItem(toReplace.getWurmId());
                                    performer.getCommunicator().sendNormalServerMessage(otherBuyer.getName() + " successfully copied " + buyer.getName() + "'s price list.");
                                    return true;
                                }
                            }
                        }
                    }
                    logger.warning(otherBuyer.getName() + " may be missing a Price List.");
                }
                performer.getCommunicator().sendNormalServerMessage(buyer.getName() + " does not know who you want " +
                                                        (buyer.getSex() == (byte)0 ? "him" : "her") + " to talk to.");
                return true;
            }
        }
        return false;
    }

    static boolean isBuyer(Creature maybeBuyer) {
        return maybeBuyer.getName().startsWith("Buyer_") && maybeBuyer.getTemplate().id == CreatureTemplateIds.SALESMAN_CID;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
