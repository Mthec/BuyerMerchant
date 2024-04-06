package mod.wurmunlimited.buyermerchant;

import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Players;
import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.CreatureBehaviour;
import com.wurmonline.server.behaviours.ItemBehaviour;
import com.wurmonline.server.creatures.*;
import com.wurmonline.server.items.*;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.Questions;
import com.wurmonline.server.skills.SkillList;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.db.BuyerScheduler;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Properties;

import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("ConstantConditions")
class BuyerMerchantTest extends WurmTradingTest {

    private Method method;
    private BuyerMerchant buyerMerchant;

    @BeforeEach
    void createBuyerMerchant() throws NoSuchFieldException, IllegalAccessException {
        buyerMerchant = new BuyerMerchant();
        ReflectionUtil.setPrivateField(buyerMerchant, BuyerMerchant.class.getDeclaredField("templateId"), factory.createBuyerContract().getTemplateId());
        method = mock(Method.class);
    }

    @Test
    void testItemTemplateNoException() {
        assertDoesNotThrow(new BuyerMerchant()::onItemTemplatesCreated);
    }

    @Test
    void traderBookBehaviourAction() throws Throwable {
        Item item1 = factory.createNewItem();
        Item item2 = factory.createBuyerContract();
        Object[] args1 = new Object[] {new Object(), owner, item2, Actions.MANAGE_TRADERS, 1.0f};
        Object[] args2 = new Object[] {new Object(), owner, item1, item2, Actions.MANAGE_TRADERS, 1.0f};

        InvocationHandler handler1 = (o, method, args) -> buyerMerchant.TraderBookBehaviourAction(o, method, args, (short)args[3], (Item)args[2], (Creature)args[1]);
        InvocationHandler handler2 = (o, method, args) -> buyerMerchant.TraderBookBehaviourAction(o, method, args, (short)args[4], (Item)args[3], (Creature)args[1]);

        ItemBehaviour traderBookBehaviour = mock(ItemBehaviour.class);
        assertEquals(true, handler1.invoke(traderBookBehaviour, method, args1));
        verify(method, never()).invoke(any(), any());
        String bml = factory.getCommunicator(owner).lastBmlContent;
        assertNotEquals(FakeCommunicator.empty, bml);

        assertEquals(true, handler2.invoke(traderBookBehaviour, method, args2));
        verify(method, never()).invoke(any(), any());
        assertNotEquals(FakeCommunicator.empty, factory.getCommunicator(owner).lastBmlContent);
        assertNotSame(bml, factory.getCommunicator(owner).lastBmlContent);
    }

    @Test
    void traderBookBehaviourActionWrongAction() throws Throwable {
        Item item1 = factory.createNewItem();
        Item item2 = factory.createBuyerContract();
        Object[] args1 = new Object[] {new Object(), owner, item2, Actions.MANAGE_TRADERS, 1.0f};
        Object[] args2 = new Object[] {new Object(), owner, item1, item2, Actions.MANAGE_TRADERS, 1.0f};

        InvocationHandler handler1 = (o, method, args) -> buyerMerchant.TraderBookBehaviourAction(o, method, args, (short)args[3], (Item)args[2], (Creature)args[1]);
        InvocationHandler handler2 = (o, method, args) -> buyerMerchant.TraderBookBehaviourAction(o, method, args, (short)args[4], (Item)args[3], (Creature)args[1]);

        for (short i = 50; i < 100; ++i) {
            if (i == Actions.MANAGE_TRADERS)
                continue;

            args1[3] = i;
            args2[4] = i;

            ItemBehaviour traderBookBehaviour = mock(ItemBehaviour.class);
            assertNull(handler1.invoke(traderBookBehaviour, method, args1));
            verify(method).invoke(traderBookBehaviour, args1);
            assertEquals(FakeCommunicator.empty, factory.getCommunicator(owner).lastBmlContent);

            assertNull(handler2.invoke(traderBookBehaviour, method, args2));
            verify(method).invoke(traderBookBehaviour, args2);
            assertEquals(FakeCommunicator.empty, factory.getCommunicator(owner).lastBmlContent);
        }
    }

    @Test
    void initiateTrade() throws Throwable {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(factory.getIsMetalId(),(byte)12);
        priceList.savePriceList();
        Object[] args1 = new Object[] { player, buyer };
        InvocationHandler handler = (o, method, args) -> buyerMerchant.initiateTrade(o, method, args);

        assertNull(handler.invoke(null, method, args1));

        assertNotNull(player.getTrade());
        assertTrue(factory.getCommunicator(player).sentStartTrading);
        assertNotNull(buyer.getTrade());
        assertTrue(factory.getCommunicator(buyer).sentStartTrading);
        assertEquals(1, player.getTrade().getTradingWindow(1).getItems().length);

        verify(method, never()).invoke(any(), any());
    }

