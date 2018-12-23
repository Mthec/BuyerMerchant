//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
// Modified from version by Code Club AB.
//

package com.wurmonline.server.items;

import com.wurmonline.server.*;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.players.Player;
import com.wurmonline.shared.util.MaterialUtilities;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.io.IOException;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

public class BuyerTradingWindow extends TradingWindow {
    private final Creature windowowner;
    private final Creature watcher;
    private final boolean offer;
    private final long windowId;
    private Set<Item> items;
    private final Trade trade;
    private static final Logger logger = Logger.getLogger(BuyerTradingWindow.class.getName());
    private static final Map<String, Logger> loggers = new HashMap();
    public static boolean freeMoney = false;
    public static boolean destroyBoughtItems = false;

    BuyerTradingWindow(Creature aOwner, Creature aWatcher, boolean aOffer, long aWurmId, Trade aTrade) {
        super(aOwner, aWatcher, aOffer, aWurmId, aTrade);
        this.windowowner = aOwner;
        this.watcher = aWatcher;
        this.offer = aOffer;
        this.windowId = aWurmId;
        this.trade = aTrade;
    }

    public static void stopLoggers() {
        Iterator var0 = loggers.values().iterator();

        while (true) {
            Logger logger;
            do {
                if (!var0.hasNext()) {
                    return;
                }

                logger = (Logger) var0.next();
            } while (logger == null);

            Handler[] var2 = logger.getHandlers();
            int var3 = var2.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                Handler h = var2[var4];
                h.close();
            }
        }
    }

    private static Logger getLogger(long wurmid) {
        String name = "trader" + wurmid;
        Logger personalLogger = (Logger) loggers.get(name);
        if (personalLogger == null) {
            personalLogger = Logger.getLogger(name);
            personalLogger.setUseParentHandlers(false);
            Handler[] h = logger.getHandlers();

            for (int i = 0; i != h.length; ++i) {
                personalLogger.removeHandler(h[i]);
            }

            try {
                FileHandler fh = new FileHandler(name + ".log", 0, 1, true);
                fh.setFormatter(new SimpleFormatter());
                personalLogger.addHandler(fh);
            } catch (IOException var6) {
                Logger.getLogger(name).log(Level.WARNING, name + ":no redirection possible!");
            }

            loggers.put(name, personalLogger);
        }

        return personalLogger;
    }

    @Override
    public boolean mayMoveItemToWindow(Item item, Creature creature, long window) {
        if (window == 3L && this.windowId == 1L) {
            Shop shop = this.windowowner.getShop();
            if (shop != null && shop.getOwnerId() == creature.getWurmId())
                return true;
        }
        return false;
    }

    @Override
    public boolean mayAddFromInventory(Creature creature, Item item) {
        if (!item.isTraded()) {
            if (item.isNoTrade()) {
                creature.getCommunicator().sendSafeServerMessage(item.getNameWithGenus() + " is not tradable.");
            } else if (this.windowowner.equals(creature)) {
                try {
                    long owneri = item.getOwner();
                    if (owneri != this.watcher.getWurmId() && owneri != this.windowowner.getWurmId()) {
                        this.windowowner.setCheated("Traded " + item.getName() + "[" + item.getWurmId() + "] with " + this.watcher.getName() + " owner=" + owneri);
                    }
                } catch (NotOwnedException var8) {
                    this.windowowner.setCheated("Traded " + item.getName() + "[" + item.getWurmId() + "] with " + this.watcher.getName() + " not owned?");
                }

                if (this.windowId == 2L || this.windowId == 1L) {
                    if (item.isHollow()) {
                        for (Item contents : item.getAllItems(true)) {
                            if (contents.isNoTrade() || contents.isVillageDeed() || contents.isHomesteadDeed() || contents.getTemplateId() == 781) {
                                creature.getCommunicator().sendSafeServerMessage(contents.getNameWithGenus() + " contains a non-tradable item.");
                                return false;
                            }
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public long getWurmId() {
        return this.windowId;
    }

    @Override
    public Item[] getItems() {
        return this.items != null ? (Item[]) this.items.toArray(new Item[this.items.size()]) : new Item[0];
    }

    private void removeExistingContainedItems(Item item) {
        if (item.isHollow()) {
            Item[] itemarr = item.getItemsAsArray();
            Item[] var3 = itemarr;
            int var4 = itemarr.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                Item lElement = var3[var5];
                this.removeExistingContainedItems(lElement);
                if (lElement.getTradeWindow() == this) {
                    this.removeFromTrade(lElement, false);
                } else if (lElement.getTradeWindow() != null) {
                    lElement.getTradeWindow().removeItem(lElement);
                }
            }
        }
    }

    @Override
    public Item[] getAllItems() {
        if (this.items == null) {
            return new Item[0];
        } else {
            Set<Item> toRet = new HashSet();
            Iterator var2 = this.items.iterator();

            while (var2.hasNext()) {
                Item item = (Item) var2.next();
                toRet.add(item);
                Item[] toAdd = item.getAllItems(false);
                Item[] var5 = toAdd;
                int var6 = toAdd.length;

                for (int var7 = 0; var7 < var6; ++var7) {
                    Item lElement = var5[var7];
                    if (lElement.tradeWindow == this) {
                        toRet.add(lElement);
                    }
                }
            }

            return (Item[]) toRet.toArray(new Item[toRet.size()]);
        }
    }

    @Override
    public void stopReceivingItems() {
    }

    @Override
    public void startReceivingItems() {
    }

    // Personal Note - Add items to window.  e.g. initial buyer buying list dump or player adding from own inventory.
    // Idea for actual code would be creating both BuyerTradingWindow and TradingWindow in BuyerTrade,
    // the latter for players so they don't need to ever reach this code.
    @Override
    public void addItem(Item item) {
        if (this.items == null) {
            this.items = new HashSet();
        }

        if (item.tradeWindow == null) {
            this.removeExistingContainedItems(item);
            Item parent = item;

            try {
                parent = item.getParent();
            } catch (NoSuchItemException var4) {
                ;
            }

            this.items.add(item);
            this.addToTrade(item, parent);
            // Note - isViewableBy refers to locked containers.
            if (item == parent || parent.isViewableBy(this.windowowner)) {
                if (!this.windowowner.isPlayer()) {
                    this.windowowner.getCommunicator().sendAddToInventory(item, this.windowId, parent.tradeWindow == this ? parent.getWurmId() : 0L, 0);
                } else if (!this.watcher.isPlayer()) {
                    this.windowowner.getCommunicator().sendAddToInventory(item, this.windowId, parent.tradeWindow == this ? parent.getWurmId() : 0L, this.watcher.getTradeHandler().getTraderBuyPriceForItem(item));
                }
            }

            if (item == parent || parent.isViewableBy(this.watcher)) {
                if (!this.watcher.isPlayer()) {
                    this.watcher.getCommunicator().sendAddToInventory(item, this.windowId, parent.tradeWindow == this ? parent.getWurmId() : 0L, 0);
                } else if (!this.windowowner.isPlayer()) {
                    this.watcher.getCommunicator().sendAddToInventory(item, this.windowId, parent.tradeWindow == this ? parent.getWurmId() : 0L, this.windowowner.getTradeHandler().getTraderBuyPriceForItem(item));
                }
            }
        }

        this.tradeChanged();
    }

    private void addToTrade(Item item, Item parent) {
        if (item.tradeWindow != this) {
            item.setTradeWindow(this);
        }

        for (Item contents : item.getItemsAsArray()) {
            this.addToTrade(contents, item);
        }
    }

    private void removeFromTrade(Item item, boolean noSwap) {
        this.windowowner.getCommunicator().sendRemoveFromInventory(item, this.windowId);
        this.watcher.getCommunicator().sendRemoveFromInventory(item, this.windowId);
        if (noSwap && item.isCoin()) {
            if (item.getOwnerId() == -10L) {
                Economy.getEconomy().returnCoin(item, "Notrade", true);
            }

            item.setTradeWindow(null);
        } else {
            item.setTradeWindow(null);
        }

    }

    @Override
    public void removeItem(Item item) {
        if (this.items != null && item.tradeWindow == this) {
            this.removeExistingContainedItems(item);
            this.items.remove(item);
            this.removeFromTrade(item, true);
            this.tradeChanged();
        }

    }

    @Override
    public void updateItem(Item item) {
        if (this.items != null && item.tradeWindow == this) {
            if (!this.windowowner.isPlayer()) {
                this.windowowner.getCommunicator().sendUpdateInventoryItem(item, this.windowId, 0);
            } else if (!this.watcher.isPlayer()) {
                this.windowowner.getCommunicator().sendUpdateInventoryItem(item, this.windowId, this.watcher.getTradeHandler().getTraderBuyPriceForItem(item));
            }

            if (!this.watcher.isPlayer()) {
                this.watcher.getCommunicator().sendUpdateInventoryItem(item, this.windowId, 0);
            } else if (!this.windowowner.isPlayer()) {
                this.watcher.getCommunicator().sendUpdateInventoryItem(item, this.windowId, this.windowowner.getTradeHandler().getTraderBuyPriceForItem(item));
            }

            this.tradeChanged();
        }

    }

    private void tradeChanged() {
        if (this.windowId == 2L && !this.trade.creatureTwo.isPlayer()) {
            this.trade.setCreatureTwoSatisfied(false);
        }

        if (this.windowId == 3L || this.windowId == 4L) {
            this.trade.setCreatureOneSatisfied(false);
            this.trade.setCreatureTwoSatisfied(false);
            int c = this.trade.getNextTradeId();
            this.windowowner.getCommunicator().sendTradeChanged(c);
            this.watcher.getCommunicator().sendTradeChanged(c);
        }

    }

    @Override
    boolean hasInventorySpace() {
        if (this.offer) {
            this.windowowner.getCommunicator().sendAlertServerMessage("There is a bug in the trade system. This shouldn't happen. Please report.");
            this.watcher.getCommunicator().sendAlertServerMessage("There is a bug in the trade system. This shouldn't happen. Please report.");
            logger.log(Level.WARNING, "Inconsistency! This is offer window number " + this.windowId + ". Traders are " + this.watcher.getName() + ", " + this.windowowner.getName());
            return false;
        } else if (!(this.watcher instanceof Player)) {
            return true;
        } else {
            Item inventory = this.watcher.getInventory();
            if (inventory == null) {
                this.windowowner.getCommunicator().sendAlertServerMessage("Could not find inventory for " + this.watcher.getName() + ". Trade aborted.");
                this.watcher.getCommunicator().sendAlertServerMessage("Could not find your inventory item. Trade aborted. Please contact administrators.");
                logger.log(Level.WARNING, "Failed to locate inventory for " + this.watcher.getName());
                return false;
            } else {
                if (this.items != null) {
                    int nums = 0;
                    Iterator var3 = this.items.iterator();

                    while (var3.hasNext()) {
                        Item item = (Item) var3.next();
                        if (!inventory.testInsertItem(item)) {
                            return false;
                        }

                        if (!item.isCoin()) {
                            ++nums;
                        }

                        if (!item.canBeDropped(false) && ((Player) this.watcher).isGuest()) {
                            this.windowowner.getCommunicator().sendAlertServerMessage("Guests cannot receive the item " + item.getName() + ".");
                            this.watcher.getCommunicator().sendAlertServerMessage("Guests cannot receive the item " + item.getName() + ".");
                            return false;
                        }
                    }

                    // TODO - Anyway to get traded items to remove from nums?
                    if (this.watcher.getPower() <= 0 && nums + inventory.getNumItemsNotCoins() > 99) {
                        this.watcher.getCommunicator().sendAlertServerMessage("You may not carry that many items in your inventory.");
                        this.windowowner.getCommunicator().sendAlertServerMessage(this.watcher.getName() + " may not carry that many items in " + this.watcher.getHisHerItsString() + " inventory.");
                        return false;
                    }
                }

                return true;
            }
        }
    }

    @Override
    int getWeight() {
        int toReturn = 0;
        Item item;
        if (this.items != null) {
            for (Iterator var2 = this.items.iterator(); var2.hasNext(); toReturn += item.getFullWeight()) {
                item = (Item) var2.next();
            }
        }

        return toReturn;
    }

    @Override
    boolean validateTrade() {
        if (this.windowowner.isDead()) {
            return false;
        } else if (this.windowowner instanceof Player && !this.windowowner.hasLink()) {
            return false;
        } else {
            if (this.items != null) {
                Iterator var1 = this.items.iterator();

                while (var1.hasNext()) {
                    Item tit = (Item) var1.next();
                    if ((this.windowowner instanceof Player || !tit.isCoin()) && tit.getOwnerId() != this.windowowner.getWurmId()) {
                        this.windowowner.getCommunicator().sendAlertServerMessage(tit.getName() + " is not owned by you. Trade aborted.");
                        this.watcher.getCommunicator().sendAlertServerMessage(tit.getName() + " is not owned by " + this.windowowner.getName() + ". Trade aborted.");
                        return false;
                    }

                    Item[] allItems = tit.getAllItems(false);
                    Item[] var4 = allItems;
                    int var5 = allItems.length;

                    for (int var6 = 0; var6 < var5; ++var6) {
                        Item lAllItem = var4[var6];
                        if ((this.windowowner instanceof Player || !lAllItem.isCoin()) && lAllItem.getOwnerId() != this.windowowner.getWurmId()) {
                            this.windowowner.getCommunicator().sendAlertServerMessage(lAllItem.getName() + " is not owned by you. Trade aborted.");
                            this.watcher.getCommunicator().sendAlertServerMessage(lAllItem.getName() + " is not owned by " + this.windowowner.getName() + ". Trade aborted.");
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }

    @Override
    void swapOwners() {
        boolean errors = false;
        if (!this.offer) {
            Item inventory = this.watcher.getInventory();
            Item ownInventory = this.windowowner.getInventory();
            Shop shop = null;
            int moneyAdded = 0;
            int moneyLost = 0;
            if (this.windowowner.isNpcTrader()) {
                shop = Economy.getEconomy().getShop(this.windowowner);
            } else if (this.watcher.isNpcTrader()) {
                shop = Economy.getEconomy().getShop(this.watcher);
            }

            if (this.items != null) {
                for (Item lIt : (Item[]) this.items.toArray(new Item[this.items.size()])) {
                    Item item = lIt;
                    this.removeExistingContainedItems(lIt);
                    this.removeFromTrade(lIt, false);
                    boolean coin = lIt.isCoin();
                    long parentId = lIt.getParentId();
                    boolean ok = true;
                    if (!(this.windowowner instanceof Player)) {
                        if (this.watcher.isLogged()) {
                            this.watcher.getLogger().log(Level.INFO, this.windowowner.getName() + " buying " + lIt.getName() + " with id " + lIt.getWurmId() + " from " + this.watcher.getName());
                        }
                    }
                    if (ok) {
                        try {
                            Item parent = Items.getItem(parentId);
                            parent.dropItem(lIt.getWurmId(), false);
                        } catch (NoSuchItemException var36) {
                            if (!coin) {
                                logger.log(Level.WARNING, "Parent not found for item " + lIt.getWurmId());
                            }
                        }

                        // This is the Player's Window aka 4
                        if (!(this.watcher instanceof Player)) {
                            if (coin) {
                                if (shop != null) {
                                    if (shop.isPersonal()) {
                                        getLogger(shop.getWurmId()).log(Level.INFO, this.watcher.getName() + " received " + MaterialUtilities.getMaterialString(lIt.getMaterial()) + " " + lIt.getName() + ", id: " + lIt.getWurmId() + ", QL: " + lIt.getQualityLevel());
                                        if (this.windowowner.getWurmId() == shop.getOwnerId()) {
                                            inventory.insertItem(lIt);
                                            moneyAdded += Economy.getValueFor(lIt.getTemplateId());
                                        } else {
                                            logger.warning(this.windowowner.getName() + " tried to give money to buyer.  Value - " + Economy.getValueFor(lIt.getTemplateId()));
                                        }
                                    }
                                } else {
                                    logger.log(Level.WARNING, this.windowowner.getName() + ", id=" + this.windowowner.getWurmId() + " failed to locate TraderMoney.");
                                }
                            } else if (PriceList.isPriceList(lIt)) {
                                // Override price list with new one provided by owner.
                                for (Item priceList : this.watcher.getInventory().getItems()) {
                                    if (PriceList.isPriceList(priceList)) {
                                        Items.destroyItem(priceList.getWurmId());
                                        break;
                                    }
                                }
                                inventory.insertItem(lIt);

                                if (shop != null) {
                                    getLogger(shop.getWurmId()).log(Level.INFO, this.watcher.getName() + " updated Price List - " + lIt.getName() + ", id: " + lIt.getWurmId());
                                }
                            } else if (destroyBoughtItems) {
                                Items.destroyItem(lIt.getWurmId(), true);
                            } else {
                                inventory.insertItem(lIt);

                                if (shop != null) {
                                    getLogger(shop.getWurmId()).log(Level.INFO, this.watcher.getName() + " received " + MaterialUtilities.getMaterialString(lIt.getMaterial()) + " " + lIt.getName() + ", id: " + lIt.getWurmId() + ", QL: " + lIt.getQualityLevel());
                                }
                            }
                        // This is for the Buyer's Window aka 3
                        } else {
                            inventory.insertItem(lIt);
                            if (coin) {
                                moneyLost += Economy.getValueFor(lIt.getTemplateId());
                            }
                            getLogger(shop.getWurmId()).log(Level.INFO, this.watcher.getName() + " received " + MaterialUtilities.getMaterialString(lIt.getMaterial()) + " " + lIt.getName() + ", id: " + lIt.getWurmId() + ", QL: " + lIt.getQualityLevel());
                        }
                    }

                    if (!ok) {
                        errors = true;
                    }
                }
            }

            if (!errors) {
                this.windowowner.getCommunicator().sendNormalServerMessage("The trade was completed successfully.");
            } else {
                this.windowowner.getCommunicator().sendNormalServerMessage("The trade was completed, not all items were traded.");
            }

            if (shop != null && !freeMoney) {
                int diff = moneyAdded - moneyLost;
                if (this.windowowner.isNpcTrader()) {
                    if (this.watcher.getWurmId() == shop.getOwnerId()) {
                        if (diff != 0) {
                            logger.info(this.watcher.getName() + " - Paying out " + String.valueOf(Math.abs(diff)) + " to owner.  Current shop value - " + String.valueOf(shop.getMoney()));
                            shop.setMoney(shop.getMoney() + (long)diff);
                            logger.info(this.watcher.getName() + " - My shop is now at " + shop.getMoney());
                        }
                    } else {
                        long totalPrice = (long) (moneyLost * 1.1F);
                        long kadd = totalPrice - moneyLost;
                        if (totalPrice != 0) {
                            // Make change and remove coins.
                            Economy economy = Economy.getEconomy();
                            List<Item> coins = new ArrayList<>();
                            for (Item item : ownInventory.getItems()) {
                                if (item.isCoin())
                                    coins.add(item);
                            }
                            coins.sort((i1, i2) -> Integer.compare(economy.getValueFor(i1.getTemplateId()), economy.getValueFor(i2.getTemplateId())));
                            logger.finer("All coins " + coins.stream().map(c -> Integer.toString(economy.getValueFor(c.getTemplateId()))).collect(Collectors.joining(",")));

                            long moneyRequired = totalPrice;
                            try {
                                while (moneyRequired > 0 && coins.size() > 0) {
                                    Item coin = coins.remove(0);
                                    moneyRequired -= economy.getValueFor(coin.getTemplateId());
                                    ownInventory.removeItem(coin);
                                    economy.returnCoin(coin, "BuyerShop");
                                }
                            } catch (IndexOutOfBoundsException e) {
                                logger.warning("Buyer shop total and inventory coins have become desynced.");
                                e.printStackTrace();
                            }

                            logger.finer("Coins left - " + coins.stream().map(c -> Integer.toString(economy.getValueFor(c.getTemplateId()))).collect(Collectors.joining(",")));

                            if (moneyRequired < 0) {
                                logger.finer("Reinserting " + Math.abs(moneyRequired) + " coins.");
                                Item[] change = economy.getCoinsFor(Math.abs(moneyRequired));
                                for (Item item : change)
                                    ownInventory.insertItem(item);
                            }


                            if (kadd != 0L) {
                                Shop kingsMoney = economy.getKingsShop();
                                kingsMoney.setMoney(kingsMoney.getMoney() + kadd);
                                logger.info(this.windowowner.getName() + " - Paying King - " + kadd);
                            }

                            shop.addMoneySpent(totalPrice);

                            logger.info(this.windowowner.getName() + " - Paying out " + String.valueOf(moneyLost) + " to " + this.watcher.getName() + ".  Current shop value - " + String.valueOf(shop.getMoney()));
                            shop.setMoney(shop.getMoney() - totalPrice);
                            logger.info(this.windowowner.getName() + " - My shop is now at " + shop.getMoney());

                            shop.setLastPolled(System.currentTimeMillis());
                        }
                    }
                } else if (this.windowowner.getWurmId() == shop.getOwnerId()) {
                    if (diff != 0) {
                        logger.info(this.windowowner.getName() + " - My owner just gave me " + String.valueOf(diff));
                        shop.setMoney(shop.getMoney() + (long)diff);
                        logger.info(this.windowowner.getName() + " - My shop is now at " + shop.getMoney());
                    }
                }
            }
        } else {
            this.windowowner.getCommunicator().sendAlertServerMessage("There is a bug in the trade system. This shouldn't happen. Please report to the Buyer mod thread.");
            this.watcher.getCommunicator().sendAlertServerMessage("There is a bug in the trade system. This shouldn't happen. Please report to the Buyer mod thread.");
            logger.log(Level.WARNING, "Inconsistency! This is offer window number " + this.windowId + ". Traders are " + this.watcher.getName() + ", " + this.windowowner.getName());
        }

    }

    @Override
    void endTrade() {
        if (this.items != null) {
            Item[] its = (Item[]) this.items.toArray(new Item[this.items.size()]);
            Item[] var2 = its;
            int var3 = its.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                Item lIt = var2[var4];
                this.removeExistingContainedItems(lIt);
                this.items.remove(lIt);
                this.removeFromTrade(lIt, true);
            }
        }

        this.items = null;
    }
}
