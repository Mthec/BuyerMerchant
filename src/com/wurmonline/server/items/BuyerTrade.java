//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
// Modified from version by Code Club AB.
//

package com.wurmonline.server.items;

import com.wurmonline.server.creatures.BuyerHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.util.logging.Logger;

public class BuyerTrade extends Trade {
    private final BuyerTradingWindow creatureOneOfferWindow;
    private final BuyerTradingWindow creatureTwoOfferWindow;
    private final BuyerTradingWindow creatureOneRequestWindow;
    private final BuyerTradingWindow creatureTwoRequestWindow;
    private boolean creatureOneSatisfied = false;
    private boolean creatureTwoSatisfied = false;
    private int currentCounter = -1;
    private static final Logger logger = Logger.getLogger(Trade.class.getName());
    private long tax = 0L;

    public BuyerTrade(Creature playerCreature, Creature buyerCreature) throws PriceList.NoPriceListOnBuyer {
        PriceList.getPriceListFromBuyer(buyerCreature);

        this.creatureOne = playerCreature;
        this.creatureOne.startTrading();
        this.creatureTwo = buyerCreature;
        this.creatureTwo.startTrading();
        this.creatureTwoOfferWindow = new BuyerTradingWindow(buyerCreature, playerCreature, true, 1L, this);
        this.creatureOneOfferWindow = new BuyerTradingWindow(playerCreature, buyerCreature, true, 2L, this);
        this.creatureOneRequestWindow = new BuyerTradingWindow(buyerCreature, playerCreature, false, 3L, this);
        this.creatureTwoRequestWindow = new BuyerTradingWindow(playerCreature, buyerCreature, false, 4L, this);
    }

    @Override
    public void setMoneyAdded(long money) {
        throw new UnsupportedOperationException("This method is not used by buyers.");
    }

    @Override
    public void addShopDiff(long money) {
        throw new UnsupportedOperationException("This method is not used by buyers.");
    }

    @Override
    long getMoneyAdded() {
        throw new UnsupportedOperationException("This method is not used by buyers.");
    }

    @Override
    public TradingWindow getTradingWindow(long id) {
        if (id == OFFERWINTWO) {
            return this.creatureTwoOfferWindow;
        } else if (id == OFFERWINONE) {
            return this.creatureOneOfferWindow;
        } else {
            return id == REQUESTWINONE ? this.creatureOneRequestWindow : this.creatureTwoRequestWindow;
        }
    }

    @Override
    public void setSatisfied(Creature creature, boolean satisfied, int id) {
        if (id == this.currentCounter) {
            if (creature.equals(this.creatureOne)) {
                this.creatureOneSatisfied = satisfied;
            } else {
                this.creatureTwoSatisfied = satisfied;
            }

            if (this.creatureOneSatisfied && this.creatureTwoSatisfied) {
                if (this.makeBuyerTrade()) {
                    this.creatureOne.getCommunicator().sendCloseTradeWindow();
                    this.creatureTwo.getCommunicator().sendCloseTradeWindow();
                } else {
                    this.creatureOne.getCommunicator().sendTradeAgree(creature, satisfied);
                    this.creatureTwo.getCommunicator().sendTradeAgree(creature, satisfied);
                }
            } else {
                this.creatureOne.getCommunicator().sendTradeAgree(creature, satisfied);
                this.creatureTwo.getCommunicator().sendTradeAgree(creature, satisfied);
            }
        }

    }

    @Override
    int getNextTradeId() {
        return ++this.currentCounter;
    }

