package com.wurmonline.server.economy;

import com.wurmonline.server.creatures.Creature;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.mockito.Mockito.mock;

public class FakeShop extends Shop {
    public int merchantDataInt;
    public long merchantDataLong;

    private FakeShop(long aWurmId, long aMoney) {
        super(aWurmId, aMoney);
    }

    public static FakeShop createFakeShop(Creature trader, Creature owner) {
        return createFakeShop(trader.getWurmId(), owner.getWurmId());
    }

    public static FakeShop createFakeShop() {
        return createFakeShop(0, -10L);
    }

    private static FakeShop createFakeShop(long wurmId, long ownerId) {
        Objenesis ob = new ObjenesisStd();
        FakeShop newShop = ob.newInstance(FakeShop.class);
        try {
            Field wurmid = Shop.class.getDeclaredField("wurmid");
            wurmid.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(wurmid, wurmid.getModifiers() & ~Modifier.FINAL);
            wurmid.set(newShop, wurmId);

            Field localSupplyDemand = Shop.class.getDeclaredField("localSupplyDemand");
            localSupplyDemand.setAccessible(true);
            modifiers.setInt(localSupplyDemand, localSupplyDemand.getModifiers() & ~Modifier.FINAL);
            localSupplyDemand.set(newShop, mock(LocalSupplyDemand.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        newShop.money = 0;
        newShop.ownerId = ownerId;

        return newShop;
    }

    public static FakeShop createFakeTraderShop(long wurmId) {
        Objenesis ob = new ObjenesisStd();
        FakeShop newShop = ob.newInstance(FakeShop.class);
        try {
            Field wurmid = Shop.class.getDeclaredField("wurmid");
            wurmid.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(wurmid, wurmid.getModifiers() & ~Modifier.FINAL);
            wurmid.set(newShop, wurmId);

            Field localSupplyDemand = Shop.class.getDeclaredField("localSupplyDemand");
            localSupplyDemand.setAccessible(true);
            modifiers.setInt(localSupplyDemand, localSupplyDemand.getModifiers() & ~Modifier.FINAL);
            localSupplyDemand.set(newShop, mock(LocalSupplyDemand.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return newShop;
    }

    public void setToNotPersonalTrader() {
        ownerId = -10;
    }

    @Override
    void create() {
        // Nothing to do.
    }

    @Override
    boolean traderMoneyExists() {
        return false;
    }

    @Override
    public void setMoney(long var1) {
        money = var1;
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setPriceModifier(float var1) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setFollowGlobalPrice(boolean var1) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setUseLocalPrice(boolean var1) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setLastPolled(long var1) {
        lastPolled = var1;
    }

    @Override
    public void setTax(float var1) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void addMoneyEarned(long var1) {
        System.out.println(var1 + " earned.");
    }

    @Override
    public void addMoneySpent(long var1) {
        moneySpent += var1;
    }

    @Override
    public void resetEarnings() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void addTax(long var1) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setOwner(long var1) {
        ownerId = var1;
    }

    @Override
    public void setMerchantData(int var1, long var2) {
        merchantDataInt = var1;
        merchantDataLong = var2;
    }
}
