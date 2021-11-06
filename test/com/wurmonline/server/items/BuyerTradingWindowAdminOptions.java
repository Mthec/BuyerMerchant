package com.wurmonline.server.items;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.behaviours.MethodsCreatures;
import com.wurmonline.server.creatures.BuyerHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.BuyerMerchant;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static mod.wurmunlimited.Assert.hasCoinsOfValue;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

// free_money and destroy_bought_items tests.
class BuyerTradingWindowAdminOptions extends WurmTradingTest {

    @SuppressWarnings("SameParameterValue")
    private void setOptions(boolean freeMoney, boolean destroyBoughtItems) {
        try {
            BuyerMerchant buyerMerchant = new BuyerMerchant();
            ReflectionUtil.setPrivateField(buyerMerchant, BuyerMerchant.class.getDeclaredField("freeMoney"), freeMoney);
            ReflectionUtil.setPrivateField(buyerMerchant, BuyerMerchant.class.getDeclaredField("destroyBoughtItems"), destroyBoughtItems);
            buyerMerchant.onServerStarted();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testNoFreeMoneyOnFalse() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        assert !BuyerTradingWindow.freeMoney;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, -1, 1.0f, MonetaryConstants.COIN_GOLD);
        priceList.savePriceList();
        buyer.getInventory().insertItem(factory.createNewCopperCoin());
        factory.getShop(buyer).setMoney(MonetaryConstants.COIN_COPPER);

        makeBuyerTrade();
        Item item = factory.createNewItem(factory.getIsMetalId());
        item.setMaterial(ItemMaterials.MATERIAL_IRON);
        player.getInventory().insertItem(item);
        trade.getCreatureTwoRequestWindow().addItem(item);
        balance();

        setSatisfied(player);

        assertEquals(2, buyer.getInventory().getItemCount());
        assertEquals(MonetaryConstants.COIN_COPPER, factory.getShop(buyer).getMoney());
        assertThat(buyer, hasCoinsOfValue((long)MonetaryConstants.COIN_COPPER));
        assertThat(player, hasCoinsOfValue(0L));
        assertTrue(player.getInventory().getItems().contains(item));
        assertThat(player, receivedMessageContaining("low on cash"));
    }