    // Renamed to avoid ambiguous call in testing.
    private boolean makeBuyerTrade() {
        if ((!this.creatureOne.isPlayer() || this.creatureOne.hasLink()) && !this.creatureOne.isDead()) {
            if ((!this.creatureTwo.isPlayer() || this.creatureTwo.hasLink()) && !this.creatureTwo.isDead()) {
                if (this.creatureOneRequestWindow.hasInventorySpace() && this.creatureTwoRequestWindow.hasInventorySpace()) {
                    int reqOneWeight = this.creatureOneRequestWindow.getWeight();
                    int reqTwoWeight = this.creatureTwoRequestWindow.getWeight();
                    int diff = reqOneWeight - reqTwoWeight;
                    if (diff > 0 && this.creatureOne instanceof Player && !this.creatureOne.canCarry(diff)) {
                        this.creatureTwo.getCommunicator().sendNormalServerMessage(this.creatureOne.getName() + " cannot carry that much.", (byte)3);
                        this.creatureOne.getCommunicator().sendNormalServerMessage("You cannot carry that much.", (byte)3);
                        if (this.creatureOne.getPower() > 0) {
                            this.creatureOne.getCommunicator().sendNormalServerMessage("You cannot carry that much. You would carry " + diff + " more.");
                        }

                        return false;
                    }

                    // Can remove, but will probably leave to keep methods similar.
                    diff = reqTwoWeight - reqOneWeight;
                    if (diff > 0 && this.creatureTwo instanceof Player && !this.creatureTwo.canCarry(diff)) {
                        this.creatureOne.getCommunicator().sendNormalServerMessage(this.creatureTwo.getName() + " cannot carry that much.", (byte)3);
                        this.creatureTwo.getCommunicator().sendNormalServerMessage("You cannot carry that much.", (byte)3);
                        return false;
                    }

                    boolean ok = this.creatureOneRequestWindow.validateTrade();
                    if (!ok) {
                        return false;
                    }

                    ok = this.creatureTwoRequestWindow.validateTrade();
                    if (ok) {
                        this.creatureOneRequestWindow.swapOwners();
                        this.creatureTwoRequestWindow.swapOwners();
                        ((BuyerHandler)creatureTwo.getTradeHandler()).setTradeSuccessful();

                        this.creatureTwoOfferWindow.endTrade();
                        this.creatureOneOfferWindow.endTrade();
                        Shop shop;
                        // Can remove, but will probably leave to keep methods similar.
                        if (this.creatureOne.isNpcTrader()) {
                            shop = Economy.getEconomy().getShop(this.creatureOne);
                            shop.setMerchantData(this.creatureOne.getNumberOfShopItems());
                        }

                        if (this.creatureTwo.isNpcTrader()) {
                            shop = Economy.getEconomy().getShop(this.creatureTwo);
                            // Minus price list.
                            shop.setMerchantData(this.creatureTwo.getNumberOfShopItems() - 1);
                        }

                        // shopDiff is the difference between moneySpent and moneyEarned, as it occurs across two TradingWindows
                        // it should be finalised here.  Does not get set for Buyers though.

                        this.creatureOne.setTrade(null);
                        this.creatureTwo.setTrade(null);
                        return true;
                    }
                }

                return false;
            } else {
                if (this.creatureTwo.hasLink()) {
                    this.creatureTwo.getCommunicator().sendNormalServerMessage("You may not trade right now.", (byte)3);
                }

                this.creatureOne.getCommunicator().sendNormalServerMessage(this.creatureTwo.getName() + " cannot trade right now.", (byte)3);
                this.end(this.creatureTwo, false);
                return true;
            }
        } else {
            if (this.creatureOne.hasLink()) {
                this.creatureOne.getCommunicator().sendNormalServerMessage("You may not trade right now.", (byte)3);
            }

            this.creatureTwo.getCommunicator().sendNormalServerMessage(this.creatureOne.getName() + " cannot trade right now.", (byte)3);
            this.end(this.creatureOne, false);
            return true;
        }
    }

    @Override
    public void end(Creature creature, boolean closed) {
        if (creature.equals(this.creatureOne)) {
            this.creatureTwo.getCommunicator().sendCloseTradeWindow();
            if (!closed) {
                this.creatureOne.getCommunicator().sendCloseTradeWindow();
            }

            this.creatureTwo.getCommunicator().sendNormalServerMessage(this.creatureOne.getName() + " withdrew from the trade.", (byte)2);
            this.creatureOne.getCommunicator().sendNormalServerMessage("You withdraw from the trade.", (byte)2);
        } else {
            this.creatureOne.getCommunicator().sendCloseTradeWindow();
            if (!closed || !this.creatureTwo.isPlayer()) {
                this.creatureTwo.getCommunicator().sendCloseTradeWindow();
            }

            this.creatureOne.getCommunicator().sendNormalServerMessage(this.creatureTwo.getName() + " withdrew from the trade.", (byte)2);
            this.creatureTwo.getCommunicator().sendNormalServerMessage("You withdraw from the trade.", (byte)2);
        }

        this.creatureTwoOfferWindow.endTrade();
        this.creatureOneOfferWindow.endTrade();
        this.creatureOneRequestWindow.endTrade();
        this.creatureTwoRequestWindow.endTrade();
        this.creatureOne.setTrade(null);
        this.creatureTwo.setTrade(null);
    }

    @Override
    boolean isCreatureOneSatisfied() {
        return this.creatureOneSatisfied;
    }

    @Override
    void setCreatureOneSatisfied(boolean aCreatureOneSatisfied) {
        this.creatureOneSatisfied = aCreatureOneSatisfied;
    }

    @Override
    boolean isCreatureTwoSatisfied() {
        return this.creatureTwoSatisfied;
    }

    @Override
    void setCreatureTwoSatisfied(boolean aCreatureTwoSatisfied) {
        this.creatureTwoSatisfied = aCreatureTwoSatisfied;
    }

    @Override
    public int getCurrentCounter() {
        return this.currentCounter;
    }

    @Override
    void setCurrentCounter(int aCurrentCounter) {
        this.currentCounter = aCurrentCounter;
    }

    @Override
    public long getTax() {
        return this.tax;
    }

    @Override
    public void setTax(long aTax) {
        this.tax = aTax;
    }

    @Override
    public TradingWindow getCreatureOneRequestWindow() {
        return this.creatureOneRequestWindow;
    }

    @Override
    public TradingWindow getCreatureTwoRequestWindow() {
        return this.creatureTwoRequestWindow;
    }

    @Override
    Creature getCreatureOne() {
        return this.creatureOne;
    }

    @Override
    Creature getCreatureTwo() {
        return this.creatureTwo;
    }
}
