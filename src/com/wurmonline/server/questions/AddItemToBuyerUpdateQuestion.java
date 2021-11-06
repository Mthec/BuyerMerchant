package com.wurmonline.server.questions;

import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.ItemTemplate;
import mod.wurmunlimited.buyermerchant.ItemDetails;

import java.util.List;

public class AddItemToBuyerUpdateQuestion extends AddItemToBuyerQuestion {
    private static final String[] questionTitles = new String[] {
            "Schedule an update to the buyers list:",
            "Select the material for the new schedule:",
            "Select the material for the new schedule:",
            "Set final item details for the new schedule:"};

    AddItemToBuyerUpdateQuestion(Creature aResponder, long aTarget) {
        super(aResponder, questionTitles[0], aTarget);
    }

    protected AddItemToBuyerUpdateQuestion(Creature aResponder, long aTarget, ItemTemplate template, byte material, int stage, String filter, boolean customMaterial, List<String> materialList) {
        super(aResponder, questionTitles[stage], aTarget, template, material, stage, filter, customMaterial, materialList);
    }

    @Override
    protected AddItemToBuyerQuestion createQuestion(Creature responder, long target, ItemTemplate itemTemplate, byte material, int stage, String filter, boolean customMaterial, List<String> materialsList) {
        return new AddItemToBuyerUpdateQuestion(responder, target, itemTemplate, material, stage, filter, customMaterial, materialsList);
    }

    @Override
    protected void addItem() {
        Creature responder = this.getResponder();
        try {
            Creature buyer = Server.getInstance().getCreature(this.target);
            ItemDetails details = SetBuyerPricesQuestion.getItemDetails(responder, getAnswer(), -1, itemTemplate.getTemplateId(), itemTemplate.getName(), itemTemplate.getWeightGrams());

            new SetUpdateIntervalQuestion(responder, buyer, itemTemplate, material, details).sendQuestion();
        } catch (NoSuchPlayerException | NoSuchCreatureException e) {
            e.printStackTrace();
        }
    }
}