    @Test
    void testFreeMoneyOnTrue() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        BuyerTradingWindow.freeMoney = true;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, -1, 1.0f, MonetaryConstants.COIN_GOLD);
        priceList.savePriceList();
        buyer.getInventory().insertItem(factory.createNewCopperCoin());
        factory.getShop(buyer).setMoney(MonetaryConstants.COIN_COPPER);

        makeBuyerTrade();
        Item item = factory.createNewItem(factory.getIsMetalId());
        item.setMaterial(ItemMaterials.MATERIAL_IRON);
        player.getInventory().insertItem(item);
        trade.getCreatureTwoRequestWindow().addItem(item);
        balance();

        setSatisfied(player);

        assertEquals(3, buyer.getInventory().getItemCount());
        assertEquals(MonetaryConstants.COIN_COPPER, factory.getShop(buyer).getMoney());
        assertThat(buyer, hasCoinsOfValue((long)MonetaryConstants.COIN_COPPER));
        assertThat(player, hasCoinsOfValue((long)MonetaryConstants.COIN_GOLD));
        assertFalse(player.getInventory().getItems().contains(item));
        assertThat(player, receivedMessageContaining("completed successfully"));
    }

    @Test
    void testItemsNotDestroyedOnFalse() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        assert !BuyerTradingWindow.destroyBoughtItems;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, -1, 1.0f, MonetaryConstants.COIN_COPPER);
        priceList.savePriceList();
        Stream.of(Economy.getEconomy().getCoinsFor((long)(MonetaryConstants.COIN_COPPER * 1.1f))).forEach(buyer.getInventory()::insertItem);
        factory.getShop(buyer).setMoney((long)(MonetaryConstants.COIN_COPPER * 1.1f));

        makeBuyerTrade();
        Item item = factory.createNewItem(factory.getIsMetalId());
        item.setMaterial(ItemMaterials.MATERIAL_IRON);
        player.getInventory().insertItem(item);
        trade.getCreatureTwoRequestWindow().addItem(item);
        balance();

        setSatisfied(player);

        assertEquals(2, buyer.getInventory().getItemCount());
        assertTrue(buyer.getInventory().getItems().contains(item));
        assertEquals(0, factory.getShop(buyer).getMoney());
        assertThat(buyer, hasCoinsOfValue(0L));
        assertThat(player, hasCoinsOfValue((long)MonetaryConstants.COIN_COPPER));
        assertFalse(player.getInventory().getItems().contains(item));
        assertThat(player, receivedMessageContaining("completed successfully"));
    }

    @Test
    void testItemsDestroyedOnTrue() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        BuyerTradingWindow.destroyBoughtItems = true;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, -1, 1.0f, MonetaryConstants.COIN_COPPER);
        priceList.savePriceList();
        Stream.of(Economy.getEconomy().getCoinsFor((long)(MonetaryConstants.COIN_COPPER * 1.1f))).forEach(buyer.getInventory()::insertItem);
        factory.getShop(buyer).setMoney((long)(MonetaryConstants.COIN_COPPER * 1.1f));

        makeBuyerTrade();
        Item item = factory.createNewItem(factory.getIsMetalId());
        item.setMaterial(ItemMaterials.MATERIAL_IRON);
        player.getInventory().insertItem(item);
        trade.getCreatureTwoRequestWindow().addItem(item);
        balance();

        setSatisfied(player);

        assertNull(factory.getItem(item.getWurmId()));
        assertEquals(1, buyer.getInventory().getItemCount());
        assertFalse(buyer.getInventory().getItems().contains(item));
        assertEquals(0, factory.getShop(buyer).getMoney());
        assertThat(buyer, hasCoinsOfValue(0L));
        assertThat(player, hasCoinsOfValue((long)MonetaryConstants.COIN_COPPER));
        assertFalse(player.getInventory().getItems().contains(item));
        assertThat(player, receivedMessageContaining("completed successfully"));
    }

    @Test
    void testDestroyAndFreeMoney() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        BuyerTradingWindow.freeMoney = true;
        BuyerTradingWindow.destroyBoughtItems = true;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, -1, 1.0f, MonetaryConstants.COIN_GOLD);
        priceList.savePriceList();
        Stream.of(Economy.getEconomy().getCoinsFor((long)(MonetaryConstants.COIN_COPPER * 1.1f))).forEach(buyer.getInventory()::insertItem);
        int buyerItemCount = buyer.getInventory().getItemCount();
        factory.getShop(buyer).setMoney((long)(MonetaryConstants.COIN_COPPER * 1.1f));

        makeBuyerTrade();
        Item item = factory.createNewItem(factory.getIsMetalId());
        item.setMaterial(ItemMaterials.MATERIAL_IRON);
        player.getInventory().insertItem(item);
        trade.getCreatureTwoRequestWindow().addItem(item);
        balance();

        setSatisfied(player);

        assertNull(factory.getItem(item.getWurmId()));
        assertEquals(buyerItemCount, buyer.getInventory().getItemCount());
        assertFalse(buyer.getInventory().getItems().contains(item));
        assertEquals((long)(MonetaryConstants.COIN_COPPER * 1.1f), factory.getShop(buyer).getMoney());
        assertThat(buyer, hasCoinsOfValue((long)(MonetaryConstants.COIN_COPPER * 1.1f)));
        assertThat(player, hasCoinsOfValue((long)MonetaryConstants.COIN_GOLD));
        assertFalse(player.getInventory().getItems().contains(item));
        assertThat(player, receivedMessageContaining("completed successfully"));
    }

    @Test
    void testMaxPersonalItemsSetOnDestroyBoughtItems() {
        assert !BuyerTradingWindow.destroyBoughtItems;
        assert BuyerHandler.maxPersonalItems < 100;

        setOptions(false, true);

        assertTrue(BuyerTradingWindow.destroyBoughtItems);
        assertEquals(Integer.MAX_VALUE, BuyerHandler.getMaxNumPersonalItems());
    }

    @Test
    void testNoWeightRestrictionForDestroyBoughtItems() throws IOException, PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException {
        assert !BuyerTradingWindow.destroyBoughtItems;
        // Iron ore.
        int templateId = 38;
        int numberOfItems = 50;
        long price = MonetaryConstants.COIN_COPPER * 2;

        setOptions(true, true);

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(templateId, ItemMaterials.MATERIAL_IRON, -1, 1.0f, (int)price);
        priceList.savePriceList();
        assert Economy.getEconomy().getCoinsFor(price * numberOfItems).length < 99;

        List<Item> items = new ArrayList<>(numberOfItems);
        factory.createManyItems(templateId, numberOfItems).forEach(items::add);
        items.forEach(item -> item.setOwnerId(player.getWurmId()));
        items.forEach(item -> item.setParentId(player.getInventory().getWurmId(), true));
        int itemsWeight = items.stream().mapToInt(Item::getWeightGrams).sum();
        assert !player.canCarry(itemsWeight);
        Item item = items.get(0);
        assert item.getTemplateId() == templateId;
        assert item.getMaterial() == ItemMaterials.MATERIAL_IRON;
        assert item.getQualityLevel() >= 1.0f;
        player.addCarriedWeight(itemsWeight);

        makeBuyerTrade();
        items.forEach(trade.getTradingWindow(2)::addItem);
        balance();
        setSatisfied(player);

        assertThat(player, receivedMessageContaining("completed successfully"));
        assertTrue(player.getInventory().getItems().stream().noneMatch(i -> i.getTemplateId() == templateId));
        assertThat(player, hasCoinsOfValue((price * numberOfItems)));
        assertEquals(1, buyer.getInventory().getItemCount());
    }

    @Test
    void testAcceptsCoinsFromOwnerOnDestroyBoughtItemsWhileNotFreeMoney() {
        BuyerTradingWindow.destroyBoughtItems = true;
        assert !BuyerTradingWindow.freeMoney;

        Item item = factory.createNewItem();
        Item coin = factory.createNewCopperCoin();
        owner.getInventory().insertItem(item);
        owner.getInventory().insertItem(coin);
        makeOwnerBuyerTrade();

        trade.getTradingWindow(2).addItem(item);
        trade.getTradingWindow(2).addItem(coin);

        balance();
        setSatisfied(owner);

        assertTrue(owner.getInventory().getItems().contains(item));
        assertFalse(owner.getInventory().getItems().contains(coin));
        assertEquals(MonetaryConstants.COIN_COPPER, factory.getShop(buyer).getMoney());
    }

    @Test
    void testAcceptsCoinsFromOwnerOnNotDestroyBoughtItemsAndNotFreeMoney() {
        assert !BuyerTradingWindow.freeMoney;
        assert !BuyerTradingWindow.destroyBoughtItems;
        assert factory.getShop(buyer).getMoney() == 0;

        Item item = factory.createNewItem();
        Item coin = factory.createNewCopperCoin();
        owner.getInventory().insertItem(item);
        owner.getInventory().insertItem(coin);
        makeOwnerBuyerTrade();

        trade.getTradingWindow(2).addItem(item);
        trade.getTradingWindow(2).addItem(coin);

        balance();
        setSatisfied(owner);

        assertTrue(owner.getInventory().getItems().contains(item));
        assertFalse(owner.getInventory().getItems().contains(coin));
        assertFalse(buyer.getInventory().getItems().contains(item));
        assertEquals(MonetaryConstants.COIN_COPPER, factory.getShop(buyer).getMoney());
    }

    @Test
    void testAcceptsPriceListWhenOptionsTrue() throws NoSuchTemplateException, FailedException {
        BuyerTradingWindow.freeMoney = true;
        BuyerTradingWindow.destroyBoughtItems = true;

        Item priceList = PriceList.getNewBuyList();
        owner.getInventory().insertItem(priceList);
        Item buyerList = buyer.getInventory().getFirstContainedItem();
        makeOwnerBuyerTrade();

        trade.getTradingWindow(2).addItem(priceList);

        balance();
        setSatisfied(owner);

        assertFalse(owner.getInventory().getItems().contains(priceList));
        assertTrue(buyer.getInventory().getItems().contains(priceList));
        assertFalse(buyer.getInventory().getItems().contains(buyerList));
    }

    @Test
    void testOwnerTradedMoneyAddsToShopWhenFreeMoneyTrue() {
        BuyerTradingWindow.freeMoney = true;

        Item coin = factory.createNewCopperCoin();
        owner.getInventory().insertItem(coin);
        assert factory.getShop(buyer).getMoney() == 0;

        makeOwnerBuyerTrade();
        trade.getTradingWindow(2).addItem(coin);

        balance();
        setSatisfied(owner);

        assertEquals(MonetaryConstants.COIN_COPPER, factory.getShop(buyer).getMoney());
        assertTrue(buyer.getInventory().getItems().contains(coin));
        assertFalse(owner.getInventory().getItems().contains(coin));
    }

    @Test
    void testOwnerCanStillRemoveCoinsWhenFreeMoneyTrue() {
        BuyerTradingWindow.freeMoney = true;

        Item coin = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(coin);
        factory.getShop(buyer).setMoney(MonetaryConstants.COIN_COPPER);

        makeOwnerBuyerTrade();
        trade.getCreatureOneRequestWindow().addItem(coin);

        balance();
        setSatisfied(owner);

        assertEquals(0, factory.getShop(buyer).getMoney());
        assertFalse(buyer.getInventory().getItems().contains(coin));
        assertThat(owner, hasCoinsOfValue((long)MonetaryConstants.COIN_COPPER));
    }

    @Test
    void testMaxItemsSet() {
        Properties config = new Properties();
        config.setProperty("max_items", "100");
        config.setProperty("apply_max_to_merchants", "true");
        BuyerMerchant buyerMerchant = new BuyerMerchant();
        buyerMerchant.configure(config);
        buyerMerchant.onServerStarted();
        assertEquals(100 + 1, BuyerHandler.maxPersonalItems);
        assertEquals(100, TradeHandler.getMaxNumPersonalItems());
    }

    @Test
    void testMaxItemsSetOnlyForBuyer() {
        Properties config = new Properties();
        config.setProperty("max_items", "100");
        config.setProperty("apply_max_to_merchants", "false");
        BuyerMerchant buyerMerchant = new BuyerMerchant();
        buyerMerchant.configure(config);
        buyerMerchant.onServerStarted();
        assertEquals(100 + 1, BuyerHandler.maxPersonalItems);
        assertEquals(50, TradeHandler.getMaxNumPersonalItems());
    }

    @Test
    void testBadMaxItemsValue() {
        Properties config = new Properties();
        config.setProperty("max_items", "abc");
        config.setProperty("apply_max_to_merchants", "true");
        BuyerMerchant buyerMerchant = new BuyerMerchant();
        buyerMerchant.configure(config);
        buyerMerchant.onServerStarted();
        assertEquals(50 + 1, BuyerHandler.maxPersonalItems);
        assertEquals(50, TradeHandler.getMaxNumPersonalItems());
    }

    private void tradeAllItems(Set<Item> items) {
        tradeAllItems(items, player);
    }

    private void tradeAllItems(Set<Item> items, Creature seller) {
        while (items.size() > 0) {
            int count = 0;

            makeBuyerTrade();
            TradingWindow window = trade.getTradingWindow(2);
            while (count < 50 && items.size() > 0) {
                Item item = items.iterator().next();
                seller.getInventory().insertItem(item);
                window.addItem(item);
                items.remove(item);
                ++count;
            }
            balance();
            setSatisfied(seller);
        }
    }

    private void tradeItems(int numberOfItems) throws PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException, IOException {
        Arrays.stream(Economy.getEconomy().getCoinsFor((int)(MonetaryConstants.COIN_IRON * numberOfItems * 1.1f))).forEach(buyer.getInventory()::insertItem);
        factory.getShop(buyer).setMoney((long)(MonetaryConstants.COIN_IRON * numberOfItems * 1.1f));
        Set<Item> items = new HashSet<>(numberOfItems);
        factory.createManyItems(numberOfItems).forEach(items::add);
        Item item = items.iterator().next();
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(item.getTemplateId(), item.getMaterial(), -1, 1.0f, MonetaryConstants.COIN_IRON);
        priceList.savePriceList();

        tradeAllItems(items);
    }

    @Test
    void testMaxItems() throws Exception {
        int numberOfItems = 100;
        int extraItems = 10;
        Properties config = new Properties();
        config.setProperty("max_items", Integer.toString(numberOfItems));
        config.setProperty("apply_max_to_merchants", "true");
        BuyerMerchant buyerMerchant = new BuyerMerchant();
        buyerMerchant.configure(config);
        buyerMerchant.onServerStarted();
        assert BuyerHandler.maxPersonalItems == numberOfItems + 1;
        assert TradeHandler.getMaxNumPersonalItems() == numberOfItems;
        Creature merchant = factory.createNewMerchant(owner);

        // Buyer
        tradeItems(numberOfItems + extraItems);
        assertThat(player, receivedMessageContaining("completed successfully"));
        assertEquals(numberOfItems + 1, buyer.getInventory().getNumItemsNotCoins());
        assertEquals((long)(MonetaryConstants.COIN_IRON * extraItems * 1.1f), factory.getShop(buyer).getMoney());
        assertThat(player, hasCoinsOfValue((long)(MonetaryConstants.COIN_IRON * numberOfItems)));

        // Merchant
        Set<Item> items = new HashSet<>(numberOfItems + extraItems);
        factory.createManyItems(numberOfItems + extraItems).forEach(items::add);

        while (items.size() > 0) {
            int count = 0;

            Field tradeHandler = Creature.class.getDeclaredField("tradeHandler");
            tradeHandler.setAccessible(true);
            tradeHandler.set(merchant, null);

            Method initiateTrade = MethodsCreatures.class.getDeclaredMethod("initiateTrade", Creature.class, Creature.class);
            initiateTrade.setAccessible(true);
            initiateTrade.invoke(null, owner, merchant);
            Trade trade = owner.getTrade();

            TradingWindow window = trade.getTradingWindow(2);
            Method addToInventory = TradeHandler.class.getDeclaredMethod("addToInventory", Item.class, long.class);
            addToInventory.setAccessible(true);
            while (count < 50 && items.size() > 0) {
                Item item = items.iterator().next();
                owner.getInventory().insertItem(item);
                window.addItem(item);
                addToInventory.invoke(merchant.getTradeHandler(), item, 2);
                items.remove(item);
                ++count;
            }
            Method balance = TradeHandler.class.getDeclaredMethod("balance");
            balance.setAccessible(true);
            balance.invoke(merchant.getTradeHandler());
            trade.setSatisfied(owner, true, trade.getCurrentCounter());
        }

        assertThat(owner, receivedMessageContaining("completed successfully"));
        // Plus one, see last if (size > maxPersonalItems) in TradeHandler.suckInterestingItems().
        assertEquals(TradeHandler.getMaxNumPersonalItems() + 1, merchant.getInventory().getItemCount());
        assertEquals(numberOfItems + extraItems - merchant.getInventory().getItemCount(), owner.getInventory().getItemCount());
    }

    @Test
    void testMaxItemsDefaultsTo50() throws PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException, IOException {
        Properties config = new Properties();
        config.setProperty("max_items", "1.0");
        config.setProperty("apply_max_to_merchant", "true");
        BuyerMerchant buyerMerchant = new BuyerMerchant();
        buyerMerchant.configure(config);
        buyerMerchant.onServerStarted();
        assert BuyerHandler.maxPersonalItems == 50 + 1;

        int numberOfItems = 51;
        long money = (long)(MonetaryConstants.COIN_IRON * numberOfItems * 1.1f);
        Arrays.stream(Economy.getEconomy().getCoinsFor((int)money)).forEach(buyer.getInventory()::insertItem);
        factory.getShop(buyer).setMoney(money);
        Set<Item> items = new HashSet<>(numberOfItems);
        factory.createManyItems(numberOfItems).forEach(items::add);
        Item item = items.iterator().next();
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(item.getTemplateId(), item.getMaterial(), -1, 1.0f, MonetaryConstants.COIN_IRON);
        priceList.savePriceList();

        tradeAllItems(items);

        assertThat(player, receivedMessageContaining("cannot add more items"));
        // Remove Price List to keep the money calculations simpler.
        int itemCount = buyer.getInventory().getNumItemsNotCoins() - 1;
        assertEquals(BuyerHandler.maxPersonalItems - 1, itemCount);
        assertEquals((long)(money - (MonetaryConstants.COIN_IRON * itemCount * 1.1f)), factory.getShop(buyer).getMoney());
        assertThat(player, hasCoinsOfValue((long)(MonetaryConstants.COIN_IRON * itemCount)));
    }

    @Test
    void testBuyerOnlyMaxItems() throws PriceList.PriceListFullException, PriceList.PageNotAdded, NoSuchTemplateException, IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        int numberOfItems = 100;
        int extraItems = 10;
        Properties config = new Properties();
        config.setProperty("max_items", Integer.toString(numberOfItems));
        config.setProperty("apply_max_to_merchants", "false");
        BuyerMerchant buyerMerchant = new BuyerMerchant();
        buyerMerchant.configure(config);
        buyerMerchant.onServerStarted();
        assert BuyerHandler.maxPersonalItems == numberOfItems + 1;
        assert TradeHandler.getMaxNumPersonalItems() == 50;
        Creature merchant = factory.createNewMerchant(owner);

        // Buyer
        tradeItems(numberOfItems + extraItems);
        assertThat(player, receivedMessageContaining("completed successfully"));
        assertEquals(numberOfItems + 1, buyer.getInventory().getNumItemsNotCoins());
        assertEquals((long)(MonetaryConstants.COIN_IRON * extraItems * 1.1f), factory.getShop(buyer).getMoney());
        assertThat(player, hasCoinsOfValue((long)(MonetaryConstants.COIN_IRON * numberOfItems)));

        // Merchant
        Set<Item> items = new HashSet<>(numberOfItems + extraItems);
        factory.createManyItems(numberOfItems + extraItems).forEach(items::add);

        while (items.size() > 0) {
            int count = 0;

            Field tradeHandler = Creature.class.getDeclaredField("tradeHandler");
            tradeHandler.setAccessible(true);
            tradeHandler.set(merchant, null);

            Method initiateTrade = MethodsCreatures.class.getDeclaredMethod("initiateTrade", Creature.class, Creature.class);
            initiateTrade.setAccessible(true);
            initiateTrade.invoke(null, owner, merchant);
            Trade trade = owner.getTrade();

            TradingWindow window = trade.getTradingWindow(2);
            Method addToInventory = TradeHandler.class.getDeclaredMethod("addToInventory", Item.class, long.class);
            addToInventory.setAccessible(true);
            while (count < 50 && items.size() > 0) {
                Item item = items.iterator().next();
                owner.getInventory().insertItem(item);
                window.addItem(item);
                addToInventory.invoke(merchant.getTradeHandler(), item, 2);
                items.remove(item);
                ++count;
            }
            Method balance = TradeHandler.class.getDeclaredMethod("balance");
            balance.setAccessible(true);
            balance.invoke(merchant.getTradeHandler());
            trade.setSatisfied(owner, true, trade.getCurrentCounter());
        }

        assertThat(owner, receivedMessageContaining("completed successfully"));
        // Plus one, see last if (size > maxPersonalItems) in TradeHandler.suckInterestingItems().
        assertEquals(TradeHandler.getMaxNumPersonalItems() + 1, merchant.getInventory().getItemCount());
        assertEquals(numberOfItems + extraItems - merchant.getInventory().getItemCount(), owner.getInventory().getItemCount());
    }

    @Test
    void testIntegerOverflowProtection() throws NoSuchFieldException, IllegalAccessException {
        BuyerMerchant buyerMerchant = new BuyerMerchant();
        ReflectionUtil.setPrivateField(buyerMerchant, BuyerMerchant.class.getDeclaredField("maxItems"), Integer.MAX_VALUE);
        assertDoesNotThrow(buyerMerchant::onServerStarted);
        assertEquals(Integer.MAX_VALUE, BuyerHandler.maxPersonalItems);
    }

    // For turn_to_player_max_power see BuyerMerchantTest.
}
