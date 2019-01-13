package com.wurmonline.server.items;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.behaviours.MethodsCreatures;
import com.wurmonline.server.creatures.BuyerHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.PriceList;
import mod.wurmunlimited.buyermerchant.PriceListTest;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.FieldSetter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BuyerTradeTest extends WurmTradingTest {

    private Item[] makeTrade() {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        Item coin1 = factory.createNewCopperCoin();
        Item coin2 = factory.createNewCopperCoin();
        Item coin3 = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(coin1);
        buyer.getInventory().insertItem(coin2);
        factory.getShop(buyer).setMoney(200L);

        TradingWindow windowOne = trade.getCreatureOneRequestWindow();
        windowOne.startReceivingItems();
        windowOne.addItem(coin3);
        windowOne.stopReceivingItems();
        TradingWindow windowTwo = trade.getCreatureTwoRequestWindow();
        windowTwo.startReceivingItems();
        windowTwo.addItem(item);
        windowTwo.stopReceivingItems();

        setSatisfied(player);
        setSatisfied(buyer);

        return new Item[] { coin1, coin2, coin3, item };
    }

    @Test
    void testGetTradingWindowReturnsBuyerTradingWindow() {
        makeBuyerTrade();

        TradingWindow win1 = trade.getTradingWindow(Trade.OFFERWINTWO);
        TradingWindow win2 = trade.getTradingWindow(Trade.OFFERWINONE);
        TradingWindow win3 = trade.getTradingWindow(Trade.REQUESTWINONE);
        TradingWindow win4 = trade.getTradingWindow(Trade.REQUESTWINTWO);

        assertTrue(win1 instanceof BuyerTradingWindow);
        assertTrue(win2 instanceof BuyerTradingWindow);
        assertTrue(win3 instanceof BuyerTradingWindow);
        assertTrue(win4 instanceof BuyerTradingWindow);
    }

    @Test
    void testTradeClearedFromCreatureOnEnd() {
        makeBuyerTrade();
        trade.end(player, true);

        assertNull(player.getTrade());
        assertNull(buyer.getTrade());
    }

    @Test
    void testNotImplementedMethods() {
        makeBuyerTrade();
        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, () -> trade.setMoneyAdded(1)),
                () -> assertThrows(UnsupportedOperationException.class, () -> trade.addShopDiff(1)),
                () -> assertThrows(UnsupportedOperationException.class, () -> trade.getMoneyAdded())
        );
    }

    // TODO - Run normal Trade object and compare server messages?
    @Test
    void testCreaturesSatisfied() {
        makeBuyerTrade();

        trade.setCreatureOneSatisfied(true);
        assertTrue(trade.isCreatureOneSatisfied());
        trade.setCreatureTwoSatisfied(true);
        assertTrue(trade.isCreatureTwoSatisfied());
    }

    @Test
    void testSetSatisfied() {
        makeBuyerTrade();

        setSatisfied(player);
        assertTrue(trade.isCreatureOneSatisfied());
        setSatisfied(buyer);
        assertTrue(trade.isCreatureTwoSatisfied());
    }

    @Test
    void testTradingWindowsClosed() {
        makeBuyerTrade();

        setSatisfied(player);
        assertFalse(factory.getCommunicator(player).tradeWindowClosed);
        assertFalse(factory.getCommunicator(buyer).tradeWindowClosed);

        trade.setSatisfied(player, false, trade.getCurrentCounter());
        setSatisfied(buyer);
        assertFalse(factory.getCommunicator(player).tradeWindowClosed);
        assertFalse(factory.getCommunicator(buyer).tradeWindowClosed);

        setSatisfied(player);
        assertTrue(factory.getCommunicator(player).tradeWindowClosed);
        assertTrue(factory.getCommunicator(buyer).tradeWindowClosed);
    }

    @Test
    void testCreatureAgrees() {
        makeBuyerTrade();
        setSatisfied(player);
        assertTrue(factory.getCommunicator(player).tradeAgreed);
        assertNull(factory.getCommunicator(buyer).tradeAgreed);

        trade.setSatisfied(player, false, trade.getCurrentCounter());
        setSatisfied(buyer);
        assertFalse(factory.getCommunicator(player).tradeAgreed);
        assertTrue(factory.getCommunicator(buyer).tradeAgreed);
    }

    @Test
    void testCannotCarryThatMuch() throws NoSuchFieldException {
        Item item = factory.createNewItem();
        FieldSetter.setField(item, Item.class.getDeclaredField("weight"), 500000000);
        makeBuyerTrade();
        player.setTrade(trade);
        buyer.setTrade(trade);
        buyer.getInventory().insertItem(item);
        trade.getCreatureOneRequestWindow().addItem(item);
        setSatisfied(player);
        setSatisfied(buyer);

        assertThat(player, receivedMessageContaining("cannot carry that much"));
        assertThat(buyer, receivedMessageContaining("cannot carry that much"));

        assertFalse(factory.getCommunicator(player).tradeWindowClosed);
        assertFalse(factory.getCommunicator(buyer).tradeWindowClosed);
    }

    @Test
    void testMoneyTransferredToPlayer() {
        makeBuyerTrade();
        Item coin, item;
        Item[] items = makeTrade();
        coin = items[2];
        item = items[3];

        assertThat(player, hasCoinsOfValue(100L));
        assertTrue(player.getInventory().getItems().contains(coin));
        assertFalse(player.getInventory().getItems().contains(item));

        assertThat(buyer, hasCoinsOfValue(90L));
        assertTrue(buyer.getInventory().getItems().contains(item));
    }

    @Test
    void testMoneyTransferredToOwner() {
        makeOwnerBuyerTrade();

        Item item = factory.createNewItem();
        owner.getInventory().insertItem(item);
        Item coin1 = factory.createNewCopperCoin();
        Item coin2 = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(coin1);
        buyer.getInventory().insertItem(coin2);

        trade.getCreatureOneRequestWindow().addItem(coin1);
        trade.getCreatureTwoRequestWindow().addItem(item);

        setSatisfied(owner);
        setSatisfied(buyer);

        assertThat(owner, hasCoinsOfValue(100L));
        assertFalse(owner.getInventory().getItems().contains(item));

        assertThat(buyer, hasCoinsOfValue(100L));
        assertTrue(buyer.getInventory().getItems().contains(item));
    }

    @Test
    void testMoneyTransferredToPlayerTradeVersion() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        player = factory.createNewPlayer();
        buyer = factory.createNewBuyer(factory.createNewPlayer());
        factory.getShop(buyer).setToNotPersonalTrader();
        Method initiateTrade = MethodsCreatures.class.getDeclaredMethod("initiateTrade", Creature.class, Creature.class);
        initiateTrade.setAccessible(true);
        initiateTrade.invoke(null, player, buyer);
        trade = player.getTrade();

        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        Item coin1 = factory.createNewCopperCoin();
        Item coin2 = factory.createNewCopperCoin();
        Item coin3 = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(coin1);
        buyer.getInventory().insertItem(coin2);

        trade.getCreatureOneRequestWindow().addItem(coin3);
        trade.getCreatureTwoRequestWindow().addItem(item);
        trade.setMoneyAdded(100L);

        setSatisfied(player);
        setSatisfied(buyer);

        assertThat(player, hasCoinsOfValue(100L));
        assertFalse(player.getInventory().getItems().contains(item));

        assertThat(player, hasCoinsOfValue(100L));
        assertTrue(buyer.getInventory().getItems().contains(item));
    }

    @Test
    void testMoneyTransferredToPlayerTradeMerchantVersion() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        player = factory.createNewPlayer();
        buyer = factory.createNewBuyer(factory.createNewPlayer());
        Method initiateTrade = MethodsCreatures.class.getDeclaredMethod("initiateTrade", Creature.class, Creature.class);
        initiateTrade.setAccessible(true);
        initiateTrade.invoke(null, player, buyer);
        trade = player.getTrade();

        Item item = factory.createNewItem();
        buyer.getInventory().insertItem(item);
        Item coin1 = factory.createNewCopperCoin();
        Item coin2 = factory.createNewCopperCoin();
        player.getInventory().insertItem(coin1);
        player.getInventory().insertItem(coin2);

        TradingWindow windowOne = trade.getCreatureOneRequestWindow();
        windowOne.startReceivingItems();
        windowOne.addItem(item);
        windowOne.stopReceivingItems();
        TradingWindow windowTwo = trade.getCreatureTwoRequestWindow();
        windowTwo.startReceivingItems();
        windowTwo.addItem(coin1);
        windowTwo.stopReceivingItems();
        trade.setMoneyAdded(100L);

        setSatisfied(player);
        setSatisfied(buyer);

        assertThat(player, hasCoinsOfValue(100L));
        assertTrue(player.getInventory().getItems().contains(item));

        assertThat(buyer, hasCoinsOfValue(90L));
        assertFalse(buyer.getInventory().getItems().contains(item));
    }

    @Test
    void testMoneyTransferredToPlayerTradeMerchantFullVersion() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        player = factory.createNewPlayer();
        buyer = factory.createNewBuyer(factory.createNewPlayer());
        Method initiateTrade = MethodsCreatures.class.getDeclaredMethod("initiateTrade", Creature.class, Creature.class);
        initiateTrade.setAccessible(true);
        initiateTrade.invoke(null, player, buyer);
        trade = player.getTrade();

        Item item = factory.createNewItem();
        item.setPrice(100);
        buyer.getInventory().insertItem(item);
        Item coin1 = factory.createNewCopperCoin();
        Item coin2 = factory.createNewCopperCoin();
        player.getInventory().insertItem(coin1);
        player.getInventory().insertItem(coin2);

        TradingWindow windowOne = trade.getCreatureOneRequestWindow();
        windowOne.startReceivingItems();
        windowOne.addItem(item);
        windowOne.stopReceivingItems();
        TradingWindow windowTwo = trade.getTradingWindow(2);
        windowTwo.startReceivingItems();
        windowTwo.addItem(coin1);
        windowTwo.stopReceivingItems();

        balance();
        assertSame(trade.getCreatureTwoRequestWindow().getItems()[0], coin1);
        setSatisfied(player);

        assertThat(player, hasCoinsOfValue(100L));
        assertTrue(player.getInventory().getItems().contains(item));

        assertThat(buyer, hasCoinsOfValue(90L));
        assertFalse(buyer.getInventory().getItems().contains(item));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testNoPriceListOnBuyer() {
        ItemsPackageFactory.removeItem(buyer, buyer.getInventory().getItems().stream().filter(PriceList::isPriceList).findFirst().get());
        assertThrows(PriceList.NoPriceListOnBuyer.class, () -> new BuyerTrade(player, buyer));
    }

    private void invalidCreatureWindow(Creature creature, TradingWindow window) {
        Item item = factory.createNewItem();
        creature.getInventory().insertItem(item);
        assert creature.getWurmId() != 1010101;
        item.setOwnerId(1010101);

        window.addItem(item);

        setSatisfied(player);
        setSatisfied(buyer);

        assertTrue(factory.getCommunicator(player).tradeAgreed);
        assertFalse(factory.getCommunicator(player).tradeWindowClosed);
        assertTrue(factory.getCommunicator(buyer).tradeAgreed);
        assertFalse(factory.getCommunicator(buyer).tradeWindowClosed);
    }

    @Test
    void testInvalidCreatureOneWindow() {
        makeBuyerTrade();
        invalidCreatureWindow(player, trade.getCreatureOneRequestWindow());
    }

    @Test
    void testInvalidCreatureTwoWindow() {
        makeBuyerTrade();
        invalidCreatureWindow(buyer, trade.getCreatureTwoRequestWindow());
    }

    @Test
    void testTradeSetToNullOnCompletion() {
        makeBuyerTrade();
        makeTrade();

        assertNull(player.getTrade());
        assertNull(buyer.getTrade());
    }

    @Test
    void testNotTradeWhenPlayerDead() {
        makeBuyerTrade();

        player.getSaveFile().dead = true;

        makeTrade();

        assertThat(player, receivedMessageContaining("not trade right now"));
        assertNull(player.getTrade());
        assertNull(buyer.getTrade());
    }

    @Test
    void testNotTradeWhenBuyerDead() throws IOException {
        makeBuyerTrade();

        buyer.getStatus().setDead(true);

        makeTrade();

        assertThat(player, receivedMessageContaining("not trade right now"));
        assertNull(player.getTrade());
        assertNull(buyer.getTrade());
    }

    @Test
    void testNotTradeWhenNoLink() throws NoSuchFieldException {
        makeBuyerTrade();
        FieldSetter.setField(player, Player.class.getDeclaredField("receivedLinkloss"), 1L);
        assert !player.hasLink();

        makeTrade();

        assertNull(buyer.getTrade());
        assertThat(buyer, receivedMessageContaining("cannot trade right now"));
    }

    @Test
    void testBuyerDoesNotAddOwnInventoryToNonOwnerTrade() {
        Item item = factory.createNewItem();
        buyer.getInventory().insertItem(item);
        Set<Item> items = new HashSet<>(buyer.getInventory().getItems());
        makeBuyerTrade();

        Set<Item> set = Stream.of(trade.getTradingWindow(1).getItems()).collect(Collectors.toSet());
        assertThat(items, containsNoneOf(set));
    }

    @Test
    void testTradeUpdatesBuyerShopCount() {
        makeBuyerTrade();
        makeTrade();

        buyer.getInventory().getItems().removeIf(Item::isCoin);
        assertEquals(2, buyer.getInventory().getItems().size());
        assertEquals(1, factory.getShop(buyer).merchantDataInt);
    }

    @Test
    void testGetCreatureReturnsCorrectCreature() {
        makeOwnerBuyerTrade();
        assertSame(owner, trade.getCreatureOne());
        assertSame(buyer, trade.getCreatureTwo());
    }

    @Test
    void testReplacePriceList() {
        Item oldPriceList = buyer.getInventory().getFirstContainedItem();
        assert (PriceList.isPriceList(oldPriceList));

        Item newPriceList = factory.createPriceList(PriceListTest.one + "\n" + PriceListTest.two);
        owner.getInventory().insertItem(newPriceList);

        makeOwnerBuyerTrade();
        trade.getTradingWindow(2).addItem(newPriceList);
        balance();
        setSatisfied(owner);

        assertEquals(1, buyer.getInventory().getItems().size());
        assertSame(newPriceList, buyer.getInventory().getItems().iterator().next());
        assertThrows(NoSuchItemException.class, () -> Items.getItem(oldPriceList.getWurmId()));
    }

    @Test
    void testMoneyTradedProperly() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, PriceList.PageNotAdded {
        PriceList priceList  = PriceList.getPriceListFromBuyer(buyer);
        Item tradingItem = factory.createNewItem();
        priceList.addItem(tradingItem.getTemplateId(), tradingItem.getMaterial(), tradingItem.getQualityLevel(), MonetaryConstants.COIN_COPPER);
        priceList.savePriceList();

        player.getInventory().insertItem(tradingItem);

        Item coin1 = factory.createNewCopperCoin();
        Item coin2 = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(coin1);
        buyer.getInventory().insertItem(coin2);
        factory.getShop(buyer).setMoney(MonetaryConstants.COIN_COPPER * 2);

        makeBuyerTrade();
        assertTrue(trade.getTradingWindow(2).mayAddFromInventory(player, tradingItem));
        trade.getTradingWindow(2).addItem(tradingItem);

        balance();
        setSatisfied(player);

        assertEquals((long)(MonetaryConstants.COIN_COPPER * 0.9f), factory.getShop(buyer).getMoney());
        assertThat(buyer, hasCoinsOfValue((long)(MonetaryConstants.COIN_COPPER * 0.9f)));
        assertThat(player, hasCoinsOfValue((long)(MonetaryConstants.COIN_COPPER)));

        makeOwnerBuyerTrade();
        BuyerHandler handler = (BuyerHandler)buyer.getTradeHandler();
        Method method = BuyerHandler.class.getDeclaredMethod("addItemsToTrade");
        method.setAccessible(true);
        method.invoke(handler);

        for (Item item : trade.getTradingWindow(1).getAllItems()) {
            item.getTradeWindow().removeItem(item);
            trade.getTradingWindow(3).addItem(item);
        }

        assertEquals(0, trade.getTradingWindow(1).getItems().length);
        assertNotEquals(0, trade.getTradingWindow(3).getItems().length);

        balance();
        setSatisfied(owner);

        assertEquals(0, factory.getShop(buyer).getMoney());
        assertThat(buyer, hasCoinsOfValue(0L));
        assertThat(player, hasCoinsOfValue((long)(MonetaryConstants.COIN_COPPER)));
        assertThat(owner, hasCoinsOfValue((long)(MonetaryConstants.COIN_COPPER * 0.9f)));
    }
}