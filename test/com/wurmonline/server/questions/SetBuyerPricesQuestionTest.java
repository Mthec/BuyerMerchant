package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.*;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.WurmTradingQuestionTest;
import mod.wurmunlimited.buyermerchant.PriceList;
import mod.wurmunlimited.buyermerchant.PriceListTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SetBuyerPricesQuestionTest extends WurmTradingQuestionTest {

    private void askQuestion() {
        super.askQuestion(new SetBuyerPricesQuestion(owner, buyer.getWurmId()));
    }

    private void addItemToPriceList(int templateId, float ql, int price) {
        try {
            PriceList list = PriceList.getPriceListFromBuyer(buyer);
            ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(templateId);
            list.addItem(template.getTemplateId(), ItemMaterials.MATERIAL_MEAT_DRAGON, ql, price);
            list.savePriceList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Properties generateProperties(String id, float ql, int price) {
        Properties answers = new Properties();
        answers.setProperty(id+"q", Float.toString(ql));
        answers.setProperty(id+"g", Integer.toString((int)new Change(price).getGoldCoins()));
        answers.setProperty(id+"s", Integer.toString((int)new Change(price).getSilverCoins()));
        answers.setProperty(id+"c", Integer.toString((int)new Change(price).getCopperCoins()));
        answers.setProperty(id+"i", Integer.toString((int)new Change(price).getIronCoins()));
        return answers;
    }

    private Properties generateProperties(int id, float ql, int price) {
        return generateProperties(Integer.toString(id), ql, price);
    }

    private Properties generateProperties(float ql, int price) {
        return generateProperties("", ql, price);
    }

    @Test
    void setItemQLAndPrice() throws PriceList.PriceListFullException {
        float ql = 50;
        int price = 123456789;
        Properties answers = generateProperties(ql, price);
        PriceList priceList = new PriceList(PriceListTest.createPriceList(PriceListTest.one));
        PriceList.Entry item = priceList.iterator().next();

        SetBuyerPricesQuestion.setItemQLAndPrice(item, -1, answers, factory.createNewCreature());
        assertEquals(ql, item.getQualityLevel(), 0.01);
        assertEquals(price, item.getPrice());
    }

    @Test
    void setItemQLAndPriceWithId() throws PriceList.PriceListFullException {
        int id = 12;
        float ql = 50;
        int price = 123456789;
        Properties answers = generateProperties(id, ql, price);
        PriceList priceList = new PriceList(PriceListTest.createPriceList(PriceListTest.one));
        PriceList.Entry item = priceList.iterator().next();

        SetBuyerPricesQuestion.setItemQLAndPrice(item, 12, answers, factory.createNewCreature());
        assertEquals(ql, item.getQualityLevel(), 0.01);
        assertEquals(price, item.getPrice());
    }

    @Test
    void setItemQLAndPriceNegativeQL() throws PriceList.PriceListFullException {
        float ql = -100;
        int price = 123456789;
        Properties answers = generateProperties(ql, price);
        PriceList priceList = new PriceList(PriceListTest.createPriceList(PriceListTest.one));
        PriceList.Entry item = priceList.iterator().next();
        item.updateItem(7, (byte)0, 1, 1);

        SetBuyerPricesQuestion.setItemQLAndPrice(item, -1, answers, factory.createNewCreature());
        assertEquals(1.0, item.getQualityLevel(), 0.01);
    }

    @Test
    void setItemQLAndPriceOver100QL() throws PriceList.PriceListFullException {
        float ql = 101;
        int price = 123456789;
        Properties answers = generateProperties(ql, price);
        PriceList priceList = new PriceList(PriceListTest.createPriceList(PriceListTest.one));
        PriceList.Entry item = priceList.iterator().next();
        item.updateItem(7, (byte)0, 1, 1);

        Creature creature = factory.createNewCreature();
        SetBuyerPricesQuestion.setItemQLAndPrice(item, -1, answers, creature);
        assertEquals(1.0, item.getQualityLevel(), 0.01);
        assertEquals("Failed to set the minimum quality level for " + item.getName() + ".", factory.getCommunicator(creature).lastNormalServerMessage);
    }

    @Test
    void testPriceNumberFormatErrors() throws PriceList.NoPriceListOnBuyer, PriceList.PriceListFullException {
        float ql = 89.7f;
        int price = 987654321;
        Properties answers = generateProperties(ql, price);
        answers.setProperty("g", "a");
        answers.setProperty("s", "1.0");
        answers.setProperty("c", "!");
        answers.setProperty("i", ".2");
        addItemToPriceList(1, ql, price);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        SetBuyerPricesQuestion.setItemQLAndPrice(priceList.iterator().next(), -1, answers, owner);

        String[] messages = factory.getCommunicator(owner).getMessages();
        assertEquals(4, messages.length);
        assertTrue(messages[0].contains("Failed to set"));
        assertTrue(messages[1].contains("Failed to set"));
        assertTrue(messages[2].contains("Failed to set"));
        assertTrue(messages[3].contains("Failed to set"));

        assertEquals(PriceList.unauthorised, priceList.getItems().iterator().next().getPrice());
    }

    @Test
    void testPriceNegative() throws PriceList.NoPriceListOnBuyer, PriceList.PriceListFullException {
        float ql = 89.7f;
        int price = 987654321;
        Properties answers = generateProperties(ql, price);
        answers.setProperty("g", "-1");
        answers.setProperty("s", "-1");
        answers.setProperty("c", "-2");
        answers.setProperty("i", "-10");
        addItemToPriceList(1, ql, price);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        SetBuyerPricesQuestion.setItemQLAndPrice(priceList.iterator().next(), -1, answers, owner);

        String[] messages = factory.getCommunicator(owner).getMessages();
        assertEquals(1, messages.length);
        assertTrue(messages[0].contains("Failed to set a negative price"));

        assertEquals(PriceList.unauthorised, priceList.getItems().iterator().next().getPrice());
    }

    @Test
    void sendQuestion() throws NoSuchCreatureException {
        Creature player = factory.createNewPlayer();
        Creature buyer = factory.createNewBuyer(player);
        assert factory.getCreature(buyer.getWurmId()) == buyer;
        SetBuyerPricesQuestion question = new SetBuyerPricesQuestion(player, buyer.getWurmId());
        question.sendQuestion();
        assertNotEquals("No shop registered for that creature.", factory.getCommunicator(player).lastNormalServerMessage);
        assertTrue(!factory.getCommunicator(player).lastBmlContent.equals(FakeCommunicator.empty));
    }

    @Test
    void testInvalidBuyerId() {
        long fakeId = 1;
        while (true) {
            try {
                factory.getCreature(fakeId);
            } catch (NoSuchCreatureException e) {
                break;
            }
            ++fakeId;
        }
        new SetBuyerPricesQuestion(owner, fakeId).sendQuestion();
        FakeCommunicator playerCom = factory.getCommunicator(owner);
        assertEquals(FakeCommunicator.empty, playerCom.lastBmlContent);
        assertEquals("No such creature.", playerCom.lastNormalServerMessage);
    }

    @Test
    void testNoPriceList() {
        ItemsPackageFactory.removeItem(buyer, buyer.getInventory().getItems().toArray(new Item[0])[0]);
        askQuestion();
        FakeCommunicator playerCom = factory.getCommunicator(owner);
        assertEquals(FakeCommunicator.empty, playerCom.lastBmlContent);
        assertEquals(PriceList.noPriceListFoundPlayerMessage, playerCom.lastNormalServerMessage);
    }

    @Test
    void testOwnershipChangedCantManage() {
        askQuestion();
        buyer.getShop().setOwner(player.getWurmId());
        answers = generateProperties(1.0f, 100);
        answer();
        assertThat(owner, receivedMessageContaining("You don't own"));
    }

    @Test
    void testNonOwnerCantManage() {
        new SetBuyerPricesQuestion(player, buyer.getWurmId()).sendQuestion();
        FakeCommunicator ownerCom = factory.getCommunicator(owner);
        assertEquals(FakeCommunicator.empty, ownerCom.lastBmlContent);
        assertThat(player, receivedMessageContaining("You don't own"));
    }

    @Test
    void testOwnerCanManage() {
        askQuestion();
        FakeCommunicator ownerCom = factory.getCommunicator(owner);
        assertNotEquals(FakeCommunicator.empty, ownerCom.lastBmlContent);
        assertEquals(FakeCommunicator.empty, ownerCom.lastNormalServerMessage);
    }

    @Test
    void testRowsAdded() throws PriceList.PriceListFullException, IOException, NoSuchTemplateException {
        FakeCommunicator ownerCom = factory.getCommunicator(owner);
        askQuestion();
        int empty = factory.getCommunicator(owner).lastBmlContent.length();
        Item priceList = buyer.getInventory().getItems().toArray(new Item[0])[0];
        PriceList list = new PriceList(priceList);
        list.addItem(1, (byte)0);
        list.savePriceList();
        askQuestion();
        int length1 = ownerCom.lastBmlContent.length();

        list = new PriceList(priceList);
        list.addItem(1, (byte)0);
        list.savePriceList();
        askQuestion();
        int length2 = ownerCom.lastBmlContent.length();

        list = new PriceList(priceList);
        list.addItem(1, (byte)0);
        list.savePriceList();
        askQuestion();
        int length3 = ownerCom.lastBmlContent.length();

        assertThat(Arrays.asList(empty, length1, length2, length3), inAscendingOrder());
    }

    @Test
    void testItemValuesCorrect() throws PriceList.PriceListFullException, IOException, NoSuchTemplateException {
        PriceList list = PriceList.getPriceListFromBuyer(buyer);
        float ql = 55.6f;
        int money = 1122334455;
        Change change = new Change(money);
        list.addItem(1, (byte)1, ql, money);
        list.savePriceList();
        askQuestion();

        String bml = factory.getCommunicator(owner).lastBmlContent;
        assert !bml.equals(FakeCommunicator.empty);
        Matcher m = Pattern.compile("id=\"[0-9]+[qgsci]\";text=\"([0-9.]+)\"").matcher(bml);

        assertTrue(m.find() && Math.abs(Float.parseFloat(m.group(1)) - ql) < 0.01f);
        assertTrue(m.find() && Long.parseLong(m.group(1)) == change.getGoldCoins());
        assertTrue(m.find() && Long.parseLong(m.group(1)) == change.getSilverCoins());
        assertTrue(m.find() && Long.parseLong(m.group(1)) == change.getCopperCoins());
        assertTrue(m.find() && Long.parseLong(m.group(1)) == change.getIronCoins());
    }

    @Test
    void testItemNameCorrect() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(7, (byte)1);
        priceList.savePriceList();
        String itemName = priceList.iterator().next().getName();
        askQuestion();

        String bml = factory.getCommunicator(owner).lastBmlContent;
        assert !bml.equals(FakeCommunicator.empty);
        Matcher m = Pattern.compile("\\?\"}label\\{text=\"([a-zA-Z_]+)\"};").matcher(bml);

        assertTrue(m.find());
        assertEquals(itemName, m.group(1));
    }

    @Test
    void testItemMaterialCorrect() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        PriceList list = PriceList.getPriceListFromBuyer(buyer);
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(factory.getIsMetalId());
        list.addItem(template.getTemplateId(), ItemMaterials.MATERIAL_MEAT_DRAGON);
        list.savePriceList();
        askQuestion();

        String bml = com.lastBmlContent;
        assert !bml.equals(FakeCommunicator.empty);
        Item item = factory.createNewItem();
        item.setTemplateId(template.getTemplateId());
        item.setMaterial(ItemMaterials.MATERIAL_MEAT_DRAGON);

        assertTrue(bml.contains(Question.itemNameWithColorByRarity(item)));
    }

    @Test
    void testAddItemToBuyerQuestionAsked() {
        askQuestion();

        // Reset bml messages.
        factory.attachFakeCommunicator(owner);

        answers.setProperty("new", "true");
        answer();

        new AddItemToBuyerQuestion(owner, buyer.getWurmId()).sendQuestion();

        assertThat(owner, bmlEqual());
    }

    @Test
    void testItemQLAndPriceSetCorrectly() throws PriceList.NoPriceListOnBuyer {
        float ql = 96.0f;
        int money = 123456789;
        int templateId = factory.getIsMetalId();
        addItemToPriceList(templateId, ql, money);

        askQuestion();
        answers = generateProperties(1, ql, money);
        answer();

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        PriceList.Entry item = priceList.iterator().next();

        assertEquals(ql, item.getQualityLevel(), 0.01f);
        assertEquals(money, item.getPrice());
    }

    @Test
    void testRemoveItemFromList() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        for (int i = 1; i < 15; ++i) {
            priceList.addItem(7, (byte)0, 1.0f, i);
        }
        priceList.savePriceList();

        askQuestion();

        Matcher matcher = Pattern.compile("id=\"(\\d+)i\";").matcher(com.lastBmlContent);
        Set<Integer> deleted = new HashSet<>(15);
        while (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            if (id > 4 && id < 8) {
                deleted.add(id);
                answers.setProperty(id + "remove", "true");
            }
        }
        answer();

        priceList = PriceList.getPriceListFromBuyer(buyer);
        assertEquals(11, priceList.size());
        assertThat(priceList.stream().mapToInt(PriceList.Entry::getPrice).boxed().collect(Collectors.toSet()), containsNoneOf(deleted));
    }

    @Test
    void testKilogramsString() throws NoSuchTemplateException, IOException, PriceList.PriceListFullException {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(factory.getIsWoodId());
        assert template.getWeightGrams() == 24000;
        ItemTemplate template2 = ItemTemplateFactory.getInstance().getTemplate(factory.getIsCoinId());
        assert template2.getWeightGrams() == 10;

        PriceList list = PriceList.getPriceListFromBuyer(buyer);
        list.addItem(template.getTemplateId(), ItemMaterials.MATERIAL_MEAT_DRAGON);
        list.addItem(template2.getTemplateId(), ItemMaterials.MATERIAL_MEAT_DRAGON);
        list.savePriceList();
        askQuestion();

        assertTrue(com.lastBmlContent.contains("24kg"));
        assertTrue(com.lastBmlContent.contains("0.01kg"));
    }
}