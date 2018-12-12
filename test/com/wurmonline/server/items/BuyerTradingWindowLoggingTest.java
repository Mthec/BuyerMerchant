package com.wurmonline.server.items;

import mod.wurmunlimited.WurmTradingTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

class BuyerTradingWindowLoggingTest extends WurmTradingTest {

    private Logger logger;

    private void attachSpyLogger() {
        try {
            logger = spy(Logger.getLogger(BuyerTradingWindow.class.getName()));
            Field windowLogger = BuyerTradingWindow.class.getDeclaredField("logger");
            windowLogger.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(windowLogger, windowLogger.getModifiers() & ~Modifier.FINAL);
            windowLogger.set(null, logger);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testPayingOutToOwner() {
        makeOwnerBuyerTrade();
        attachSpyLogger();
        Item coin = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(coin);
        factory.getShop(buyer).setMoney(100);
        trade.getCreatureOneRequestWindow().addItem(coin);
        trade.getCreatureOneRequestWindow().swapOwners();

        verify(logger, times(1)).info(contains("Paying out 100 to owner"));
        verify(logger, times(1)).info(contains("My shop is now at 0"));
    }

    @Test
    void testOwnerPayingIn() {
        makeOwnerBuyerTrade();
        attachSpyLogger();
        Item coin = factory.createNewCopperCoin();
        owner.getInventory().insertItem(coin);
        trade.getCreatureTwoRequestWindow().addItem(coin);
        trade.getCreatureTwoRequestWindow().swapOwners();

        verify(logger, times(1)).info(contains("My owner just gave me 100"));
        verify(logger, times(1)).info(contains("My shop is now at 100"));
    }

    @Test
    void testPayingOutToPlayer() {
        makeBuyerTrade();
        attachSpyLogger();
        Item coin = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(coin);
        factory.getShop(buyer).setMoney(110);
        trade.getCreatureOneRequestWindow().addItem(coin);
        trade.getCreatureOneRequestWindow().swapOwners();

        verify(logger, times(1)).info(contains("Paying King - 10"));
        verify(logger, times(1)).info(contains("Paying out 100 to " + player.getName()));
        verify(logger, times(1)).info(contains("My shop is now at 0"));

    }
}
