package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import mod.wurmunlimited.buyermerchant.db.BuyerScheduler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddItemToBuyerUpdateQuestionTests extends AddItemToBuyerQuestionTest {
    @Override
    protected AddItemToBuyerQuestion askQuestion(Creature asker, Creature buyer) {
        AddItemToBuyerQuestion question = new AddItemToBuyerUpdateQuestion(asker, buyer.getWurmId());
        super.askQuestion(question);
        return question;
    }

    @Override
    protected byte getMaterialType() {
        return BuyerScheduler.getUpdatesFor(buyer)[0].material;
    }

    @Override
    protected boolean instanceOfMenu(Question question) {
        return question instanceof SetUpdateIntervalQuestion;
    }

    @Override
    protected void answerInterval() {
        answers.setProperty("submit", "true");
        answers.setProperty("interval", "10");
        answer();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testItemWithCorrectDetailsAddedToPriceList() throws NoSuchTemplateException {
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
        answerInterval();

        BuyerScheduler.Update item = BuyerScheduler.getUpdatesFor(buyer)[0];
        Change price = new Change(item.getPrice());

        assertAll(
                () -> assertEquals(templateId, item.template.getTemplateId(), "Template Id incorrect"),
                () -> assertEquals(material, item.material, "Material incorrect"),
                () -> assertEquals(weight, item.getWeight(), "Weight incorrect"),
                () -> assertEquals(ql, item.getMinQL(), "QL incorrect"),
                () -> assertEquals(change.goldCoins, price.goldCoins, "Gold incorrect"),
                () -> assertEquals(change.silverCoins, price.silverCoins, "Silver incorrect"),
                () -> assertEquals(change.copperCoins, price.copperCoins, "Copper incorrect"),
                () -> assertEquals(change.ironCoins, price.ironCoins, "Iron incorrect"),
                () -> assertEquals(remainingToPurchase, item.getRemainingToPurchase(), "Remaining to Purchase incorrect"),
                () -> assertEquals(minimumPurchase, item.getMinimumPurchase(), "Minimum Purchase incorrect"),
                () -> assertEquals(acceptsDamaged, item.getAcceptsDamaged(), "Accepts Damaged incorrect")
        );
    }
}