    @Test
    void initiateTradeNoBuyer() throws Throwable {
        Object[] args1 = new Object[] { player, player };
        InvocationHandler handler = (o, method, args) -> buyerMerchant.initiateTrade(o, method, args);

        assertNull(handler.invoke(null, method, args1));

        assertNull(player.getTrade());
        assertFalse(factory.getCommunicator(player).sentStartTrading);

        verify(method, times(1)).invoke(any(), any());
    }

    @Test
    void initiateTradeBuyerLostList() throws Throwable {
        ItemsPackageFactory.removeItem(buyer, buyer.getInventory().getFirstContainedItem());
        Object[] args1 = new Object[] { player, buyer };
        InvocationHandler handler = (o, method, args) -> buyerMerchant.initiateTrade(o, method, args);

        assertNull(handler.invoke(null, method, args1));

        assertNull(player.getTrade());
        assertFalse(factory.getCommunicator(player).sentStartTrading);
        assertNull(buyer.getTrade());
        assertFalse(factory.getCommunicator(buyer).sentStartTrading);
        assertThat(player, receivedMessageContaining("misplaced their price list"));

        verify(method, never()).invoke(any(), any());
    }

    @Test
    void getTradeHandler() throws Throwable {
        InvocationHandler handler = (o, method, args) -> buyerMerchant.getTradeHandler(o, method, args);

        makeBuyerTrade();
        Object buyerHandler = handler.invoke(buyer, method, null);
        assertTrue(buyerHandler instanceof BuyerHandler);
        assertSame(buyerHandler, buyer.getTradeHandler());

        verify(method, never()).invoke(any(), any());
    }

    @Test
    void getTradeHandlerNotBuyer() throws Throwable {
        InvocationHandler handler = (o, method, args) -> buyerMerchant.getTradeHandler(o, method, args);

        makeBuyerTrade();
        Object buyerHandler = handler.invoke(player, method, null);
        assertNull(buyerHandler);

        verify(method, times(1)).invoke(any(), any());
    }

    @Test
    void swapOwners() throws Throwable {
        Item contract1 = spy(factory.createBuyerContract());
        Item contract2 = spy(factory.createBuyerContract());
        int realContractId = contract1.getTemplateId();

        player.getInventory().insertItem(contract1);
        player.getInventory().insertItem(contract2);

        trade = new Trade(player, owner);
        trade.getCreatureOneRequestWindow().addItem(contract1);
        trade.getCreatureOneRequestWindow().addItem(contract2);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.swapOwners(o, method, args);

        assertNull(handler.invoke(trade.getCreatureOneRequestWindow(), method, null));
        assertEquals(realContractId, contract1.getTemplateId());
        assertEquals(realContractId, contract2.getTemplateId());
        verify(contract1, times(1)).setTemplateId(300);
        verify(contract1, times(1)).setTemplateId(realContractId);
        verify(contract2, times(1)).setTemplateId(300);
        verify(contract2, times(1)).setTemplateId(realContractId);
        verify(method, times(1)).invoke(trade.getCreatureOneRequestWindow(), (Object[])null);
    }

    @Test
    void swapOwnersDoesNotAlterOtherItems() throws Throwable {
        Item hatchet = spy(factory.createNewItem(factory.getIsMetalId()));
        Item log = spy(factory.createNewItem(factory.getIsWoodId()));

        player.getInventory().insertItem(hatchet);
        player.getInventory().insertItem(log);

        trade = new Trade(player, owner);
        trade.getCreatureTwoRequestWindow().addItem(hatchet);
        trade.getCreatureTwoRequestWindow().addItem(log);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.swapOwners(o, method, args);

        assertNull(handler.invoke(trade.getCreatureTwoRequestWindow(), method, null));
        verify(hatchet, times(0)).setTemplateId(anyInt());
        verify(log, times(0)).setTemplateId(anyInt());
        verify(method, times(1)).invoke(trade.getCreatureTwoRequestWindow(), (Object[])null);
    }

    @Test
    void swapOwnersActualTrade() throws Throwable {
        Item contract1 = factory.createBuyerContract();
        Item contract2 = factory.createBuyerContract();
        int realContractId = contract1.getTemplateId();

        player.getInventory().insertItem(contract1);
        player.getInventory().insertItem(contract2);

        trade = new Trade(player, owner);
        trade.getCreatureTwoRequestWindow().addItem(contract1);
        trade.getCreatureTwoRequestWindow().addItem(contract2);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.swapOwners(o, method, args);

        Method method = TradingWindow.class.getDeclaredMethod("swapOwners");
        method.setAccessible(true);

        assertNull(handler.invoke(trade.getCreatureTwoRequestWindow(), method, null));
        assertEquals(realContractId, contract1.getTemplateId());
        assertEquals(realContractId, contract2.getTemplateId());

        assertEquals(owner.getWurmId(), contract1.getOwnerId());
        assertEquals(owner.getWurmId(), contract2.getOwnerId());

        assertEquals(0, player.getInventory().getItemCount());
        assertEquals(2, owner.getInventory().getItemCount());
    }

