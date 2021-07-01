package com.wurmonline.server.creatures;

import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.items.BuyerTrade;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemsPackageFactory;
import com.wurmonline.server.items.TradingWindow;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.PriceListTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static mod.wurmunlimited.Assert.sameContentsAs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BuyerHandler_OwnerTest extends WurmTradingTest {

    private BuyerHandler handler;

    @Override
    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        makeOwnerBuyerTrade();
        //noinspection ConstantConditions
        handler = (BuyerHandler)buyer.getTradeHandler();
    }

    @Test
    void testBuyerItemsAddedToWindow() {
        Item priceList = buyer.getInventory().getFirstContainedItem();
        factory.createManyItems(5).forEach(buyer.getInventory()::insertItem);

        handler.addItemsToTrade();

        List<Item> items = Arrays.asList(trade.getTradingWindow(1).getAllItems());
        assertFalse(items.contains(priceList));
        ItemsPackageFactory.removeItem(buyer, priceList);
        assertThat(items, sameContentsAs(buyer.getInventory().getItems()));
    }

    // Suck Interesting Items.

    @Test
    void testSucksNewPriceList() {
        Item priceList = factory.createPriceList(PriceListTest.two);
        owner.getInventory().insertItem(priceList);
        TradingWindow window = trade.getTradingWindow(2);
        window.addItem(priceList);
        handler.balance();

        assertEquals(0, window.getItems().length);
        assertSame(priceList, trade.getCreatureTwoRequestWindow().getAllItems()[0]);
    }

    @Test
    void testSucksCoins() {
        int items = 5;
        Arrays.asList(Economy.getEconomy().getCoinsFor(500)).forEach(owner.getInventory()::insertItem);
        TradingWindow window = trade.getTradingWindow(2);
        owner.getInventory().getItems().forEach(window::addItem);
        handler.balance();

        assertEquals(0, window.getItems().length);
        assertEquals(items, trade.getCreatureTwoRequestWindow().getAllItems().length);
        Stream.of(trade.getCreatureTwoRequestWindow().getAllItems()).forEach(i -> assertTrue(i.isCoin()));
    }

    @Test
    void testDoesNotSuckItems() {
        int items = 5;
        factory.createManyItems(items).forEach(owner.getInventory()::insertItem);
        TradingWindow window = trade.getTradingWindow(2);
        owner.getInventory().getItems().forEach(window::addItem);
        handler.balance();

        assertEquals(items, window.getItems().length);
        assertNotEquals(items, trade.getCreatureTwoRequestWindow().getAllItems().length);
    }

    // Balance

    @Test
    void testBuyerAlwaysSatisfied() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int items = 5;
        factory.createManyItems(items).forEach(owner.getInventory()::insertItem);
        Arrays.asList(Economy.getEconomy().getCoinsFor(500)).forEach(owner.getInventory()::insertItem);
        TradingWindow window = trade.getTradingWindow(2);
        owner.getInventory().getItems().forEach(window::addItem);
        handler.balance();

        Method isCreatureTwoSatisfied = BuyerTrade.class.getDeclaredMethod("isCreatureTwoSatisfied");
        isCreatureTwoSatisfied.setAccessible(true);
        assertTrue((boolean)isCreatureTwoSatisfied.invoke(trade));
    }
}
