package com.wurmonline.server.questions;

import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.NoSuchTemplateException;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.io.IOException;
import java.util.List;

public class AddItemToBuyerInstantQuestion extends AddItemToBuyerQuestion {
    private static final String[] questionTitles = new String[] {
            "Add a new item to the buyers list:",
            "Select the material for the new entry:",
            "Select the material for the new entry:",
            "Set final details for the new entry:"};

    AddItemToBuyerInstantQuestion(Creature aResponder, long aTarget) {
        super(aResponder, questionTitles[0], aTarget);
    }

    protected AddItemToBuyerInstantQuestion(Creature aResponder, long aTarget, ItemTemplate template, byte material, int stage, String filter, boolean customMaterial, List<String> materialList) {
        super(aResponder, questionTitles[stage], aTarget, template, material, stage, filter, customMaterial, materialList);
    }

    @Override
    protected AddItemToBuyerQuestion createQuestion(Creature player, long target, ItemTemplate itemTemplate, byte material, int stage, String filter, boolean customMaterial, List<String> materialsList) {
        return new AddItemToBuyerInstantQuestion(player, target, itemTemplate, material, stage, filter, customMaterial, materialsList);
    }

    @Override
    protected void addItem() {
        Creature responder = this.getResponder();
        try {
            Creature buyer = Server.getInstance().getCreature(this.target);

            try {
                PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
                // Any material is 0.  Client will not label 0 material (unknown).
                PriceList.Entry newItem = priceList.addItem(itemTemplate.getTemplateId(), material);
                SetBuyerPricesQuestion.setItemDetails(newItem, -1, this.getAnswer(), responder);
                priceList.savePriceList();
                responder.getCommunicator().sendNormalServerMessage(buyer.getName() + " adds the item to their list.");
            } catch (PriceList.NoPriceListOnBuyer | PriceList.PageNotAdded noPriceListOnBuyer) {
                responder.getCommunicator().sendNormalServerMessage(PriceList.noPriceListFoundPlayerMessage);
                noPriceListOnBuyer.printStackTrace();
            } catch (PriceList.PriceListFullException e) {
                responder.getCommunicator().sendNormalServerMessage(PriceList.noSpaceOnPriceListPlayerMessage);
            } catch (NoSuchTemplateException | IOException e) {
                responder.getCommunicator().sendNormalServerMessage(PriceList.couldNotCreateItemPlayerMessage);
                e.printStackTrace();
            }

        } catch (NoSuchPlayerException | NoSuchCreatureException e) {
            e.printStackTrace();
        }

        new SetBuyerPricesQuestion(responder, this.target).sendQuestion();
    }
}