    @Test
    void swapOwnersCleanupOnError() throws Throwable {
        Item contract1 = spy(factory.createBuyerContract());
        int realContractId = contract1.getTemplateId();

        player.getInventory().insertItem(contract1);

        trade = new Trade(player, owner);
        trade.getCreatureTwoRequestWindow().addItem(contract1);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.swapOwners(o, method, args);
        when(method.invoke(any(), any())).thenThrow(new InvocationTargetException(new NoSuchItemException("")));

        try {
            handler.invoke(trade.getCreatureTwoRequestWindow(), method, null);
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof NoSuchItemException))
                throw e;
        }
        assertEquals(realContractId, contract1.getTemplateId());
        verify(contract1, times(1)).setTemplateId(300);
        verify(contract1, times(1)).setTemplateId(realContractId);
    }

    @Test
    void swapOwnersNoContracts() {
        Item item = factory.createNewCopperCoin();

        player.getInventory().insertItem(item);

        trade = new Trade(player, owner);
        trade.getCreatureTwoRequestWindow().addItem(item);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.swapOwners(o, method, args);

        assertDoesNotThrow(() -> handler.invoke(trade.getCreatureTwoRequestWindow(), method, null));
    }

    @Test
    void swapOwnersBuyerContractRestockedOnSaleAtTrader() throws Throwable {
        Creature trader = factory.createNewTrader();
        Item contract1 = factory.createBuyerContract();
        Item contract2 = factory.createNewItem(ItemList.merchantContract);
        int realContractId = contract1.getTemplateId();

        trader.getInventory().insertItem(contract1);
        trader.getInventory().insertItem(contract2);

        trade = new Trade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        trade.getCreatureOneRequestWindow().addItem(contract1);
        trade.getTradingWindow(1).addItem(contract2);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.swapOwners(o, method, args);

        Method method = TradingWindow.class.getDeclaredMethod("swapOwners");
        method.setAccessible(true);

        assertNull(handler.invoke(trade.getCreatureOneRequestWindow(), method, null));
        assertEquals(realContractId, contract1.getTemplateId());

        assertEquals(player.getWurmId(), contract1.getOwnerId());

        assertEquals(2, trader.getInventory().getItemCount());
        assertEquals(1, player.getInventory().getItemCount());
        assertFalse(trader.getInventory().getItems().contains(contract1));
        assertTrue(player.getInventory().getItems().contains(contract1));
    }

    @Test
    void swapOwnersContractsRestockedCorrectlyEvenWhenBoth() throws Throwable {
        Creature trader = factory.createNewTrader();
        Item contract1 = factory.createBuyerContract();
        Item contract2 = factory.createNewItem(ItemList.merchantContract);
        int realContractId = contract1.getTemplateId();

        trader.getInventory().insertItem(contract1);
        trader.getInventory().insertItem(contract2);

        trade = new Trade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        trade.getCreatureOneRequestWindow().addItem(contract1);
        trade.getCreatureOneRequestWindow().addItem(contract2);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.swapOwners(o, method, args);

        Method method = TradingWindow.class.getDeclaredMethod("swapOwners");
        method.setAccessible(true);

        assertNull(handler.invoke(trade.getCreatureOneRequestWindow(), method, null));
        assertEquals(realContractId, contract1.getTemplateId());

        assertEquals(player.getWurmId(), contract1.getOwnerId());

        assertEquals(2, trader.getInventory().getItemCount());
        assertEquals(2, player.getInventory().getItemCount());
        assertFalse(trader.getInventory().getItems().contains(contract1));
        assertTrue(player.getInventory().getItems().contains(contract1));
        assertFalse(trader.getInventory().getItems().contains(contract2));
        assertTrue(player.getInventory().getItems().contains(contract1));
        assertTrue(trader.getInventory().getItems().stream().allMatch(item ->
                  item.getTemplateId() == factory.createBuyerContract().getTemplateId() || item.getTemplateId() == ItemList.merchantContract));
    }

    @Test
    void createShop() throws Throwable {
        Creature trader = factory.createNewTrader();
        InvocationHandler handler = (o, method, args) -> buyerMerchant.createShop(o, method, args);

        Object[] args1 = new Object[] {trader};
        handler.invoke(null, method, args1);

        assertEquals(1, trader.getInventory().getItemCount());
        assertEquals(factory.createBuyerContract().getTemplateId(), trader.getInventory().getFirstContainedItem().getTemplateId());

        verify(method, times(1)).invoke(null, args1);
    }

    @Test
    void createShopNoContractsOnTraders() throws Throwable {
        Properties properties = new Properties();
        properties.setProperty("contracts_on_traders", "false");
        buyerMerchant.configure(properties);

        Creature trader = factory.createNewTrader();
        InvocationHandler handler = (o, method, args) -> buyerMerchant.createShop(o, method, args);

        Object[] args1 = new Object[] {trader};
        handler.invoke(null, method, args1);

        assertEquals(0, trader.getInventory().getItemCount());

        verify(method, times(1)).invoke(null, args1);
    }

    @Test
    void actionMANAGECanDismiss() throws Throwable {
        try {
            player.setPower((byte)5);
            Servers.setTestServer(true);
            Action action = mock(Action.class);
            CreatureBehaviour cb = spy(CreatureBehaviour.class);
            InvocationHandler handler = (o, method, args) -> buyerMerchant.action(o, method, args);
            Object[] args1 = new Object[]{action, player, buyer, Actions.MANAGE, 1.0f};

            assertEquals(true, handler.invoke(cb, method, args1));
            verify(method, never()).invoke(null, args1);
            assertEquals(1, Questions.getNumUnanswered());
            assertTrue(factory.getCommunicator(player).lastBmlContent.contains("dismiss this buyer"));
        } finally {
            Servers.setTestServer(false);
        }
    }

    @Test
    void actionMANAGECannotDismiss() throws Throwable {
        Action action = mock(Action.class);
        CreatureBehaviour cb = spy(CreatureBehaviour.class);
        InvocationHandler handler = (o, method, args) -> buyerMerchant.action(o, method, args);
        Object[] args1 = new Object[]{action, player, buyer, Actions.MANAGE, 1.0f};

        handler.invoke(cb, method, args1);
        verify(method, times(1)).invoke(cb, args1);
        assertEquals(0, Questions.getNumUnanswered());
        assertEquals(FakeCommunicator.empty, factory.getCommunicator(player).lastBmlContent);
    }

    @Test
    void actionMANAGENotBuyer() throws Throwable {
        CreatureBehaviour cb = spy(CreatureBehaviour.class);
        InvocationHandler handler = (o, method, args) -> buyerMerchant.action(o, method, args);

        Object[] args1 = new Object[] {new Object(), owner, player, Actions.MANAGE, 1.0f};
        handler.invoke(cb, method, args1);
        verify(method, times(1)).invoke(cb, args1);
        assertEquals(0, Questions.getNumUnanswered());
    }

    @Test
    void actionTHREATENPriceListNotDropped() throws Throwable {
        try {
            Item item = factory.createNewItem(factory.getIsMetalId());
            Item priceList = buyer.getInventory().getFirstContainedItem();
            buyer.getInventory().insertItem(item);
            Creature enemy = factory.createNewPlayer();
            enemy.currentKingdom = Kingdom.KINGDOM_HOTS;
            enemy.getSkills().learn(SkillList.TAUNTING, 100.0f);
            Servers.setHostileServer(true);
            CreatureBehaviour cb = spy(CreatureBehaviour.class);
            Action action = mock(Action.class);
            when(action.getTimeLeft()).thenReturn(10);
            InvocationHandler handler = (o, method, args) -> buyerMerchant.action(o, method, args);

            float counter = 2.0f;
            assert counter * 10.0f > action.getTimeLeft();
            Object[] args1 = new Object[] {action, enemy, buyer, Actions.THREATEN, counter};

            handler.invoke(cb, CreatureBehaviour.class.getDeclaredMethod("action", Action.class, Creature.class, Creature.class, short.class, float.class), args1);
            assertTrue(buyer.getInventory().getItems().contains(priceList));
            assertFalse(buyer.getInventory().getItems().contains(item));
        } finally {
            Servers.setHostileServer(false);
        }
    }

    @Test
    void actionTHREATENNotEnoughTimeElapsed() throws Throwable {
        try {
            Item item = factory.createNewItem(factory.getIsMetalId());
            Item priceList = buyer.getInventory().getFirstContainedItem();
            buyer.getInventory().insertItem(item);
            Creature enemy = factory.createNewPlayer();
            enemy.currentKingdom = Kingdom.KINGDOM_HOTS;
            enemy.getSkills().learn(SkillList.TAUNTING, 100.0f);
            Servers.setHostileServer(true);
            CreatureBehaviour cb = spy(CreatureBehaviour.class);
            Action action = mock(Action.class);
            when(action.getTimeLeft()).thenReturn(10);
            InvocationHandler handler = (o, method, args) -> buyerMerchant.action(o, method, args);

            float counter = 0.5f;
            assert !(counter * 10.0f > action.getTimeLeft());
            Object[] args1 = new Object[] {action, enemy, buyer, Actions.THREATEN, counter};

            handler.invoke(cb, CreatureBehaviour.class.getDeclaredMethod("action", Action.class, Creature.class, Creature.class, short.class, float.class), args1);
            assertTrue(buyer.getInventory().getItems().contains(priceList));
            assertTrue(buyer.getInventory().getItems().contains(item));
        } finally {
            Servers.setHostileServer(false);
        }
    }

    @Test
    void actionTHREATENNotBuyerUnaffected() throws Throwable {
        try {
            Item item = factory.createNewItem(factory.getIsMetalId());
            Item priceList = buyer.getInventory().getFirstContainedItem();
            assert PriceList.isPriceList(priceList);
            buyer.setName("Merchant_Albert");
            assert !BuyerMerchant.isBuyer(buyer);
            buyer.getInventory().insertItem(item);
            Creature enemy = factory.createNewPlayer();
            enemy.currentKingdom = Kingdom.KINGDOM_HOTS;
            enemy.getSkills().learn(SkillList.TAUNTING, 100.0f);
            Servers.setHostileServer(true);
            CreatureBehaviour cb = spy(CreatureBehaviour.class);
            Action action = mock(Action.class);
            when(action.getTimeLeft()).thenReturn(10);
            InvocationHandler handler = (o, method, args) -> buyerMerchant.action(o, method, args);

            float counter = 2.0f;
            assert counter * 10.0f > action.getTimeLeft();
            Object[] args1 = new Object[] {action, enemy, buyer, Actions.THREATEN, counter};

            handler.invoke(cb, CreatureBehaviour.class.getDeclaredMethod("action", Action.class, Creature.class, Creature.class, short.class, float.class), args1);
            assertFalse(buyer.getInventory().getItems().contains(priceList));
            assertFalse(buyer.getInventory().getItems().contains(item));
        } finally {
            Servers.setHostileServer(false);
        }
    }

    @Test
    void giveAction() throws Throwable {
        CreatureBehaviour cb = spy(CreatureBehaviour.class);
        Action action = mock(Action.class);
        InvocationHandler handler = (o, method, args) -> buyerMerchant.giveAction(o, method, args);

        Item coin = factory.createNewCopperCoin();
        player.getInventory().insertItem(coin);
        player.setPower((byte)2);
        Object[] args1 = new Object[] {action, player, coin, buyer, Actions.GIVE, 1.0f};
        handler.invoke(cb, CreatureBehaviour.class.getDeclaredMethod("action", Action.class, Creature.class, Item.class, Creature.class, short.class, float.class), args1);
        assertFalse(player.getInventory().getItems().contains(coin));
        assertTrue(buyer.getInventory().getItems().contains(coin));
        assertEquals(100, factory.getShop(buyer).getMoney());
    }

    @Test
    void giveActionDoesNotAddToShopIfNotBuyer() throws Throwable {
        CreatureBehaviour cb = spy(CreatureBehaviour.class);
        Action action = mock(Action.class);
        Item coin = factory.createNewCopperCoin();
        when(method.invoke(any(), any())).then((Answer<Boolean>)i -> {
            owner.getInventory().insertItem(coin);
            return true;
        });
        InvocationHandler handler = (o, method, args) -> buyerMerchant.giveAction(o, method, args);


        player.getInventory().insertItem(coin);
        player.setPower((byte)2);
        Object[] args1 = new Object[] {action, player, coin, owner, Actions.GIVE, 1.0f};
        assertDoesNotThrow(() -> handler.invoke(cb, method, args1));
        assertFalse(player.getInventory().getItems().contains(coin));
        assertTrue(owner.getInventory().getItems().contains(coin));
    }

    @Test
    void giveActionDoesNotAddToShopIfNotCoin() throws Throwable {
        CreatureBehaviour cb = spy(CreatureBehaviour.class);
        Action action = mock(Action.class);
        Item notCoin = factory.createNewItem(factory.getIsWoodId());
        when(method.invoke(any(), any())).then((Answer<Boolean>)i -> {
            buyer.getInventory().insertItem(notCoin);
            return true;
        });
        InvocationHandler handler = (o, method, args) -> buyerMerchant.giveAction(o, method, args);

        player.getInventory().insertItem(notCoin);
        player.setPower((byte)2);
        Object[] args1 = new Object[] {action, player, notCoin, buyer, Actions.GIVE, 1.0f};
        handler.invoke(cb, method, args1);
        verify(method, times(1)).invoke(cb, args1);
        assertFalse(player.getInventory().getItems().contains(notCoin));
        assertTrue(buyer.getInventory().getItems().contains(notCoin));
        assertEquals(0, factory.getShop(buyer).getMoney());
    }

    @Test
    void handle_EXAMINE() throws Throwable {
        assert BuyerScheduler.getUpdatesFor(buyer).length == 0;
        InvocationHandler handler = (o, method, args) -> buyerMerchant.handle_EXAMINE(o, method, args);
        Method getNoDb = Players.class.getDeclaredMethod("getInstanceForUnitTestingWithoutDatabase");
        getNoDb.setAccessible(true);
        getNoDb.invoke(null);

        Object[] args1 = new Object[] {player, buyer};

        handler.invoke(null, method, args1);

        assertEquals(2, factory.getCommunicator(player).getMessages().length);
        assertThat(player, receivedMessageContaining("buying items on behalf of Owner"));
        assertEquals("He has a normal build.", factory.getCommunicator(player).lastNormalServerMessage);
    }

    @Test
    void handle_EXAMINEWithUpdates() throws Throwable {
        BuyerScheduler.addUpdateFor(buyer, ItemTemplateFactory.getInstance().getTemplate(ItemList.pickAxe), (byte)0, 1, 1.0f, 1, 1, 1, false, 1);
        BuyerScheduler.Update[] updates = BuyerScheduler.getUpdatesFor(buyer);
        assert updates.length == 1;
        InvocationHandler handler = (o, method, args) -> buyerMerchant.handle_EXAMINE(o, method, args);
        Method getNoDb = Players.class.getDeclaredMethod("getInstanceForUnitTestingWithoutDatabase");
        getNoDb.setAccessible(true);
        getNoDb.invoke(null);

        Object[] args1 = new Object[] {player, buyer};
        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        ReflectionUtil.setPrivateField(null, BuyerScheduler.class.getDeclaredField("clock"), clock);
        ReflectionUtil.setPrivateField(updates[0], BuyerScheduler.Update.class.getDeclaredField("lastUpdated"), clock.millis());

        handler.invoke(null, method, args1);

        String[] messages = factory.getCommunicator(player).getMessages();
        assertEquals(3, messages.length);
        assertThat(player, receivedMessageContaining("buying items on behalf of Owner"));
        assertEquals("He has a normal build.", messages[1]);
        assertThat(player, receivedMessageContaining("update their stock in 1 hours."));
    }

    @Test
    void handle_EXAMINENoSuchPlayerAsOwner() throws Throwable {
        InvocationHandler handler = (o, method, args) -> buyerMerchant.handle_EXAMINE(o, method, args);
        Players players = mock(Players.class);
        ReflectionUtil.setPrivateField(null, Players.class.getDeclaredField("instance"), players);
        when(players.getNameFor(anyLong())).thenThrow(NoSuchPlayerException.class);

        long id = 199999999;
        assertThrows(NoSuchPlayerException.class, () -> Players.getInstance().getNameFor(id));
        factory.getShop(buyer).setOwner(id);
        Object[] args1 = new Object[] {player, buyer};

        handler.invoke(null, method, args1);

        assertEquals(2, factory.getCommunicator(player).getMessages().length);
        assertThat(player, receivedMessageContaining("buying items."));
        assertFalse(factory.getCommunicator(player).getMessages()[0].contains("Owner"));
        assertEquals("He has a normal build.", factory.getCommunicator(player).lastNormalServerMessage);
    }

    @Test
    void handle_EXAMINENotBuyer() throws Throwable {
        InvocationHandler handler = (o, method, args) -> buyerMerchant.handle_EXAMINE(o, method, args);
        Method getNoDb = Players.class.getDeclaredMethod("getInstanceForUnitTestingWithoutDatabase");
        getNoDb.setAccessible(true);
        getNoDb.invoke(null);

        Object[] args1 = new Object[] {player, owner};

        handler.invoke(null, method, args1);

        assertEquals(0, factory.getCommunicator(player).getMessages().length);
        verify(method, times(1)).invoke(null, args1);
    }

    @Test
    void dismissMerchant() throws Throwable {
        InvocationHandler handler = (o, method, args) -> buyerMerchant.dismissMerchant(o, method, args);
        Object[] args1 = new Object[] {player, buyer.getWurmId()};

        handler.invoke(null, method, args1);
        verify(method, never()).invoke(null, args1);
        assertTrue(buyer.isDead());
        assertThrows(NoSuchCreatureException.class, () -> factory.getCreature(buyer.getWurmId()));
    }

    @Test
    void dismissMerchantNotBuyer() throws Throwable {
        ItemsPackageFactory.removeItem(buyer, buyer.getInventory().getFirstContainedItem());
        buyer.setName("Merchant_Fred");
        assert !BuyerMerchant.isBuyer(buyer);
        InvocationHandler handler = (o, method, args) -> buyerMerchant.dismissMerchant(o, method, args);
        Object[] args1 = new Object[] {player, buyer.getWurmId()};

        handler.invoke(null, method, args1);
        verify(method, times(1)).invoke(null, args1);
    }

    @Test
    void die() throws Throwable {
        Item priceList = buyer.getInventory().getFirstContainedItem();
        assert priceList.hasNoDecay();
        InvocationHandler handler = (o, method, args) -> buyerMerchant.die(o, method, args);
        Object[] args1 = new Object[] {true, ""};

        handler.invoke(buyer, method, args1);
        verify(method, times(1)).invoke(buyer, args1);
        assertFalse(priceList.hasNoDecay());
    }

    @Test
    void dieNotCalledOnOthers() throws Throwable {
        InvocationHandler handler = (o, method, args) -> buyerMerchant.die(o, method, args);
        Object[] args1 = new Object[] {true, ""};

        Creature p = spy(player);
        handler.invoke(p, method, args1);
        verify(method, times(1)).invoke(p, args1);
        verify(p, never()).getInventory();
    }

    @Test
    void stopLoggers() throws Throwable {
        InvocationHandler handler = (o, method, args) -> buyerMerchant.stopLoggers(o, method, args);

        handler.invoke(null, method, null);
        verify(method, times(1)).invoke(null, (Object[])null);
    }

    @Test
    void turnToPlayerMaxPower() throws Throwable {
        Properties properties = new Properties();
        properties.setProperty("turn_to_player_max_power", "1");
        buyerMerchant.configure(properties);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.initiateTrade(o, method, args);
        Object[] args1 = new Object[]{player, buyer};
        assert Math.abs(buyer.getStatus().getRotation() - 180.0f) < 0.001f;

        handler.invoke(null, method, args1);
        assertNotEquals(180.0f, buyer.getStatus().getRotation());
    }

    @Test
    void turnToPlayerMaxPowerLowerThanPlayerPower() throws Throwable {
        player.setPower((byte)5);
        Properties properties = new Properties();
        properties.setProperty("turn_to_player_max_power", "1");
        buyerMerchant.configure(properties);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.initiateTrade(o, method, args);
        Object[] args1 = new Object[]{player, buyer};
        assert Math.abs(buyer.getStatus().getRotation() - 180.0f) < 0.001f;

        handler.invoke(null, method, args1);
        assertEquals(180.0f, buyer.getStatus().getRotation());
    }

    @Test
    void turnToPlayerMaxPowerAtMaxPower() throws Throwable {
        player.setPower((byte)5);
        Properties properties = new Properties();
        properties.setProperty("turn_to_player_max_power", "5");
        buyerMerchant.configure(properties);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.initiateTrade(o, method, args);
        Object[] args1 = new Object[]{player, buyer};
        assert Math.abs(buyer.getStatus().getRotation() - 180.0f) < 0.001f;

        handler.invoke(null, method, args1);
        assertNotEquals(180.0f, buyer.getStatus().getRotation());
    }

    @Test
    void turnToPlayerMaxPowerMerchantsAndTraders() throws Throwable {
        Creature merchant = factory.createNewMerchant(owner);
        Creature trader = factory.createNewTrader();
        player.setPower((byte)5);
        Properties properties = new Properties();
        properties.setProperty("turn_to_player_max_power", "5");
        buyerMerchant.configure(properties);

        InvocationHandler handler = (o, method, args) -> buyerMerchant.initiateTrade(o, method, args);
        Object[] args1 = new Object[]{player, merchant};
        assert Math.abs(merchant.getStatus().getRotation() - 180.0f) < 0.001f;
        Object[] args2 = new Object[]{player, trader};
        assert Math.abs(trader.getStatus().getRotation() - 180.0f) < 0.001f;

        handler.invoke(null, method, args1);
        assertNotEquals(180.0f, merchant.getStatus().getRotation());
        handler.invoke(null, method, args2);
        assertNotEquals(180.0f, trader.getStatus().getRotation());
    }

    @Test
    void testUpdateTradersContractsOnTrader() throws IllegalAccessException, NoSuchFieldException {
        Field contractsOnTraders = BuyerMerchant.class.getDeclaredField("contractsOnTraders");
        contractsOnTraders.setAccessible(true);
        assert contractsOnTraders.getBoolean(buyerMerchant);

        Properties properties = new Properties();
        properties.setProperty("update_traders", "true");
        buyerMerchant.configure(properties);

        Creature trader = factory.createNewTrader();
        assert trader.getInventory().getItemCount() == 0;

        buyerMerchant.onServerStarted();

        assertEquals(1, trader.getInventory().getItemCount());
        assertEquals(factory.createBuyerContract().getTemplateId(), trader.getInventory().getFirstContainedItem().getTemplateId());
    }

    @Test
    void testUpdateTradersNoContractsOnTrader() {
        Properties properties = new Properties();
        properties.setProperty("update_traders", "true");
        properties.setProperty("contracts_on_traders", "false");
        buyerMerchant.configure(properties);

        Creature trader = factory.createNewTrader();
        trader.getInventory().insertItem(factory.createBuyerContract());
        assert trader.getInventory().getItemCount() == 1;

        buyerMerchant.onServerStarted();

        assertEquals(0, trader.getInventory().getItemCount());
    }

    @Test
    void testUpdateTradersOnlyOneContract() {
        Properties properties = new Properties();
        properties.setProperty("update_traders", "true");
        buyerMerchant.configure(properties);

        Creature trader = factory.createNewTrader();
        Item contract = factory.createBuyerContract();
        trader.getInventory().insertItem(contract);
        assert trader.getInventory().getItemCount() == 1;

        buyerMerchant.onServerStarted();

        assertEquals(1, trader.getInventory().getItemCount());
        assertSame(contract, trader.getInventory().getFirstContainedItem());
    }

    @Test
    void testMaxItemsOnBuyerOver1000() throws PriceList.NoPriceListOnBuyer, EntryBuilder.EntryBuilderException, PriceList.PageNotAdded, PriceList.PriceListFullException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int maxItems = 1500;
        Properties properties = new Properties();
        properties.setProperty("max_items", String.valueOf(maxItems));
        buyerMerchant.configure(properties);

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        EntryBuilder.addEntry(priceList).templateId(ItemList.rake).price(1).build();
        priceList.savePriceList();

        buyerMerchant.onServerStarted();
        assertEquals(maxItems + 1, BuyerHandler.getMaxNumPersonalItems(buyer));

        factory.getShop(buyer).setMoney(Integer.MAX_VALUE);
        factory.createManyItems(ItemList.rake, maxItems).forEach(player.getInventory()::insertItem);

        makeBuyerTrade();
        player.getInventory().getItems().forEach(trade.getTradingWindow(2)::addItem);
        TradeHandler tradeHandler = buyer.getTradeHandler();
        assert tradeHandler != null;
        //noinspection ConstantConditions
        BuyerHandler handler = (BuyerHandler)tradeHandler;
        ReflectionUtil.callPrivateMethod(handler, BuyerHandler.class.getDeclaredMethod("balance"));

        assertEquals(maxItems, trade.getTradingWindow(4).getItems().length);
    }

    @Test
    void testDestroyBoughtItemsStillSetsMerchantMaxItems() {
        int maxItems = 1500;
        Properties properties = new Properties();
        properties.setProperty("max_items", String.valueOf(maxItems));
        properties.setProperty("apply_max_to_merchants", "true");
        properties.setProperty("destroy_bought_items", "true");
        buyerMerchant.configure(properties);

        buyerMerchant.onServerStarted();
        assertEquals(Integer.MAX_VALUE, BuyerHandler.getMaxNumPersonalItems(buyer));
        assertEquals(maxItems, TradeHandler.getMaxNumPersonalItems());
    }

    @Test
    void testWillLeaveServerEmptyContract() throws Throwable {
        BuyerMerchant mod = new BuyerMerchant();
        InvocationHandler handler = mod::willLeaveServer;
        Item item = factory.createNewItem(mod.getContractTemplateId());
        Method method = mock(Method.class);
        Object[] args = new Object[] { true, false, false };

        assertTrue((Boolean)handler.invoke(item, method, args));
        assertFalse(item.isTransferred());
        verify(method, never()).invoke(item, args);
    }

    @Test
    void testWillLeaveServerUsedContract() throws Throwable {
        Player gm = factory.createNewPlayer();

        BuyerMerchant mod = new BuyerMerchant();
        InvocationHandler handler = mod::willLeaveServer;
        Item item = factory.createNewItem(mod.getContractTemplateId());
        item.setData(gm.getWurmId());
        Method method = mock(Method.class);
        Object[] args = new Object[] { true, false, false };

        assertFalse((Boolean)handler.invoke(item, method, args));
        assertTrue(item.isTransferred());
        verify(method, never()).invoke(item, args);
    }

    @Test
    void testWillLeaveServerNotContract() throws Throwable {
        Player gm = factory.createNewPlayer();

        InvocationHandler handler = new BuyerMerchant()::willLeaveServer;
        Item item = factory.createNewItem(ItemList.lunchbox);
        item.setData(gm.getWurmId());
        boolean isTransferred = item.isTransferred();
        Method method = mock(Method.class);
        Object[] args = new Object[] { true, false, false };

        assertNull(handler.invoke(item, method, args));
        assertEquals(isTransferred, item.isTransferred());
        verify(method, times(1)).invoke(item, args);
    }
}
