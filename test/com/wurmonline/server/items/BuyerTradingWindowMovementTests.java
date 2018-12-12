package com.wurmonline.server.items;

import mod.wurmunlimited.WurmTradingTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BuyerTradingWindowMovementTests extends WurmTradingTest {

    private TradingWindow window1;
    private TradingWindow window2;
    private TradingWindow window3;
    private TradingWindow window4;
    private Item playerItem;
    private Item buyerItem;
    private Item ownerItem;

    @Override
    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();

        makeBuyerTrade();

        window1 = trade.getTradingWindow(1);
        window2 = trade.getTradingWindow(2);
        window3 = trade.getTradingWindow(3);
        window4 = trade.getTradingWindow(4);

        playerItem = factory.createNewItem();
        player.getInventory().insertItem(playerItem);
        buyerItem = factory.createNewItem();
        buyer.getInventory().insertItem(buyerItem);
        ownerItem = factory.createNewItem();
        owner.getInventory().insertItem(ownerItem);
    }

    //
    // MayMoveFromInventory
    //

    @Test
    void testPlayerMayMoveFromInventoryToPlayerOfferWindow() {
        assertTrue(window2.mayAddFromInventory(player, playerItem));
    }

    @Test
    void testBuyerMayMoveFromInventoryToPlayerOfferWindow() {
        assertFalse(window2.mayAddFromInventory(buyer, buyerItem));
    }

    @Test
    void testPlayerMayMoveFromInventoryToBuyerOfferWindow() {
        assertFalse(window1.mayAddFromInventory(player, playerItem));
    }

    @Test
    void testBuyerMayMoveFromInventoryToBuyerOfferWindow() {
        assertTrue(window1.mayAddFromInventory(buyer, buyerItem));
    }

    @Test
    void testMayNotAddToRequestWindowsFromInventory() {
        assertAll(
                () -> assertFalse(window3.mayAddFromInventory(player, playerItem)),
                () -> assertFalse(window4.mayAddFromInventory(player, playerItem)),
                () -> assertFalse(window3.mayAddFromInventory(buyer, buyerItem)),
                () -> assertFalse(window4.mayAddFromInventory(buyer, buyerItem))
        );
    }

    @Test
    void testNonTradableItemsCantBeAdded() {
        assertTrue(window2.mayAddFromInventory(player, playerItem));
        Item item = factory.createNoTradeItem();
        player.getInventory().insertItem(item);
        assert item.isNoTrade();
        assertFalse(window2.mayAddFromInventory(player, item));
        assertThat(player, receivedMessageContaining("not tradable"));
    }

    @Test
    void testHollowWithAcceptableItems() {
        Item hollow = factory.createNewItem(factory.getIsHollowId());
        player.getInventory().insertItem(hollow);
        factory.createManyItems(5).forEach(hollow::insertItem);

        assertTrue(window2.mayAddFromInventory(player, hollow));
    }

    @Test
    void testHollowWithNoTradeItem() {
        Item hollow = factory.createNewItem(factory.getIsHollowId());
        player.getInventory().insertItem(hollow);
        Item noTrade = factory.createNoTradeItem();
        assert noTrade.isNoTrade();
        hollow.insertItem(noTrade, true);
        assert hollow.getItems().contains(noTrade);

        assertFalse(window2.mayAddFromInventory(player, hollow));
        assertThat(player, receivedMessageContaining("contains a non-tradable item"));
    }

    //
    // MayMoveItemToWindow
    //

    @Test
    void testAllWindowsCannotBeMovedTo() {
        for (int windowId : new int[] {1, 2, 3, 4})
        assertAll(
                () -> assertFalse(window1.mayMoveItemToWindow(playerItem, player, windowId)),
                () -> assertFalse(window2.mayMoveItemToWindow(playerItem, player, windowId)),
                () -> assertFalse(window3.mayMoveItemToWindow(playerItem, player, windowId)),
                () -> assertFalse(window4.mayMoveItemToWindow(playerItem, player, windowId)),

                () -> assertFalse(window1.mayMoveItemToWindow(buyerItem, buyer, windowId)),
                () -> assertFalse(window2.mayMoveItemToWindow(buyerItem, buyer, windowId)),
                () -> assertFalse(window3.mayMoveItemToWindow(buyerItem, buyer, windowId)),
                () -> assertFalse(window4.mayMoveItemToWindow(buyerItem, buyer, windowId))
        );
    }

    @Test
    void testOwnerMayMoveFromBuyerOfferToRequest() {
        makeOwnerBuyerTrade();

        assertTrue(trade.getTradingWindow(1).mayMoveItemToWindow(buyerItem, owner, 3));
    }
}
