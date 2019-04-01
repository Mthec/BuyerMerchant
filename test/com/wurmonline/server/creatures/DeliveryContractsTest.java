package com.wurmonline.server.creatures;

import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.*;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.EntryBuilder;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

import static mod.wurmunlimited.Assert.containsCoinsOfValue;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryContractsTest extends WurmTradingTest {

    private BuyerHandler handler;
    private TradingWindow playerOffer;
    private TradingWindow playerToTrade;
    private Item contract;
    private static int deliveryContractId = -1;

    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();

        if (ItemTemplateFactory.getInstance().getTemplateOrNull(deliveryContractId) == null) {
            deliveryContractId = new ItemTemplateBuilder("contract.delivery")
                                         .name("delivery contract", "contracts", "DESCRIPTION")
                                         .itemTypes(new short[0])
                                         .modelName("")
                                         .containerSize(1000,1000,1000)
                                         .build().getTemplateId();
            ReflectionUtil.setPrivateField(null, BuyerHandler.class.getDeclaredField("deliveryContractId"), deliveryContractId);
        }

        contract = factory.createNewItem(deliveryContractId);
        player.getInventory().insertItem(contract);
    }

    private void createHandler() {
        makeBuyerTrade();
        handler = (BuyerHandler)buyer.getTradeHandler();
        playerOffer = trade.getTradingWindow(2);
        playerToTrade = trade.getTradingWindow(4);
    }

    private void addOneCopperItemToPriceList(int templateId) {
        addOneCopperItemToPriceList(templateId, 1);
    }

    private void addOneCopperItemToPriceList(int templateId, int minimumRequired) {
        try {
            PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
            priceList.addItem(templateId, (byte)0, -1, 1.0f, MonetaryConstants.COIN_COPPER, 0, minimumRequired, false);
            priceList.savePriceList();
            Shop shop = factory.getShop(buyer);
            shop.setMoney(shop.getMoney() + (long)(MonetaryConstants.COIN_COPPER * 1.1f));
        } catch (NoSuchTemplateException | IOException | PriceList.PriceListFullException | PriceList.PageNotAdded e) {
            throw new RuntimeException(e);
        }
    }

    // To trick container check without bytecode hook.
    private void insertItemsIntoContract(Iterable<Item> items) {
        try {
            Field field = ItemTemplate.class.getDeclaredField("hollow");
            ReflectionUtil.setPrivateField(contract.getTemplate(), field, true);
            for (Item item : items) {
                contract.insertItem(item, true);
            }
            ReflectionUtil.setPrivateField(contract.getTemplate(), field, false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testSingleItemDeliveryContractAccepted() {
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 50));
        addOneCopperItemToPriceList(ItemList.dirtPile);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 50);

        createHandler();
        playerOffer.addItem(contract);
        handler.balance();

        assertEquals(0, playerOffer.getItems().length);
        assertEquals(1, playerToTrade.getItems().length);
        assertEquals(contract, playerToTrade.getItems()[0]);
    }

    @Test
    void testMultipleItemsDeliveryContractAccepted() {
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 25));
        addOneCopperItemToPriceList(ItemList.dirtPile);
        insertItemsIntoContract(factory.createManyItems(ItemList.sand, 25));
        addOneCopperItemToPriceList(ItemList.sand);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 50);

        createHandler();
        playerOffer.addItem(contract);
        handler.balance();

        assertEquals(0, playerOffer.getItems().length);
        assertEquals(1, playerToTrade.getItems().length);
        assertEquals(contract, playerToTrade.getItems()[0]);
    }

    @Test
    void testMixedAuthorisationDeliveryContractNotAccepted() {
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 25));
        addOneCopperItemToPriceList(ItemList.dirtPile);
        insertItemsIntoContract(factory.createManyItems(ItemList.sand, 25));
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 50);

        createHandler();
        playerOffer.addItem(contract);
        handler.balance();

        assertEquals(1, playerOffer.getItems().length);
        assertEquals(0, playerToTrade.getItems().length);
        assertEquals(contract, playerOffer.getItems()[0]);
        assertThat(player, receivedMessageContaining("not authorised"));
    }

    @Test
    void testMinimumDeliveryContractAccepted() {
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 25));
        addOneCopperItemToPriceList(ItemList.dirtPile, 20);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 25);

        createHandler();
        playerOffer.addItem(contract);
        handler.balance();

        assertEquals(0, playerOffer.getItems().length);
        assertEquals(1, playerToTrade.getItems().length);
        assertEquals(contract, playerToTrade.getItems()[0]);
    }

    @Test
    void testAuthorisedButMixedMinimumDeliveryContractNotAccepted() {
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 25));
        addOneCopperItemToPriceList(ItemList.dirtPile, 20);
        insertItemsIntoContract(factory.createManyItems(ItemList.sand, 25));
        addOneCopperItemToPriceList(ItemList.sand, 30);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 50);

        createHandler();
        playerOffer.addItem(contract);
        handler.balance();

        assertEquals(1, playerOffer.getItems().length);
        assertEquals(0, playerToTrade.getItems().length);
        assertEquals(contract, playerOffer.getItems()[0]);
        assertThat(player, receivedMessageContaining("need 5 more heaps of sand"));
    }

    @Test
    void testCombinedMinimumsDeliveryContractNotAccepted() {
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 25));
        factory.createManyItems(ItemList.dirtPile, 5).forEach(i -> player.getInventory().insertItem(i, true));
        addOneCopperItemToPriceList(ItemList.dirtPile, 20);
        insertItemsIntoContract(factory.createManyItems(ItemList.sand, 25));
        addOneCopperItemToPriceList(ItemList.sand, 30);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 50);

        createHandler();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(6, playerOffer.getItems().length);
        assertEquals(0, playerToTrade.getItems().length);
        assertTrue(Arrays.asList(playerOffer.getItems()).contains(contract));
        assertThat(player, receivedMessageContaining("need 15 more pile of dirt"));
        assertThat(player, receivedMessageContaining("need 5 more heaps of sand"));
    }

    @Test
    void testIndividualItemsStillAcceptedIfEnoughAfterContractRemoval() {
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 25));
        factory.createManyItems(ItemList.dirtPile, 20).forEach(i -> player.getInventory().insertItem(i, true));
        addOneCopperItemToPriceList(ItemList.dirtPile, 20);
        insertItemsIntoContract(factory.createManyItems(ItemList.sand, 25));
        addOneCopperItemToPriceList(ItemList.sand, 30);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 50);

        createHandler();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(1, playerOffer.getItems().length);
        assertEquals(20, playerToTrade.getItems().length);
        assertEquals(contract, playerOffer.getItems()[0]);
        assertThat(player, receivedMessageContaining("need 5 more heaps of sand"));
    }

    @Test
    void testContractsOnlyAcceptedOnce() {
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 10));
        factory.createManyItems(ItemList.dirtPile, 10).forEach(i -> player.getInventory().insertItem(i, true));
        addOneCopperItemToPriceList(ItemList.dirtPile);
        insertItemsIntoContract(factory.createManyItems(ItemList.sand, 10));
        addOneCopperItemToPriceList(ItemList.sand);
        insertItemsIntoContract(factory.createManyItems(ItemList.log, 10));
        factory.createManyItems(ItemList.log, 10).forEach(i -> player.getInventory().insertItem(i, true));
        addOneCopperItemToPriceList(ItemList.log);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 20);

        createHandler();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(0, playerOffer.getItems().length);
        assertEquals(21, playerToTrade.getItems().length);
    }

    @Test
    void testOnlyFullWeightItemsAcceptedAsPartOfMinimum() {
        insertItemsIntoContract(factory.createManyItems(ItemList.log, 10));
        Item item = contract.getItemsAsArray()[0];
        item.setWeight(item.getTemplate().getWeightGrams() / 2, false);
        addOneCopperItemToPriceList(ItemList.log, 2);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 10);

        createHandler();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(1, playerOffer.getItems().length);
        assertEquals(0, playerToTrade.getItems().length);
        assertThat(player, receivedMessageContaining("full weight"));
    }

    @Test
    void testContractAcceptedAndSingleItemsTrimmed() {
        assert BuyerHandler.maxPersonalItems == 51;
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 45));
        factory.createManyItems(ItemList.dirtPile, 55).forEach(i -> player.getInventory().insertItem(i, true));
        addOneCopperItemToPriceList(ItemList.dirtPile, 10);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(shop.getMoney() * 56);

        createHandler();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(6, playerOffer.getItems().length);
        assertEquals(50, playerToTrade.getItems().length);
        assertTrue(Arrays.asList(playerToTrade.getItems()).contains(contract));
        assertThat(player, receivedMessageContaining("accept all of the pile of dirt"));
    }

    @Test
    void testMixedContentsContractsRemoveOthersIfUncombinedCountIsBelowMinimum() {
        assert BuyerHandler.maxPersonalItems == 51;
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 1));
        addOneCopperItemToPriceList(ItemList.dirtPile, 10);
        insertItemsIntoContract(factory.createManyItems(ItemList.sand, 20));
        addOneCopperItemToPriceList(ItemList.sand, 10);
        Shop shop = factory.getShop(buyer);
        shop.setMoney(1000000);

        // Other contract
        contract = factory.createNewItem(deliveryContractId);
        player.getInventory().insertItem(contract);
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 8));

        createHandler();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(2, playerOffer.getItems().length);
        assertEquals(0, playerToTrade.getItems().length);
        assertThat(player, receivedMessageContaining("need 1 more dirt"));
    }

    @Test
    void testContractsWithDonationItems() throws IOException, PriceList.PageNotAdded, PriceList.PriceListFullException, EntryBuilder.EntryBuilderException {
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, 10));
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(ItemList.dirtPile).minimumRequired(10).build();
        priceList.savePriceList();

        createHandler();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(0, playerOffer.getItems().length);
        assertEquals(1, playerToTrade.getItems().length);
        assertThat(player, receivedMessageContaining("donation"));
    }

    @Test
    void testContractPricing() throws PriceList.PriceListFullException, NoSuchTemplateException, IOException, PriceList.PageNotAdded {
        int dirt = 10;
        int sand = 5;
        insertItemsIntoContract(factory.createManyItems(ItemList.dirtPile, dirt));
        addOneCopperItemToPriceList(ItemList.dirtPile);
        insertItemsIntoContract(factory.createManyItems(ItemList.sand, sand));
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(ItemList.sand, (byte)0, -1, 1.0f, 1);
        priceList.savePriceList();
        factory.getShop(buyer).setMoney(100000);

        createHandler();
        player.getInventory().getItems().forEach(playerOffer::addItem);
        handler.balance();

        assertEquals(0, playerOffer.getItems().length);
        assertEquals(1, playerToTrade.getItems().length);
        assertThat(Arrays.asList(trade.getTradingWindow(3).getItems()),
                containsCoinsOfValue((long)((dirt * MonetaryConstants.COIN_COPPER) + (sand * MonetaryConstants.COIN_IRON))));
    }
}
