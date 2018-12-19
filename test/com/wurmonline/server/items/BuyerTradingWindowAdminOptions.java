package com.wurmonline.server.items;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.creatures.BuyerHandler;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.BuyerMerchant;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.FieldSetter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
            FieldSetter.setField(buyerMerchant, BuyerMerchant.class.getDeclaredField("freeMoney"), freeMoney);
            FieldSetter.setField(buyerMerchant, BuyerMerchant.class.getDeclaredField("destroyBoughtItems"), destroyBoughtItems);
            buyerMerchant.onServerStarted();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testNoFreeMoneyOnFalse() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        assert !BuyerTradingWindow.freeMoney;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, 1.0f, MonetaryConstants.COIN_GOLD);
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
    void testFreeMoneyOnTrue() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        BuyerTradingWindow.freeMoney = true;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, 1.0f, MonetaryConstants.COIN_GOLD);
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
    void testItemsNotDestroyedOnFalse() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        assert !BuyerTradingWindow.destroyBoughtItems;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, 1.0f, MonetaryConstants.COIN_COPPER);
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
    void testItemsDestroyedOnTrue() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        BuyerTradingWindow.destroyBoughtItems = true;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, 1.0f, MonetaryConstants.COIN_COPPER);
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
    void testDestroyAndFreeMoney() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        BuyerTradingWindow.freeMoney = true;
        BuyerTradingWindow.destroyBoughtItems = true;

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, 1.0f, MonetaryConstants.COIN_GOLD);
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
    void testMaxPersonalItemsIncreasedOnDestroyBoughtItems() {
        assert !BuyerTradingWindow.destroyBoughtItems;
        assert BuyerHandler.maxPersonalItems < 100;

        setOptions(false, true);

        assertTrue(BuyerTradingWindow.destroyBoughtItems);
        assertEquals(Integer.MAX_VALUE, BuyerHandler.getMaxNumPersonalItems());
    }

    @Test
    void testNoWeightRestrictionForDestroyBoughtItems() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        assert !BuyerTradingWindow.destroyBoughtItems;
        // Iron ore.
        int templateId = 38;
        int numberOfItems = 50;
        long price = MonetaryConstants.COIN_COPPER * 2;

        setOptions(true, true);

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(templateId, ItemMaterials.MATERIAL_IRON, 1.0f, (int)price);
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
}
