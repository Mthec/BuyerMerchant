package com.wurmonline.server.creatures;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.*;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static mod.wurmunlimited.Assert.containsCoinsOfValue;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BuyerHandler_NotOwnerTest extends WurmTradingTest {

    private BuyerHandler handler;
    private TradingWindow buyerWindow;
    private TradingWindow buyerOffer;
    private TradingWindow playerWindow;
    private TradingWindow playerOffer;

    private void createHandler() {
        makeBuyerTrade();
        handler = (BuyerHandler)buyer.getTradeHandler();
        buyerWindow = trade.getTradingWindow(1);
        buyerOffer = trade.getTradingWindow(3);
        playerWindow = trade.getTradingWindow(2);
        playerOffer = trade.getTradingWindow(4);
    }

    private void addOneCopperItemToPriceList(Item item) {
        try {
            PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
            priceList.addItem(item.getTemplateId(), item.getMaterial(), 1.0f, MonetaryConstants.COIN_COPPER);
            priceList.savePriceList();
            Shop shop = factory.getShop(buyer);
            shop.setMoney(shop.getMoney() + (long)(MonetaryConstants.COIN_COPPER * 1.1f));
        } catch (NoSuchTemplateException | IOException | PriceList.PriceListFullException e) {
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
        player.getInventory().getItems().forEach(playerWindow::addItem);
        handler.balance();

        assertEquals(items, playerWindow.getItems().length);
        assertEquals(0, playerOffer.getItems().length);
    }

    @Test
    void testSuckItems() {
        int items = 5;
        factory.createManyItems(items).forEach(player.getInventory()::insertItem);

        Item item = player.getInventory().getItems().iterator().next();
        addOneCopperItemToPriceList(item);

        createHandler();

        player.getInventory().getItems().forEach(playerWindow::addItem);
        handler.balance();

        assertEquals(0, playerWindow.getItems().length);
        assertEquals(items, trade.getCreatureTwoRequestWindow().getAllItems().length);
    }

    @Test
    void testDoesNotSuckUnauthorisedItems() {
        createHandler();
        int items = 5;
        factory.createManyItems(items).forEach(player.getInventory()::insertItem);
        player.getInventory().getItems().forEach(playerWindow::addItem);
        handler.balance();

        assertEquals(items, playerWindow.getItems().length);
        assertEquals(0, buyerOffer.getAllItems().length);
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
        player.getInventory().getItems().forEach(playerWindow::addItem);
        handler.balance();

        assertThat(player, receivedMessageContaining("cannot add more items"));
        assertEquals(1, playerWindow.getItems().length);
        assertEquals(0, playerOffer.getItems().length);
    }

    @Test
    void testWillExceedMaxItems() {
        factory.createManyItems(BuyerHandler.getMaxNumPersonalItems() - 1).forEach(buyer.getInventory()::insertItem);
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        player.getInventory().insertItem(factory.createNewItem());
        addOneCopperItemToPriceList(item);

        createHandler();
        handler.addItemsToTrade();
        handler.balance();
        assertFalse(receivedMessageContaining("cannot add more items").matches(player));

        player.getInventory().getItems().forEach(playerWindow::addItem);
        handler.tradeChanged();
        handler.balance();
        assertThat(player, receivedMessageContaining("cannot add more items"));
        assertEquals(1, playerWindow.getItems().length);
        assertEquals(1, playerOffer.getItems().length);
    }

    @Test
    void testNoDamagedItems() {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);
        item.setDamage(50.0f);

        createHandler();
        handler.addItemsToTrade();
        playerWindow.addItem(item);
        handler.balance();

        assertThat(player, receivedMessageContaining("don't accept damaged"));
    }

    @Test
    void testNoLockedItems() {
        Item item = factory.createNewItem(factory.getIsLockableId());
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);
        factory.lockItem(item);

        createHandler();
        handler.addItemsToTrade();
        playerWindow.addItem(item);
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
        playerWindow.addItem(hollow);
        handler.balance();

        assertThat(player, receivedMessageContaining("Please empty"));
    }

    @Test
    void testOldCoinsRemovedFromBuyerOffer() {
        createHandler();
        int items = 5;
        factory.createManyCopperCoins(items).forEach(buyerOffer::addItem);

        handler.balance();

        assertEquals(0, buyerOffer.getAllItems().length);
        assertEquals(0, buyerWindow.getAllItems().length);
    }

    @Test
    void testAbovePriceBelowBuyersTax() {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        addOneCopperItemToPriceList(item);
        factory.getShop(buyer).setMoney(101);

        createHandler();
        handler.addItemsToTrade();
        player.getInventory().getItems().forEach(playerWindow::addItem);
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
        player.getInventory().getItems().forEach(playerWindow::addItem);
        handler.balance();

        assertTrue(buyerIsSatisfied());
        assertEquals(1, buyerWindow.getItems().length);
        Stream.of(buyerOffer.getItems()).forEach(i -> assertTrue(i.isCoin()));
        assertEquals(MonetaryConstants.COIN_COPPER, Stream.of(buyerOffer.getItems()).mapToInt(i -> Economy.getValueFor(i.getTemplateId())).sum());

        assertEquals(0, playerWindow.getItems().length);
        assertEquals(1, playerOffer.getItems().length);
        assertEquals(player.getInventory().getItems().iterator().next(), playerOffer.getItems()[0]);
    }

    @Test
    void testStillBalancedAfterChange() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(item.getTemplateId(), item.getMaterial());
        priceList.savePriceList();
        factory.getShop(buyer).setMoney(100);

        createHandler();
        handler.addItemsToTrade();
        player.getInventory().getItems().forEach(playerOffer::addItem);
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
    void testNegativePriceAlwaysReturns0() throws PriceList.PriceListFullException, IOException, NoSuchTemplateException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        Item item = factory.createNewItem(factory.getIsWoodId());
        priceList.addItem(item.getTemplateId(), item.getMaterial());
        priceList.savePriceList();

        // Skip -1 as that value is used in PriceList as an invalid price string.
        for (int i = -2; i > -100; --i) {
            priceList.iterator().next().updateItemQLAndPrice(1.0f, i);
            priceList.savePriceList();
            createHandler();
            assertEquals(0, handler.getTraderBuyPriceForItem(item));
        }
    }

    @Test
    void testAcceptingDonatedItems() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        Item item1 = factory.createNewItem(factory.getIsWoodId());
        player.getInventory().insertItem(item1);
        priceList.addItem(item1.getTemplateId(), item1.getMaterial(), 1.0f, 0);
        priceList.savePriceList();

        createHandler();
        trade.getTradingWindow(2).addItem(item1);
        balance();

        assertThat(player, receivedMessageContaining("donation"));
        assertTrue(Arrays.asList(trade.getCreatureTwoRequestWindow().getItems()).contains(item1));
        assertThat(Arrays.asList(trade.getCreatureOneRequestWindow().getItems()), containsCoinsOfValue(0L));
    }

    @Test
    void testAcceptingDonatedItemsDoesNotOverridePrice() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        Item item1 = factory.createNewItem(factory.getIsWoodId());
        player.getInventory().insertItem(item1);
        priceList.addItem(item1.getTemplateId(), item1.getMaterial(), 1.0f, 0);
        Item item2 = factory.createNewItem(factory.getIsWoodId());
        item2.setQualityLevel(20.0f);
        player.getInventory().insertItem(item2);
        priceList.addItem(item2.getTemplateId(), item2.getMaterial(), item2.getQualityLevel() - 1, 10);
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
    void testUnauthorisedItemsNotAddedToWindow() throws PriceList.PriceListFullException, NoSuchTemplateException, IOException {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsWoodId(), (byte)0, 1.0f, -1);
        priceList.addItem(factory.getIsMetalId(), (byte)0, 1.0f, 10);
        priceList.savePriceList();

        createHandler();
        handler.addItemsToTrade();

        assertEquals(1, trade.getTradingWindow(1).getItems().length);
        assertNotEquals(factory.getIsWoodId(), trade.getTradingWindow(1).getItems()[0].getTemplateId());
    }
}
