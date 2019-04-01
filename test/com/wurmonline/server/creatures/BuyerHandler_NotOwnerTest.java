package com.wurmonline.server.creatures;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.*;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.EntryBuilder;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BuyerHandler_NotOwnerTest extends WurmTradingTest {

    private BuyerHandler handler;
    private TradingWindow buyerOffer;
    private TradingWindow buyerToTrade;
    private TradingWindow playerOffer;
    private TradingWindow playerToTrade;

    private void createHandler() {
        makeBuyerTrade();
        handler = (BuyerHandler)buyer.getTradeHandler();
        buyerOffer = trade.getTradingWindow(1);
        buyerToTrade = trade.getTradingWindow(3);
        playerOffer = trade.getTradingWindow(2);
        playerToTrade = trade.getTradingWindow(4);
    }

    private void addOneCopperItemToPriceList(Item item) {
        try {
            PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
            priceList.addItem(item.getTemplateId(), item.getMaterial(), -1, 1.0f, MonetaryConstants.COIN_COPPER);
            priceList.savePriceList();
            Shop shop = factory.getShop(buyer);
            shop.setMoney(shop.getMoney() + (long)(MonetaryConstants.COIN_COPPER * 1.1f));
        } catch (NoSuchTemplateException | IOException | PriceList.PriceListFullException | PriceList.PageNotAdded e) {
            throw new RuntimeException(e);
        }
    }

    private boolean buyerIsSatisfied() {
        try {
            Method isCreatureTwoSatisfied = BuyerTrade.class.getDeclaredMethod("isCreatureTwoSatisfied");
            isCreatureTwoSatisfied.setAccessible(true);
            return (boolean)isCreatureTwoSatisfied.invoke(trade);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBuyerItemsNotAddedToWindow() {
        createHandler();
        factory.createManyItems(5).forEach(buyer.getInventory()::insertItem);

        handler.addItemsToTrade();

        List<Item> items = Arrays.asList(trade.getTradingWindow(1).getAllItems());
        assertTrue(items.isEmpty());
    }

    @Test
    void testPriceListItemsAddedToWindow() {
        addOneCopperItemToPriceList(factory.createNewItem());
        createHandler();

        handler.addItemsToTrade();

        assertEquals(1, trade.getTradingWindow(1).getAllItems().length);
    }

    @SuppressWarnings("DuplicateExpressions")
    @Test
    void testPriceListItemsDestroyedOnEnd() throws NoSuchFieldException, IllegalAccessException {
        Item item1 = factory.createNewItem();
        Item item2 = factory.createNewItem(factory.getIsHollowId());
        Item item3 = factory.createBuyerContract();
        addOneCopperItemToPriceList(item1);
        addOneCopperItemToPriceList(item2);
        addOneCopperItemToPriceList(item3);

        createHandler();
        handler.addItemsToTrade();

        Field handlerList = BuyerHandler.class.getDeclaredField("priceList");
        handlerList.setAccessible(true);
        PriceList priceList = (PriceList)handlerList.get(handler);

        assertEquals(3, priceList.getItems().size());
        Iterator<TempItem> it = priceList.getItems().iterator();
        Item temp1 = it.next();
        Item temp2 = it.next();
        Item temp3 = it.next();
        assertDoesNotThrow(() -> Items.getItem(temp1.getWurmId()));
        assertDoesNotThrow(() -> Items.getItem(temp2.getWurmId()));
        assertDoesNotThrow(() -> Items.getItem(temp3.getWurmId()));

        handler.end();

        assertThrows(NoSuchItemException.class, () -> Items.getItem(temp1.getWurmId()));
        assertThrows(NoSuchItemException.class, () -> Items.getItem(temp2.getWurmId()));
        assertThrows(NoSuchItemException.class, () -> Items.getItem(temp3.getWurmId()));
    }

    // Suck Interesting Items.

    @Test
    void tesDoesNotSucksCoins() {
        createHandler();
        int items = 5;
        factory.createManyCopperCoins(items).forEach(player.getInventory()::insertItem);
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(items, playerOffer.getItems().length);
        assertEquals(0, playerToTrade.getItems().length);
    }

    @Test
    void testSuckItems() {
        int items = 5;
        factory.createManyItems(items).forEach(player.getInventory()::insertItem);

        Item item = player.getInventory().getItems().iterator().next();
        addOneCopperItemToPriceList(item);

        createHandler();

        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(0, playerOffer.getItems().length);
        assertEquals(items, trade.getCreatureTwoRequestWindow().getAllItems().length);
    }

    @Test
    void testDoesNotSuckUnauthorisedItems() {
        createHandler();
        int items = 5;
        factory.createManyItems(items).forEach(player.getInventory()::insertItem);
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(items, playerOffer.getItems().length);
        assertEquals(0, buyerToTrade.getAllItems().length);
    }

    // Balance

    @Test
    void testAlreadyExceededMaxItems() {
        factory.createManyItems(BuyerHandler.getMaxNumPersonalItems() + 1).forEach(buyer.getInventory()::insertItem);
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);

        createHandler();
        handler.addItemsToTrade();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertThat(player, receivedMessageContaining("cannot add more items"));
        assertEquals(1, playerOffer.getItems().length);
        assertEquals(0, playerToTrade.getItems().length);
    }

    @Test
    void testWillNotExceedMaxItems() {
        factory.createManyItems(BuyerHandler.getMaxNumPersonalItems() - 2).forEach(buyer.getInventory()::insertItem);
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        player.getInventory().insertItem(factory.createNewItem());
        addOneCopperItemToPriceList(item);

        createHandler();
        handler.addItemsToTrade();
        handler.balance();
        assertFalse(receivedMessageContaining("cannot add more items").matches(player));

        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.tradeChanged();
        handler.balance();
        assertThat(player, receivedMessageContaining("cannot add more items"));
        assertEquals(1, playerOffer.getItems().length);
        assertEquals(1, playerToTrade.getItems().length);
    }

    @Test
    void testDoesNotAcceptDamagedItemsIfNotSet() {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);
        item.setQualityLevel(100);
        item.setDamage(50.0f);

        createHandler();
        handler.addItemsToTrade();
        playerOffer.addItem(item);
        handler.balance();

        assertThat(player, receivedMessageContaining("don't accept damaged"));
    }

    @Test
    void testAcceptsDamagedItemsIfSet() throws PriceList.NoPriceListOnBuyer, PriceList.PageNotAdded, PriceList.PriceListFullException, EntryBuilder.EntryBuilderException {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        PriceList.Entry entry = priceList.iterator().next();
        EntryBuilder.update(entry).acceptsDamaged().build();
        priceList.savePriceList();
        item.setDamage(50.0f);

        createHandler();
        handler.addItemsToTrade();
        playerOffer.addItem(item);
        handler.balance();

        assertThat(player, didNotReceiveMessageContaining("don't accept damaged"));
    }

    @Test
    void testDoesNotAcceptDamagedItemsIfRepairedQLIsUnderMinimumButUn_repairedIsOver() throws PriceList.NoPriceListOnBuyer, PriceList.PageNotAdded, PriceList.PriceListFullException, EntryBuilder.EntryBuilderException {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        PriceList.Entry entry = priceList.iterator().next();
        EntryBuilder.update(entry).ql(20).acceptsDamaged().build();
        priceList.savePriceList();
        item.setQualityLevel(30);
        item.setDamage(50.0f);
        assert item.getCurrentQualityLevel() < 20;

        createHandler();
        handler.addItemsToTrade();
        playerOffer.addItem(item);
        handler.balance();

        assertEquals(1, trade.getTradingWindow(2).getItems().length);
        assertEquals(0, trade.getTradingWindow(4).getItems().length);
    }

    @Test
    void testNoLockedItems() {
        Item item = factory.createNewItem(factory.getIsLockableId());
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);
        factory.lockItem(item);

        createHandler();
        handler.addItemsToTrade();
        playerOffer.addItem(item);
        handler.balance();

        assertThat(player, receivedMessageContaining("don't accept locked"));
    }

    @Test
    void testFilledHollowItems() {
        Item hollow = factory.createNewItem(factory.getIsHollowId());
        Item contained = factory.createNewItem();
        hollow.insertItem(contained, true);
        player.getInventory().insertItem(hollow);
        addOneCopperItemToPriceList(hollow);
        addOneCopperItemToPriceList(contained);

        createHandler();
        handler.addItemsToTrade();
        playerOffer.addItem(hollow);
        handler.balance();

        assertThat(player, receivedMessageContaining("Please empty"));
    }

    @Test
    void testOldCoinsRemovedFromBuyerOffer() {
        createHandler();
        int items = 5;
        factory.createManyCopperCoins(items).forEach(buyerToTrade::addItem);

        handler.balance();

        assertEquals(0, buyerToTrade.getAllItems().length);
        assertEquals(0, buyerOffer.getAllItems().length);
    }

    @Test
    void testAbovePriceBelowBuyersTax() {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);
        factory.getShop(buyer).setMoney(101);

        createHandler();
        handler.addItemsToTrade();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertThat(player, receivedMessageContaining("low on cash"));
        assertFalse(buyerIsSatisfied());
    }

    @Test
    void testCoinsBalanceTrade() {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);

        createHandler();
        handler.addItemsToTrade();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertTrue(buyerIsSatisfied());
        assertEquals(1, buyerOffer.getItems().length);
        Stream.of(buyerToTrade.getItems()).forEach(i -> assertTrue(i.isCoin()));
        assertEquals(MonetaryConstants.COIN_COPPER, Stream.of(buyerToTrade.getItems()).mapToInt(i -> Economy.getValueFor(i.getTemplateId())).sum());

        assertEquals(0, playerOffer.getItems().length);
        assertEquals(1, playerToTrade.getItems().length);
        assertEquals(player.getInventory().getItems().iterator().next(), playerToTrade.getItems()[0]);
    }

    @Test
    void testStillBalancedAfterChange() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(item.getTemplateId(), item.getMaterial());
        priceList.savePriceList();
        factory.getShop(buyer).setMoney(100);

        createHandler();
        handler.addItemsToTrade();
        player.getInventory().getItems().forEach(playerToTrade::addItem);
        handler.balance();
        assert buyerIsSatisfied();

        handler.tradeChanged();
        handler.balance();

        assertTrue(buyerIsSatisfied());
    }

    @Test
    void testPriceOnLowWeightItems() {
        Item fullWeight = factory.createNewItem();
        addOneCopperItemToPriceList(fullWeight);

        Item halfWeight = factory.createNewItem();
        halfWeight.setWeight(halfWeight.getTemplate().getWeightGrams() / 2, false);

        Item minimalWeight = factory.createNewItem();
        minimalWeight.setWeight(1, false);

        createHandler();
        handler.getTraderBuyPriceForItem(halfWeight);

        assertAll(
                () -> assertEquals(MonetaryConstants.COIN_COPPER, handler.getTraderBuyPriceForItem(fullWeight)),
                () -> assertEquals(MonetaryConstants.COIN_COPPER / 2, handler.getTraderBuyPriceForItem(halfWeight)),
                () -> assertEquals(0, handler.getTraderBuyPriceForItem(minimalWeight))
        );
    }

    @Test
    void testPriceOnLowWeightItemsWithCustomWeight() throws PriceList.NoPriceListOnBuyer, PriceList.PriceListFullException, PriceList.PageNotAdded, EntryBuilder.EntryBuilderException {
        Item fullWeight = factory.createNewItem();
        addOneCopperItemToPriceList(fullWeight);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        PriceList.Entry entry = priceList.iterator().next();
        EntryBuilder.update(entry).weight(fullWeight.getWeightGrams() / 2).build();
        priceList.savePriceList();

        Item halfWeight = factory.createNewItem();
        halfWeight.setWeight(halfWeight.getTemplate().getWeightGrams() / 2, false);

        Item minimalWeight = factory.createNewItem();
        minimalWeight.setWeight(1, false);

        createHandler();
        //handler.getTraderBuyPriceForItem(halfWeight);

        assertAll(
                () -> assertEquals(MonetaryConstants.COIN_COPPER * 2, handler.getTraderBuyPriceForItem(fullWeight)),
                () -> assertEquals(MonetaryConstants.COIN_COPPER, handler.getTraderBuyPriceForItem(halfWeight)),
                () -> assertEquals(0, handler.getTraderBuyPriceForItem(minimalWeight))
        );
    }

    @Test
    void testPriceForDamagedItemsReturnsCorrectValue() throws IOException, PriceList.PageNotAdded, PriceList.PriceListFullException, EntryBuilder.EntryBuilderException {
        Item acceptedDamage = factory.createNewItem(1);
        acceptedDamage.setQualityLevel(2);
        acceptedDamage.setDamage(50);
        Item unacceptedDamage = factory.createNewItem(2);
        unacceptedDamage.setDamage(50);
        addOneCopperItemToPriceList(unacceptedDamage);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(acceptedDamage.getTemplateId()).material(acceptedDamage.getMaterial()).price(MonetaryConstants.COIN_COPPER).acceptsDamaged().build();
        priceList.savePriceList();
        assert priceList.getEntryFor(acceptedDamage) != null;

        createHandler();

        assertEquals(MonetaryConstants.COIN_COPPER, handler.getTraderBuyPriceForItem(acceptedDamage));
        assertEquals(0, handler.getTraderBuyPriceForItem(unacceptedDamage));
    }

    @Test
    void testNegativePriceAlwaysReturns0() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, NoSuchTemplateException, EntryBuilder.EntryBuilderException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        Item item = factory.createNewItem(factory.getIsWoodId());
        priceList.addItem(item.getTemplateId(), item.getMaterial());
        priceList.savePriceList();

        // Skip -1 as that value is used in PriceList as an invalid price string.
        for (int i = -2; i > -100; --i) {
            EntryBuilder.update(priceList.iterator().next()).price(i).build();
            priceList.savePriceList();
            createHandler();
            assertEquals(0, handler.getTraderBuyPriceForItem(item));
        }
    }

    @Test
    void testAcceptingDonatedItems() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        Item item1 = factory.createNewItem(factory.getIsWoodId());
        player.getInventory().insertItem(item1);
        priceList.addItem(item1.getTemplateId(), item1.getMaterial(), -1, 1.0f, 0);
        priceList.savePriceList();

        createHandler();
        trade.getTradingWindow(2).addItem(item1);
        balance();

        assertThat(player, receivedMessageContaining("donation"));
        assertTrue(Arrays.asList(trade.getCreatureTwoRequestWindow().getItems()).contains(item1));
        assertThat(Arrays.asList(trade.getCreatureOneRequestWindow().getItems()), containsCoinsOfValue(0L));
    }

    @Test
    void testAcceptingDonatedItemsDoesNotOverridePrice() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        Item item1 = factory.createNewItem(factory.getIsWoodId());
        player.getInventory().insertItem(item1);
        priceList.addItem(item1.getTemplateId(), item1.getMaterial(), -1, 1.0f, 0);
        Item item2 = factory.createNewItem(factory.getIsWoodId());
        item2.setQualityLevel(20.0f);
        player.getInventory().insertItem(item2);
        priceList.addItem(item2.getTemplateId(), item2.getMaterial(), -1, item2.getQualityLevel() - 1, 10);
        priceList.savePriceList();
        factory.getShop(buyer).setMoney(100);

        createHandler();
        trade.getTradingWindow(2).addItem(item1);
        trade.getTradingWindow(2).addItem(item2);
        balance();

        assertThat(player, receivedMessageContaining("donation"));
        assertTrue(Arrays.asList(trade.getCreatureTwoRequestWindow().getItems()).contains(item1));
        assertTrue(Arrays.asList(trade.getCreatureTwoRequestWindow().getItems()).contains(item2));
        assertThat(Arrays.asList(trade.getCreatureOneRequestWindow().getItems()), containsCoinsOfValue(10L));
    }

    @Test
    void testUnauthorisedItemsNotAddedToWindow() throws PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException, IOException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsWoodId(), (byte)0, -1, 1.0f, -1);
        priceList.addItem(factory.getIsMetalId(), (byte)0, -1, 1.0f, 10);
        priceList.savePriceList();

        createHandler();
        handler.addItemsToTrade();

        assertEquals(1, trade.getTradingWindow(1).getItems().length);
        assertNotEquals(factory.getIsWoodId(), trade.getTradingWindow(1).getItems()[0].getTemplateId());
    }

    @Test
    void testFilledReedPen() {
        Item reedPen = factory.createNewItem(ItemTemplateFactory.getInstance().getTemplate("reed pen").getTemplateId());
        // Black ink.
        factory.fillItemWith(reedPen, 753);
        addOneCopperItemToPriceList(reedPen);

        createHandler();
        playerOffer.addItem(reedPen);
        handler.balance();

        assertThat(player, receivedMessageContaining("Please empty"));
        assertEquals(1, playerOffer.getItems().length);
        assertEquals(0, playerToTrade.getItems().length);
    }

    @Test
    void testMinimumPurchaseItemsAreLabelledSo() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, EntryBuilder.EntryBuilderException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).price(10).minimumRequired(1).build();
        EntryBuilder.addEntry(priceList).price(10).minimumRequired(100).build();
        priceList.savePriceList();

        createHandler();
        handler.addItemsToTrade();

        assertEquals(2, trade.getTradingWindow(1).getItems().length);
        List<Item> items = Arrays.asList(trade.getTradingWindow(1).getItems());
        items.sort(Comparator.comparing(Item::getName));
        assertTrue(items.get(0).getName().endsWith("any"));
        assertTrue(items.get(1).getName().endsWith("any - minimum " + 100));
    }

    @Test
    void testMinimumPurchaseReached() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, EntryBuilder.EntryBuilderException {
        int minimumPurchase = 20;
        int numberOfItems = 20;
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).price(10).minimumRequired(minimumPurchase).build();
        priceList.savePriceList();

        createHandler();
        factory.getShop(buyer).setMoney((long)(10 * numberOfItems * 1.1f));
        List<Item> items = new ArrayList<>();
        factory.createManyItems(factory.getIsWoodId(), numberOfItems).iterator().forEachRemaining(items::add);
        items.forEach(player.getInventory()::insertItem);
        items.forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(numberOfItems, playerToTrade.getItems().length);
        assertEquals(0, playerOffer.getItems().length);
        assertFalse(receivedMessageContaining("do not have enough space").matches(player));

        setSatisfied(player);
        assertThat(player, receivedMessageContaining("completed successfully"));
    }

    @Test
    void testMinimumPurchaseNotReached() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, EntryBuilder.EntryBuilderException {
        int minimumPurchase = 20;
        int numberOfItems = minimumPurchase - 1;
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).price(10).minimumRequired(minimumPurchase).build();
        priceList.savePriceList();

        createHandler();
        factory.createManyItems(factory.getIsWoodId(), numberOfItems).forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(numberOfItems, playerOffer.getItems().length);
        assertEquals(0, playerToTrade.getItems().length);
        assertThat(player, receivedMessageContaining("will need " + (minimumPurchase - numberOfItems) + " more"));
    }

    @Test
    void testMinimumPurchaseExceeded() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, EntryBuilder.EntryBuilderException {
        int minimumPurchase = 20;
        int numberOfItems = minimumPurchase + 1;
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).price(10).minimumRequired(minimumPurchase).build();
        priceList.savePriceList();

        createHandler();
        factory.getShop(buyer).setMoney((long)(10 * numberOfItems * 1.1f));
        List<Item> items = new ArrayList<>();
        factory.createManyItems(factory.getIsWoodId(), numberOfItems).iterator().forEachRemaining(items::add);
        items.forEach(player.getInventory()::insertItem);
        items.forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(numberOfItems, playerToTrade.getItems().length);
        assertEquals(0, playerOffer.getItems().length);

        setSatisfied(player);
        assertThat(player, receivedMessageContaining("completed successfully"));
    }

    @Test
    void testMinimumPurchaseExceedsSpaceButPurchasesSome() throws PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException, IOException, EntryBuilder.EntryBuilderException {
        int minimumPurchase = 20;
        int numberOfItems = minimumPurchase * 2;
        BuyerHandler.maxPersonalItems = (int)(minimumPurchase * 1.5f);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).price(10).minimumRequired(minimumPurchase).build();
        priceList.savePriceList();

        createHandler();
        long initialFunds = (long)(10 * numberOfItems * 1.1f);
        factory.getShop(buyer).setMoney(initialFunds);
        List<Item> items = new ArrayList<>();
        factory.createManyItems(factory.getIsWoodId(), numberOfItems).iterator().forEachRemaining(items::add);
        items.forEach(player.getInventory()::insertItem);
        items.forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(BuyerHandler.maxPersonalItems - 1, playerToTrade.getItems().length);
        assertEquals(numberOfItems - BuyerHandler.maxPersonalItems + 1, playerOffer.getItems().length);

        setSatisfied(player);
        assertThat(player, receivedMessageContaining("to accept all of the " + ItemTemplateFactory.getInstance().getTemplate(factory.getIsWoodId()).getPlural()));
        assertThat(player, receivedMessageContaining("completed successfully"));

        assertEquals(BuyerHandler.maxPersonalItems, buyer.getInventory().getItemCount());
        assertEquals(initialFunds - (10 * (BuyerHandler.maxPersonalItems - 1) * 1.1f), factory.getShop(buyer).getMoney());
        assertEquals(numberOfItems - BuyerHandler.maxPersonalItems + 1, player.getInventory().getNumItemsNotCoins());
    }

    @Test
    void testOverMaximumAddedSeparately() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, EntryBuilder.EntryBuilderException {
        int minimumPurchase = 20;
        int numberOfItems = minimumPurchase * 2;
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).price(10).minimumRequired(minimumPurchase).build();
        priceList.savePriceList();

        createHandler();
        long initialFunds = (long)(10 * numberOfItems * 1.1f);
        factory.getShop(buyer).setMoney(initialFunds);
        List<Item> items = new ArrayList<>();
        factory.createManyItems(factory.getIsWoodId(), numberOfItems).iterator().forEachRemaining(items::add);
        items.forEach(player.getInventory()::insertItem);
        List<Item> items1 = items.subList(0, 21);
        List<Item> items2 = items.subList(21, items.size());
        items1.forEach(playerOffer::addItem);
        handler.balance();
        items2.forEach(playerOffer::addItem);
        handler.addToInventory(items2.get(0), playerOffer.getWurmId());
        handler.balance();

        assertEquals(numberOfItems, playerToTrade.getItems().length);
        assertEquals(0, playerOffer.getItems().length);

        setSatisfied(player);
        assertFalse(receivedMessageContaining("will need").matches(player));
        assertThat(player, receivedMessageContaining("completed successfully"));

        assertEquals(numberOfItems + 1, buyer.getInventory().getItemCount());
        assertEquals(0, factory.getShop(buyer).getMoney());
        assertEquals(0, player.getInventory().getNumItemsNotCoins());
        assertThat(player, hasCoinsOfValue((long)(10 * numberOfItems)));
    }

    @Test
    void testSomeMinimumPurchaseItemsRemovedFromTrade() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, IllegalAccessException, NoSuchFieldException, EntryBuilder.EntryBuilderException {
        int minimumPurchase = 20;
        int numberOfItems = minimumPurchase * 2;
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).price(10).minimumRequired(minimumPurchase).build();
        priceList.savePriceList();

        createHandler();
        long initialFunds = (long)(10 * numberOfItems * 1.1f);
        factory.getShop(buyer).setMoney(initialFunds);
        List<Item> items = new ArrayList<>();
        factory.createManyItems(factory.getIsWoodId(), numberOfItems).iterator().forEachRemaining(items::add);
        items.forEach(player.getInventory()::insertItem);
        List<Item> items1 = items.subList(0, 21);
        List<Item> items2 = items.subList(21, items.size());
        assert items1.size() >= minimumPurchase;
        assert items2.size() < minimumPurchase;
        items1.forEach(playerOffer::addItem);
        handler.balance();
        Field creatureTwoSatisfied = BuyerTrade.class.getDeclaredField("creatureTwoSatisfied");
        creatureTwoSatisfied.setAccessible(true);
        assertTrue(creatureTwoSatisfied.getBoolean(trade));

        items1.forEach(playerToTrade::removeItem);
        items2.forEach(playerOffer::addItem);
        handler.addToInventory(items2.get(0), playerOffer.getWurmId());
        handler.balance();

        assertEquals(0, playerToTrade.getItems().length);
        assertEquals(items2.size(), playerOffer.getItems().length);
        assertTrue(Arrays.stream(playerOffer.getItems()).noneMatch(items1::contains));

        setSatisfied(player);
        assertThat(player, receivedMessageContaining("will need"));

        assertEquals(1, buyer.getInventory().getItemCount());
        assertEquals(initialFunds, factory.getShop(buyer).getMoney());
        assertEquals(items.size(), player.getInventory().getNumItemsNotCoins());
        assertThat(player, hasCoinsOfValue(0L));
    }

    @Test
    void testMultipleMinimumPurchasesAtDifferentQLs() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, EntryBuilder.EntryBuilderException {
        int minimumPurchase = 10;
        int numberOfItems = minimumPurchase * 3;
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).price(10).minimumRequired(minimumPurchase * 2).build();
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).ql(25).price(20).minimumRequired(minimumPurchase).build();
        priceList.savePriceList();

        createHandler();
        long initialFunds = (long)(10 * (minimumPurchase * 2) * 1.1f) + (long)(20 * minimumPurchase * 1.1f);
        factory.getShop(buyer).setMoney(initialFunds);
        List<Item> items = new ArrayList<>();
        factory.createManyItems(factory.getIsWoodId(), minimumPurchase * 2).iterator().forEachRemaining(items::add);
        factory.createManyItems(factory.getIsWoodId(), minimumPurchase).iterator().forEachRemaining(item -> {
            item.setQualityLevel(26);
            items.add(item);
        });
        items.forEach(player.getInventory()::insertItem);
        items.forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(numberOfItems, playerToTrade.getItems().length);
        assertEquals(0, playerOffer.getItems().length);

        setSatisfied(player);
        assertFalse(receivedMessageContaining("will need").matches(player));
        assertThat(player, receivedMessageContaining("completed successfully"));

        assertEquals(numberOfItems + 1, buyer.getInventory().getItemCount());
        assertEquals(0, factory.getShop(buyer).getMoney());
        assertEquals(0, player.getInventory().getNumItemsNotCoins());
        assertThat(player, hasCoinsOfValue((long)(10 * minimumPurchase * 2) + (long)(20 * minimumPurchase)));
    }

    @Test
    void testNoMessageSentAfterAllMinimumPurchaseEntryItemsRemoved() throws PriceList.PriceListFullException, PriceList.PageNotAdded, IOException, NoSuchFieldException, IllegalAccessException, EntryBuilder.EntryBuilderException {
        int minimumPurchase = 20;
        int numberOfItems = minimumPurchase * 2;
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).price(10).minimumRequired(minimumPurchase).build();
        priceList.savePriceList();

        createHandler();
        long initialFunds = (long)(10 * numberOfItems * 1.1f);
        factory.getShop(buyer).setMoney(initialFunds);
        List<Item> items = new ArrayList<>();
        factory.createManyItems(factory.getIsWoodId(), numberOfItems).iterator().forEachRemaining(items::add);
        items.forEach(player.getInventory()::insertItem);
        items.forEach(playerOffer::addItem);
        handler.balance();
        Field creatureTwoSatisfied = BuyerTrade.class.getDeclaredField("creatureTwoSatisfied");
        creatureTwoSatisfied.setAccessible(true);
        assertTrue(creatureTwoSatisfied.getBoolean(trade));

        items.forEach(playerToTrade::removeItem);
        handler.addToInventory(items.get(0), playerOffer.getWurmId());
        handler.balance();

        assertEquals(0, playerToTrade.getItems().length);
        assertEquals(0, playerOffer.getItems().length);

        setSatisfied(player);
        assertFalse(receivedMessageContaining("will need").matches(player));

        assertEquals(1, buyer.getInventory().getItemCount());
        assertEquals(initialFunds, factory.getShop(buyer).getMoney());
        assertEquals(items.size(), player.getInventory().getNumItemsNotCoins());
        assertThat(player, hasCoinsOfValue(0L));
    }

    @Test
    void testDoesNotAcceptUnauthorisedAsDonation() {
        Item unauthorised = factory.createNewItem();
        player.getInventory().insertItem(unauthorised);

        createHandler();
        playerOffer.addItem(unauthorised);
        handler.balance();

        assertFalse(receivedMessageContaining("donation").matches(player));
        assertEquals(1, playerOffer.getItems().length);
        assertEquals(unauthorised, playerOffer.getItems()[0]);
    }

    @Test
    void testMinimumRequirementItemsMustBeFullWeight() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, EntryBuilder.EntryBuilderException {
        int minimumPurchase = 20;
        int numberOfItems = minimumPurchase + 1;
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(factory.getIsWoodId()).price(10).minimumRequired(minimumPurchase).build();
        priceList.savePriceList();
        factory.getShop(buyer).setMoney(100000);

        createHandler();
        factory.createManyItems(factory.getIsWoodId(), numberOfItems).forEach(playerOffer::addItem);
        Item underweight = playerOffer.getItems()[0];
        underweight.setWeight(underweight.getTemplate().getWeightGrams() - 1, false);
        handler.balance();

        assertEquals(1, playerOffer.getItems().length);
        assertEquals(numberOfItems - 1, playerToTrade.getItems().length);
        assertSame(underweight, playerOffer.getItems()[0]);
        assertThat(player, receivedMessageContaining("full weight"));
    }
}
