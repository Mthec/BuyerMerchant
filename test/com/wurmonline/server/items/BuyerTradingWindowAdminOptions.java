package com.wurmonline.server.items;

import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.stream.Stream;

import static mod.wurmunlimited.Assert.hasCoinsOfValue;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

// free_money and destroy_bought_items tests.
class BuyerTradingWindowAdminOptions extends WurmTradingTest {

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
}
