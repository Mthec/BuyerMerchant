package com.wurmonline.server.items;

import com.wurmonline.server.creatures.BuyerHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.PriceList;
import mod.wurmunlimited.buyermerchant.PriceListTest;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static mod.wurmunlimited.Assert.hasCoinsOfValue;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BuyerTradingWindowTest extends WurmTradingTest {

    private BuyerTradingWindow buyerWindow;
    private BuyerTradingWindow playerWindow;

    @Override
    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        trade = new BuyerTrade(player, buyer);
        player.setTrade(trade);
        buyer.setTrade(trade);
        ReflectionUtil.setPrivateField(buyer, Creature.class.getDeclaredField("tradeHandler"), new BuyerHandler(buyer, trade));
        buyerWindow = (BuyerTradingWindow)trade.getCreatureOneRequestWindow();
        playerWindow =(BuyerTradingWindow)trade.getCreatureTwoRequestWindow();
    }

    private Trade createBaseTrade() {
        Trade trade = new Trade(player, buyer);
        player.setTrade(trade);
        buyer.setTrade(trade);
        return trade;
    }

    @Test
    void testKingsMoney() {
        Item item = factory.createNewCopperCoin();

        player.getInventory().insertItem(item);
        playerWindow.addItem(item);
        playerWindow.swapOwners();
        assertEquals(0, factory.getShop(null).getMoney());

        Item buyerInventory = buyer.getInventory();
        buyerInventory.insertItem(item);
        Stream.generate(() -> factory.createNewIronCoin()).limit(10).forEach(buyerInventory::insertItem);
        buyerWindow.addItem(factory.createNewCopperCoin());
        buyerWindow.swapOwners();
        assertEquals(10, factory.getShop(null).getMoney());
    }

    @Test
    void testOwnerGettingCoinsDoesNotPayKing() {
        makeOwnerBuyerTrade();
        buyerWindow = (BuyerTradingWindow)trade.getCreatureOneRequestWindow();
        buyerWindow.addItem(factory.createNewCopperCoin());
        buyerWindow.swapOwners();
        assertEquals(0, factory.getShop(null).getMoney());
    }

    @Test
    void testItemsMovedToCorrectWindow() {
        Item item1 = factory.createNewItem();
        Item item2 = factory.createNewItem();

        buyer.getInventory().insertItem(item1);
        player.getInventory().insertItem(item2);

        buyerWindow.addItem(item1);
        playerWindow.addItem(item2);

        buyerWindow.swapOwners();
        playerWindow.swapOwners();

        assertTrue(player.getInventory().getItems().contains(item1));
        assertFalse(buyer.getInventory().getItems().contains(item1));
        assertTrue(buyer.getInventory().getItems().contains(item2));
        assertFalse(player.getInventory().getItems().contains(item2));
    }

    @Test
    void testItemsMoveFromPlayerToBuyer() {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        playerWindow.addItem(item);

        playerWindow.swapOwners();

        assertTrue(buyer.getInventory().getItems().contains(item));
        assertFalse(player.getInventory().getItems().contains(item));
    }

    // TODO - Should be blocked adding items to non-owner player inventory?  Normal TradingWindow doesn't so should be okay?
    @Test
    void testItemsMoveFromBuyerToPlayer() {
        Item item = factory.createNewItem();
        buyer.getInventory().insertItem(item);
        buyerWindow.addItem(item);

        buyerWindow.swapOwners();

        assertTrue(player.getInventory().getItems().contains(item));
        assertFalse(buyer.getInventory().getItems().contains(item));
    }

    @Test
    void testInsertItem() {
        // Yes, yes, I know, shouldn't test third-party classes.
        Item toTrade = factory.createNewItem();
        player.getInventory().insertItem(toTrade);
        playerWindow.addItem(toTrade);

        setSatisfied(buyer);
        setSatisfied(player);

        assertNotSame(factory.getCommunicator(player).lastNormalServerMessage, FakeCommunicator.empty);
        assertTrue(buyer.getInventory().getItems().contains(toTrade));
        assertFalse(player.getInventory().getItems().contains(toTrade));
    }

    @Test
    void compareTradingWindowInsertItem() {
        // TradingWindow comparison of above.
        factory.getShop(buyer).setToNotPersonalTrader();
        Trade trade = createBaseTrade();
        Item toTrade = factory.createNewItem();
        player.getInventory().insertItem(toTrade);
        trade.getCreatureTwoRequestWindow().addItem(toTrade);

        trade.setSatisfied(buyer, true, trade.getCurrentCounter());
        trade.setSatisfied(player, true, trade.getCurrentCounter());

        assertNotSame(factory.getCommunicator(buyer).lastNormalServerMessage, FakeCommunicator.empty);
        assertTrue(buyer.getInventory().getItems().contains(toTrade));
        assertFalse(player.getInventory().getItems().contains(toTrade));
    }

    @Test
    void testRemoveItem() {
        Item item = factory.createNewItem();
        player.getInventory().insertItem(item);
        playerWindow.addItem(item);
        assert Arrays.asList(playerWindow.getAllItems()).contains(item);
        assert item.getTradeWindow() == playerWindow;

        playerWindow.removeItem(item);
        assertFalse(Arrays.asList(playerWindow.getAllItems()).contains(item));
        assertNull(item.getTradeWindow());
    }

    @Test
    void testRemoveContainerOfItems() {
        Item hollow = factory.createNewItem(factory.getIsHollowId());
        player.getInventory().insertItem(hollow);
        factory.createManyItems(5).forEach(hollow::insertItem);

        playerWindow.addItem(hollow);
        assert hollow.getItems().stream().allMatch(item -> item.getTradeWindow() == playerWindow);

        playerWindow.removeItem(hollow);
        assertTrue(hollow.getItems().stream().allMatch(item -> item.getTradeWindow() == null));
    }

    @Test
    void testTempCoinsReturnedToEconomyOnCancel() {
        Item coin = factory.createNewCopperCoin();
        buyerWindow.addItem(coin);

        trade.end(player, false);

        assertEquals(coin.parentId, -10);
        assertEquals(coin.ownerId, -10);
    }

    @Test
    void testTooManyItems() {
        makeOwnerBuyerTrade();
        factory.createManyItems(99).forEach(owner.getInventory()::insertItem);
        Item straw = factory.createNewItem();
        buyer.getInventory().insertItem(straw);
        trade.getCreatureOneRequestWindow().addItem(straw);

        setSatisfied(buyer);
        setSatisfied(owner);

        assertFalse(owner.getInventory().getItems().contains(straw));
        assertThat(owner, receivedMessageContaining("may not carry that many"));
        assertThat(buyer, receivedMessageContaining("may not carry that many"));
    }

    @Test
    void testOwnerGetMoney() {
        makeOwnerBuyerTrade();

        Item coin1 = factory.createNewCopperCoin();
        Item coin2 = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(coin1);
        buyer.getInventory().insertItem(coin2);

        trade.getCreatureOneRequestWindow().addItem(coin1);

        setSatisfied(owner);
        setSatisfied(buyer);

        assertAll(
                () -> assertThat(owner, hasCoinsOfValue(100L)),
                () -> assertThat(buyer, hasCoinsOfValue(100L)));
    }

    @Test
    void testOwnerReplacingPriceList() throws PriceList.PriceListFullException, IOException, NoSuchTemplateException, PriceList.PageNotAdded, PriceList.PriceListDuplicateException {
        trade = new BuyerTrade(owner, buyer);
        owner.setTrade(trade);
        buyer.setTrade(trade);

        Item oldPriceList = buyer.getInventory().getFirstContainedItem();
        PriceList old = PriceList.getPriceListFromBuyer(buyer);
        // PriceListTest.one - "1,1,1.0,10"
        old.addItem(1, (byte)1, -1, 1.0f, 10);
        old.savePriceList();

        Item newPriceList = factory.createPriceList(PriceListTest.two);
        owner.getInventory().insertItem(newPriceList);

        trade.getCreatureTwoRequestWindow().addItem(newPriceList);

        setSatisfied(owner);
        setSatisfied(buyer);

        assertEquals(-10L, oldPriceList.getOwnerId());
        assertFalse(owner.getInventory().getItems().contains(newPriceList));
        assertTrue(buyer.getInventory().getItems().contains(newPriceList));
        assertFalse(buyer.getInventory().getItems().contains(oldPriceList));
        assertEquals(PriceListTest.two, PriceList.getPriceListFromBuyer(buyer).iterator().next().toString());
    }

    // Special test to see if TradingWindow modification will work.

    @Test
    void testBuyerContractSelling() {
        Creature trader = factory.createNewBuyer(owner);
        factory.getShop(trader).setToNotPersonalTrader();
        Item merchantContract = factory.createNewItem(300);
        trader.getInventory().insertItem(merchantContract);
        Item notMerchantContract = factory.createNewItem(factory.getIsWoodId());
        trader.getInventory().insertItem(notMerchantContract);
        notMerchantContract.setPrice(MonetaryConstants.COIN_SILVER * 10);
        assert notMerchantContract.getTemplateId() != 300;
        notMerchantContract.setTemplateId(300);

        Trade trade = new Trade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        TradingWindow window = trade.getCreatureOneRequestWindow();

        window.addItem(merchantContract);
        window.swapOwners();

        assertEquals(MonetaryConstants.COIN_SILVER * 10 / 4, factory.getShop(null).getMoney());

        trade = new Trade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        window = trade.getCreatureOneRequestWindow();

        factory.getShop(null).setMoney(0);
        assert window.getItems().length == 0;
        window.addItem(notMerchantContract);
        window.swapOwners();

        assertEquals(MonetaryConstants.COIN_SILVER * 10 / 4, factory.getShop(null).getMoney());
    }

    @Test
    void testBuyerContractTrading() {
        Item merchantContract = factory.createNewItem(300);
        merchantContract.setData(buyer.getWurmId());
        player.getInventory().insertItem(merchantContract);
        Item notMerchantContract = factory.createNewItem(factory.getIsWoodId());
        notMerchantContract.setData(buyer.getWurmId());
        player.getInventory().insertItem(notMerchantContract);
        notMerchantContract.setPrice(MonetaryConstants.COIN_SILVER * 10);
        assert notMerchantContract.getTemplateId() != 300;
        notMerchantContract.setTemplateId(300);

        Trade trade = new Trade(player, owner);
        player.setTrade(trade);
        owner.setTrade(trade);
        TradingWindow window = trade.getCreatureTwoRequestWindow();

        window.addItem(merchantContract);
        window.swapOwners();

        assertEquals(owner.getWurmId(), merchantContract.ownerId);
        assertEquals(owner.getWurmId(), factory.getShop(buyer).getOwnerId());

        trade = new Trade(player, owner);
        player.setTrade(trade);
        owner.setTrade(trade);
        window = trade.getCreatureTwoRequestWindow();

        factory.getShop(null).setMoney(0);
        assert window.getItems().length == 0;
        window.addItem(notMerchantContract);
        window.swapOwners();

        assertEquals(owner.getWurmId(), notMerchantContract.ownerId);
        assertEquals(owner.getWurmId(), factory.getShop(buyer).getOwnerId());
    }

    @Test
    void testBuyerContractRestockedOnSaleAtTrader() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Creature trader = factory.createNewTrader();
        assert trader.isNpcTrader() && !factory.getShop(trader).isPersonal();
        int price = MonetaryConstants.COIN_SILVER * 10;
        Item contract = factory.createBuyerContract();
        contract.setPrice(price);
        trader.getInventory().insertItem(contract);
        assert contract.getPrice() == price;
        assert contract.isFullprice();
        List<Item> coins = Arrays.asList(Economy.getEconomy().getCoinsFor(price));
        coins.forEach(player.getInventory()::insertItem);
        assertThat(player, hasCoinsOfValue((long)price));

        Trade trade = new Trade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        coins.forEach(trade.getTradingWindow(2)::addItem);
        trade.getTradingWindow(3).addItem(contract);

        Method balance = TradeHandler.class.getDeclaredMethod("balance");
        balance.setAccessible(true);
        balance.invoke(trader.getTradeHandler());

        trade.setSatisfied(player, true, trade.getCurrentCounter());

        assertTrue(player.getInventory().getItems().contains(contract));
        assertEquals(price, factory.getShop(trader).getMoney());
        assertFalse(trader.getInventory().getItems().contains(contract));
        assertTrue(trader.getInventory().getItems().stream().anyMatch(i -> i.getTemplateId() == contract.getTemplateId() && i.getName().equals("personal buyer contract")));
    }
}
