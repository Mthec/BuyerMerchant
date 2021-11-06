package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddItemToBuyerInstantQuestionTests extends AddItemToBuyerQuestionTest {
    @Override
    protected AddItemToBuyerQuestion askQuestion(Creature asker, Creature buyer) {
        AddItemToBuyerQuestion question = new AddItemToBuyerInstantQuestion(asker, buyer.getWurmId());
        super.askQuestion(question);
        return question;
    }

    @Override
    protected byte getMaterialType() throws PriceList.NoPriceListOnBuyer {
        return PriceList.getPriceListFromBuyer(buyer).iterator().next().getItem().getMaterial();
    }

    @Override
    protected boolean instanceOfMenu(Question question) {
        return question instanceof SetBuyerPricesQuestion;
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testItemWithCorrectDetailsAddedToPriceList() throws PriceList.NoPriceListOnBuyer, NoSuchTemplateException {
        int templateId = 7;
        byte material = ItemTemplateFactory.getInstance().getTemplate(templateId).getMaterial();
        int weight = 1000;
        float ql = 55.6f;
        int money = 1122334455;
        int remainingToPurchase = 200;
        int minimumPurchase = 100;
        boolean acceptsDamaged = true;
        Change change = new Change(money);

        askQuestion();
        answers.setProperty("templateId", getElementPositionInOptions(com.lastBmlContent, templateId));
        answer();

        answers.setProperty("material", getElementPositionInOptions(com.lastBmlContent, Item.getMaterialString(material)));
        answer();

        answers.setProperty("weight", WeightString.toString(weight));
        answers.setProperty("q", Float.toString(ql));
        answers.setProperty("g", Long.toString(change.goldCoins));
        answers.setProperty("s", Long.toString(change.silverCoins));
        answers.setProperty("c", Long.toString(change.copperCoins));
        answers.setProperty("i", Long.toString(change.ironCoins));
        answers.setProperty("r", Integer.toString(remainingToPurchase));
        answers.setProperty("p", Integer.toString(minimumPurchase));
        answers.setProperty("d", Boolean.toString(acceptsDamaged));
        answer();

        PriceList.Entry item = PriceList.getPriceListFromBuyer(buyer).iterator().next();
        Change price = new Change(item.getPrice());

        assertAll(
                () -> assertEquals(templateId, item.getItem().getTemplateId(), "Template Id incorrect"),
                () -> assertEquals(material, item.getItem().getMaterial(), "Material incorrect"),
                () -> assertEquals(weight, item.getItem().getWeightGrams(), "Weight incorrect"),
                () -> assertEquals(ql, item.getItem().getQualityLevel(), "QL incorrect"),
                () -> assertEquals(change.goldCoins, price.goldCoins, "Gold incorrect"),
                () -> assertEquals(change.silverCoins, price.silverCoins, "Silver incorrect"),
                () -> assertEquals(change.copperCoins, price.copperCoins, "Copper incorrect"),
                () -> assertEquals(change.ironCoins, price.ironCoins, "Iron incorrect"),
                () -> assertEquals(remainingToPurchase, item.getRemainingToPurchase(), "Remaining to Purchase incorrect"),
                () -> assertEquals(minimumPurchase, item.getMinimumPurchase(), "Minimum Purchase incorrect"),
                () -> assertEquals(acceptsDamaged, item.acceptsDamaged(), "Accepts Damaged incorrect")
        );
    }
}
