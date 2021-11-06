package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.*;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.WurmTradingQuestionTest;
import mod.wurmunlimited.buyermerchant.EntryBuilder;
import mod.wurmunlimited.buyermerchant.PriceList;
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
        addItemToPriceList(templateId, ql, price, false);
    }

    private void addItemToPriceList(int templateId, float ql, int price, boolean acceptsDamage) {
        try {
            PriceList list = PriceList.getPriceListFromBuyer(buyer);
            ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(templateId);
            list.addItem(template.getTemplateId(), ItemMaterials.MATERIAL_MEAT_DRAGON, -1, ql, price, 0, 1, acceptsDamage);
            list.savePriceList();
        } catch (NoSuchTemplateException | IOException | PriceList.PageNotAdded | PriceList.PriceListFullException e) {
            throw new RuntimeException(e);
        }
    }

    private Properties generateProperties(String id, int weight, float ql, int price, int remainingToPurchase, int minimumPurchase, boolean acceptsDamaged) {
        Properties answers = new Properties();
        if (weight != -1)
            answers.setProperty(id+"weight", Integer.toString(weight).replaceFirst("(\\d\\d\\d)$", ".$1"));
        answers.setProperty(id+"q", Float.toString(ql));
        answers.setProperty(id+"g", Integer.toString((int)new Change(price).getGoldCoins()));
        answers.setProperty(id+"s", Integer.toString((int)new Change(price).getSilverCoins()));
        answers.setProperty(id+"c", Integer.toString((int)new Change(price).getCopperCoins()));
        answers.setProperty(id+"i", Integer.toString((int)new Change(price).getIronCoins()));
        answers.setProperty(id+"r", Integer.toString(remainingToPurchase));
        if (minimumPurchase != 1)
            answers.setProperty(id+"p", Integer.toString(minimumPurchase));
        answers.setProperty(id+"d", Boolean.toString(acceptsDamaged));
        return answers;
    }

    private Properties generateProperties(int id, int weight, float ql, int price, int minimumPurchase, boolean acceptsDamaged) {
        return generateProperties(Integer.toString(id), weight, ql, price, 0, minimumPurchase, acceptsDamaged);
    }

    private Properties generateProperties(int id, int weight, float ql, int price, int minimumPurchase) {
        return generateProperties(id, weight, ql, price, minimumPurchase, false);
    }

    private Properties generateProperties(int weight, float ql, int price, int minimumPurchase) {
        return generateProperties(1, weight, ql, price, minimumPurchase);
    }

    private Properties generateProperties(int weight, float ql, int price) {
        return generateProperties("1", weight, ql, price, 0, 1, false);
    }

    private Properties generateProperties(float ql, int price) {
        return generateProperties("1", -1, ql, price, 0, 1, false);
    }

    @Test
    void setItemQLAndPrice() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, NoSuchTemplateException {
        float ql = 50;
        int price = 123456789;
        Properties answers = generateProperties(ql, price);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(1,(byte)1,-1,1.0f,1);
        priceList.savePriceList();
        PriceList.Entry item = priceList.iterator().next();

        SetBuyerPricesQuestion.setItemDetails(item, 1, answers, factory.createNewCreature());
        assertEquals(ql, item.getQualityLevel(), 0.01);
        assertEquals(price, item.getPrice());
    }

    @Test
    void setItemWeight() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, NoSuchTemplateException {
        int weight = 4321;
        float ql = 50;
        int price = 123456789;
        Properties answers = generateProperties(weight, ql, price);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(1, (byte)1, -1,1.0f,1);
        priceList.savePriceList();
        PriceList.Entry item = priceList.iterator().next();

        SetBuyerPricesQuestion.setItemDetails(item, 1, answers, factory.createNewCreature());
        assertEquals(weight, item.getWeight());
    }

    @Test
    void setItemQLAndPriceWithId() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, NoSuchTemplateException {
        int id = 12;
        float ql = 50;
        int price = 123456789;
        Properties answers = generateProperties(id, -1, ql, price, 1);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(1, (byte)1,-1,1.0f,1);
        priceList.savePriceList();
        PriceList.Entry item = priceList.iterator().next();

        SetBuyerPricesQuestion.setItemDetails(item, 12, answers, factory.createNewCreature());
        assertEquals(ql, item.getQualityLevel(), 0.01);
        assertEquals(price, item.getPrice());
    }

    @Test
    void setItemQLAndPriceNegativeQL() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, NoSuchTemplateException {
        float ql = -100;
        int price = 123456789;
        Properties answers = generateProperties(ql, price);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(1, (byte)1,-1,1.0f,1);
        priceList.savePriceList();
        PriceList.Entry item = priceList.iterator().next();
        item.updateItem(7, (byte)0, -1, 1, 1, 0, 1, false);

        SetBuyerPricesQuestion.setItemDetails(item, -1, answers, factory.createNewCreature());
        assertEquals(1.0, item.getQualityLevel(), 0.01);
    }

    @Test
    void setItemQLAndPriceOver100QL() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, NoSuchTemplateException {
        float ql = 101;
        int price = 123456789;
        Properties answers = generateProperties(ql, price);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(1, (byte)1,-1,1.0f,1);
        priceList.savePriceList();
        PriceList.Entry item = priceList.iterator().next();
        item.updateItem(7, (byte)0, -1, 1, 1, 0, 1, false);

        Creature creature = factory.createNewCreature();
        SetBuyerPricesQuestion.setItemDetails(item, 1, answers, creature);
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
        answers.setProperty("weight", WeightString.toString(priceList.iterator().next().getWeight()));
        priceList.destroyItems();
        SetBuyerPricesQuestion.setItemDetails(priceList.iterator().next(), -1, answers, owner);

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
        answers.setProperty("weight", WeightString.toString(priceList.iterator().next().getWeight()));
        priceList.destroyItems();
        SetBuyerPricesQuestion.setItemDetails(priceList.iterator().next(), -1, answers, owner);

        String[] messages = factory.getCommunicator(owner).getMessages();
        assertEquals(1, messages.length);
        assertTrue(messages[0].contains("Failed to set a negative price"));

        assertEquals(PriceList.unauthorised, priceList.getItems().iterator().next().getPrice());
    }

    @Test
    void testItemAcceptsDamaged() throws PriceList.NoPriceListOnBuyer {
        addItemToPriceList(1, 1, 1, true);
        assert PriceList.getPriceListFromBuyer(buyer).iterator().next().getMinimumPurchase() == 1;

        askQuestion();
        answers = generateProperties(1, 1, 1, 1, 1, true);
        answer();

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        PriceList.Entry item = priceList.iterator().next();

        assertTrue(item.acceptsDamaged());
    }

    @Test
    void testItemDoesNotAcceptDamaged() throws PriceList.NoPriceListOnBuyer {
        addItemToPriceList(1, 1, 1, false);
        assert PriceList.getPriceListFromBuyer(buyer).iterator().next().getMinimumPurchase() == 1;

        askQuestion();
        answers = generateProperties(-1, 1, 1, 1, 1, false);
        answer();

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        PriceList.Entry item = priceList.iterator().next();

        assertFalse(item.acceptsDamaged());
    }

    @Test
    void testAcceptsDamagedInvalidSetsFalse() throws PriceList.NoPriceListOnBuyer {
        addItemToPriceList(1, 1, 1, false);
        assert PriceList.getPriceListFromBuyer(buyer).iterator().next().getMinimumPurchase() == 1;

        askQuestion();
        answers = generateProperties(-1, 1, 1, 1, 1);
        answers.setProperty("d", "abc");
        answer();

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        PriceList.Entry item = priceList.iterator().next();

        assertFalse(item.acceptsDamaged());
    }

    @Test
    void sendQuestion() throws NoSuchCreatureException {
        Creature player = factory.createNewPlayer();
        Creature buyer = factory.createNewBuyer(player);
        assert factory.getCreature(buyer.getWurmId()) == buyer;
        SetBuyerPricesQuestion question = new SetBuyerPricesQuestion(player, buyer.getWurmId());
        question.sendQuestion();
        assertNotEquals("No shop registered for that creature.", factory.getCommunicator(player).lastNormalServerMessage);
        assertNotEquals(FakeCommunicator.empty, factory.getCommunicator(player).lastBmlContent);
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
    void testRowsAdded() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, NoSuchTemplateException {
        FakeCommunicator ownerCom = factory.getCommunicator(owner);
        askQuestion();
        int empty = factory.getCommunicator(owner).lastBmlContent.length();
        PriceList list = PriceList.getPriceListFromBuyer(buyer);
        list.addItem(1, (byte)0);
        list.savePriceList();
        askQuestion();
        int length1 = ownerCom.lastBmlContent.length();

        list = PriceList.getPriceListFromBuyer(buyer);
        list.addItem(1, (byte)0);
        list.savePriceList();
        askQuestion();
        int length2 = ownerCom.lastBmlContent.length();

        list = PriceList.getPriceListFromBuyer(buyer);
        list.addItem(1, (byte)0);
        list.savePriceList();
        askQuestion();
        int length3 = ownerCom.lastBmlContent.length();

        assertThat(Arrays.asList(empty, length1, length2, length3), inAscendingOrder());
    }

    @Test
    void testItemValuesCorrect() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, NoSuchTemplateException {
        PriceList list = PriceList.getPriceListFromBuyer(buyer);
        int weight = 1234;
        float ql = 55.6f;
        int money = 1122334455;
        Change change = new Change(money);
        list.addItem(1, (byte)1, weight, ql, money);
        list.savePriceList();
        askQuestion();

        String bml = factory.getCommunicator(owner).lastBmlContent;
        assert !bml.equals(FakeCommunicator.empty);
        Matcher m = Pattern.compile("id=\"[0-9]+(weight|[qgsci])\";text=\"([0-9.]+)").matcher(bml);

        assertTrue(m.find());
        assertEquals(weight, WeightString.toInt(m.group(2)));
        assertTrue(m.find());
        assertEquals(ql, Math.abs(Float.parseFloat(m.group(2))), 0.01f);
        assertTrue(m.find());
        assertEquals(change.getGoldCoins(), Long.parseLong(m.group(2)));
        assertTrue(m.find());
        assertEquals(change.getSilverCoins(), Long.parseLong(m.group(2)));
        assertTrue(m.find());
        assertEquals(change.getCopperCoins(), Long.parseLong(m.group(2)));
        assertTrue(m.find());
        assertEquals(change.getIronCoins(), Long.parseLong(m.group(2)));
    }

    @Test
    void testItemNameCorrect() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
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
    void testItemMaterialCorrect() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
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
    void testAddItemToBuyerInstantQuestionAsked() {
        askQuestion();

        // Reset bml messages.
        factory.attachFakeCommunicator(owner);

        answers.setProperty("new", "true");
        answer();

        new AddItemToBuyerInstantQuestion(owner, buyer.getWurmId()).sendQuestion();

        assertThat(owner, bmlEqual());
    }

    @Test
    void testAddItemToBuyerUpdateQuestionAsked() {
        askQuestion();

        // Reset bml messages.
        factory.attachFakeCommunicator(owner);

        answers.setProperty("schedule", "true");
        answer();

        new UpdateScheduleQuestion(owner, buyer).sendQuestion();

        assertThat(owner, bmlEqual());
    }

    @Test
    void testItemDetailsSetCorrectly() throws PriceList.NoPriceListOnBuyer {
        int weight = 1234;
        float ql = 96.0f;
        int money = 123456789;
        int remainingToPurchase = 234;
        int minimumPurchase = 100;
        int templateId = factory.getIsMetalId();
        addItemToPriceList(templateId, ql, money);
        assert PriceList.getPriceListFromBuyer(buyer).iterator().next().getMinimumPurchase() == 1;
        assert PriceList.getPriceListFromBuyer(buyer).iterator().next().getRemainingToPurchase() == 0;
        assert !PriceList.getPriceListFromBuyer(buyer).iterator().next().acceptsDamaged();

        askQuestion();
        answers = generateProperties(String.valueOf(1), weight, ql, money, remainingToPurchase, minimumPurchase, true);
        answer();

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        PriceList.Entry item = priceList.iterator().next();

        assertEquals(weight, item.getWeight());
        assertEquals(ql, item.getQualityLevel(), 0.01f);
        assertEquals(money, item.getPrice());
        assertEquals(remainingToPurchase, item.getRemainingToPurchase());
        assertEquals(minimumPurchase, item.getMinimumPurchase());
        assertTrue(item.acceptsDamaged());
    }

    @Test
    void testRemoveItemFromList() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        for (int i = 1; i < 15; ++i) {
            priceList.addItem(7, (byte)i, -1, 1.0f, i);
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
    void testKilogramsString() throws NoSuchTemplateException, IOException, PriceList.PriceListFullException, PriceList.PageNotAdded {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(factory.getIsWoodId());
        assert template.getWeightGrams() == 24000;
        ItemTemplate template2 = ItemTemplateFactory.getInstance().getTemplate(factory.getIsCoinId());
        assert template2.getWeightGrams() == 10;

        PriceList list = PriceList.getPriceListFromBuyer(buyer);
        list.addItem(template.getTemplateId(), ItemMaterials.MATERIAL_MEAT_DRAGON);
        list.addItem(template2.getTemplateId(), ItemMaterials.MATERIAL_MEAT_DRAGON);
        list.addItem(1, ItemMaterials.MATERIAL_MEAT_DRAGON, 4321, 1.0f, 1);
        list.savePriceList();
        askQuestion();

        assertTrue(com.lastBmlContent.replace("\"};label{text=\"", "").contains("24kg"));
        assertTrue(com.lastBmlContent.replace("\"};label{text=\"", "").contains("0.01kg"));
        assertTrue(com.lastBmlContent.replace("\"};label{text=\"", "").contains("4.321kg"));
    }

    @Test
    void testPriceListSortingBml() throws PriceList.PriceListFullException, PriceList.PageNotAdded, PriceList.NoPriceListOnBuyer {
        addItemToPriceList(ItemList.coinCopper,1.0f,10);
        addItemToPriceList(ItemList.backPack,1.0f,10);
        addItemToPriceList(ItemList.log,12.0f,10);
        addItemToPriceList(ItemList.log,35.0f,10);

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        Pattern originalPattern = Pattern.compile("^[\\w]+coin[\\w]+backpack[\\w]+log[\\w]+12[\\w]+log[\\w]+35[\\w]+$");
        Pattern sortedPattern = Pattern.compile("^[\\w]+backpack[\\w]+coin[\\w]+log[\\w]+35[\\w]+log[\\w]+12[\\w]+$");
        Pattern removeSpecialCharacters = Pattern.compile("([^\\w]+)");

        new SetBuyerPricesQuestion(owner, buyer.getWurmId()).sendQuestion();
        String bml1 = removeSpecialCharacters.matcher(factory.getCommunicator(owner).lastBmlContent).replaceAll("");
        assertTrue(originalPattern.matcher(bml1).find());
        assertFalse(sortedPattern.matcher(bml1).find());

        priceList.sortAndSave();

        new SetBuyerPricesQuestion(owner, buyer.getWurmId()).sendQuestion();
        String bml2 = removeSpecialCharacters.matcher(factory.getCommunicator(owner).lastBmlContent).replaceAll("");
        assertTrue(sortedPattern.matcher(bml2).find());
    }

    @Test
    void testPriceListSortingButtonClicked() {
        addItemToPriceList(ItemList.coinCopper,1.0f,10);
        addItemToPriceList(ItemList.backPack,1.0f,10);
        addItemToPriceList(ItemList.log,12.0f,10);
        addItemToPriceList(ItemList.log,35.0f,10);

        Pattern originalPattern = Pattern.compile("^[\\w]+coin[\\w]+backpack[\\w]+log[\\w]+12[\\w]+log[\\w]+35[\\w]+$");
        Pattern sortedPattern = Pattern.compile("^[\\w]+backpack[\\w]+coin[\\w]+log[\\w]+35[\\w]+log[\\w]+12[\\w]+$");
        Pattern removeSpecialCharacters = Pattern.compile("([^\\w]+)");

        SetBuyerPricesQuestion question = new SetBuyerPricesQuestion(owner, buyer.getWurmId());
        question.sendQuestion();
        String bml1 = removeSpecialCharacters.matcher(factory.getCommunicator(owner).lastBmlContent).replaceAll("");
        assertTrue(originalPattern.matcher(bml1).find());
        assertFalse(sortedPattern.matcher(bml1).find());

        Properties properties = new Properties();
        properties.setProperty("sort", "true");
        question.answer(properties);

        String bml2 = removeSpecialCharacters.matcher(factory.getCommunicator(owner).lastBmlContent).replaceAll("");
        assertTrue(sortedPattern.matcher(bml2).find());
    }

    @Test
    void testAcceptsDamagedSetInBML() {
        addItemToPriceList(1, 1, 1, true);
        addItemToPriceList(2, 1, 1, false);

        askQuestion();
        Matcher matcher1 = Pattern.compile("checkbox\\{id=\"\\dd\"}").matcher(factory.getCommunicator(owner).lastBmlContent);
        Matcher matcher2 = Pattern.compile("checkbox\\{id=\"\\dd\";selected=\"true\"}").matcher(factory.getCommunicator(owner).lastBmlContent);

        System.out.println(factory.getCommunicator(owner).lastBmlContent);
        assertTrue(matcher1.find());
        assertFalse(matcher1.find());
        assertTrue(matcher2.find());
        assertFalse(matcher2.find());
    }

    @Test
    void testRemainingToPurchaseNotRemovedWhenSettingToZero() throws PriceList.NoPriceListOnBuyer, PriceList.PageNotAdded, PriceList.PriceListFullException, EntryBuilder.EntryBuilderException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).remainingToPurchase(10).build();
        priceList.savePriceList();
        PriceList.Entry entry = priceList.iterator().next();

        askQuestion();
        answers = generateProperties(String.valueOf(1), entry.getWeight(), entry.getQualityLevel(), entry.getPrice(), 0, entry.getMinimumPurchase(), entry.acceptsDamaged());
        answer();

        priceList = PriceList.getPriceListFromBuyer(buyer);
        assertEquals(1, priceList.size());
        assertEquals(0, priceList.iterator().next().getRemainingToPurchase());
    }

    @Test
    void testAllIdsAddedToItemDetailsCorrectly() {
        addItemToPriceList(ItemList.backPack,1.0f,10);
        askQuestion();

        Matcher matcher = Pattern.compile("input\\{maxchars=\"\\d\"; id=\"\\d(\\w+)\"").matcher(com.lastBmlContent);

        String[] ids = { "weight", "q", "g", "s", "c", "i", "r", "p" };
        for (String str : ids) {
            assertTrue(matcher.find(), str + "\n" + com.lastBmlContent + "\n");
            assertEquals(str, matcher.group(1));
        }

        assertTrue(com.lastBmlContent.contains("checkbox{id=\"1d\"}"), com.lastBmlContent + "\n");
    }

    @Test
    void testRemainingLessThanMinimumMessage() throws EntryBuilder.EntryBuilderException, PriceList.PageNotAdded, PriceList.PriceListFullException, PriceList.NoPriceListOnBuyer {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).build();
        priceList.savePriceList();
        PriceList.Entry entry = priceList.iterator().next();

        askQuestion();
        answers = generateProperties(String.valueOf(1), entry.getWeight(), entry.getQualityLevel(), entry.getPrice(), 10, 20, entry.acceptsDamaged());
        answer();

        priceList = PriceList.getPriceListFromBuyer(buyer);
        assertEquals(1, priceList.size());
        assertThat(owner, receivedMessageContaining("Purchase limit is less"));
    }

    @Test
    void testRemainingLessThanMinimumMessageNotSentAtDefaultLevels() throws EntryBuilder.EntryBuilderException, PriceList.PageNotAdded, PriceList.PriceListFullException, PriceList.NoPriceListOnBuyer {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).build();
        priceList.savePriceList();
        PriceList.Entry entry = priceList.iterator().next();

        askQuestion();
        answers = generateProperties(String.valueOf(1), entry.getWeight(), entry.getQualityLevel(), entry.getPrice(), 0, 1, entry.acceptsDamaged());
        answer();

        priceList = PriceList.getPriceListFromBuyer(buyer);
        assertEquals(1, priceList.size());
        assertThat(owner, didNotReceiveMessageContaining("Purchase limit is less"));
    }
}