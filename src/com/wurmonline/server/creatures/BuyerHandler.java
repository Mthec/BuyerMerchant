//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
// Modified from version by Code Club AB.
//

package com.wurmonline.server.creatures;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.*;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuyerHandler extends TradeHandler implements MiscConstants, ItemTypes, MonetaryConstants {
    private static final Logger logger = Logger.getLogger(BuyerHandler.class.getName());
    private Creature creature;
    private BuyerTrade trade;
    private boolean balanced = false;
    private boolean waiting = false;
    public static int maxPersonalItems = 51;
    private final Shop shop;
    private final boolean ownerTrade;
    private PriceList priceList;
    private Map<PriceList.Entry, HashSet<Item>> minimumRequired = new HashMap<>();

    public BuyerHandler(Creature aCreature, Trade _trade) throws PriceList.NoPriceListOnBuyer {
        this.creature = aCreature;
        this.trade = (BuyerTrade)_trade;
        this.shop = Economy.getEconomy().getShop(aCreature);
        this.ownerTrade = this.shop.getOwnerId() == this.trade.creatureOne.getWurmId();
        if (this.ownerTrade) {
            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(aCreature.getName() + " says, 'Welcome back, " + this.trade.creatureOne.getName() + "!'");
        } else {
            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(aCreature.getName() + " says, 'I will not sell anything, but I can offer money for these things.'");
        }
        if (this.trade.creatureOne.getPower() >= 3) {
            if (BuyerTradingWindow.freeMoney) {
                this.trade.creatureOne.getCommunicator().sendSafeServerMessage(aCreature.getName() + " says, 'I do not require any money.'");
            } else {
                long money = this.shop.getMoney();
                this.trade.creatureOne.getCommunicator().sendSafeServerMessage(aCreature.getName() + " says, 'I have " + (money != 0 ? (new Change(money)).getChangeShortString() : "no money") + ".'");
            }
        }
        priceList = PriceList.getPriceListFromBuyer(this.creature);
    }

    @Override
    void end() {
        this.creature = null;
        this.trade = null;
        priceList.destroyItems();
        priceList = null;
    }

    // What does it even do?  Think it may just be for logging.  Does also initiate tradeChanged().
    @Override
    void addToInventory(Item item, long inventoryWindow) {
        if (this.trade != null) {
            if (inventoryWindow == 2L) {
                this.tradeChanged();
                if (logger.isLoggable(Level.FINEST) && item != null) {
                    logger.finest("Added " + item.getName() + " to his offer window.");
                }
            } else if (inventoryWindow == 1L) {
                if (logger.isLoggable(Level.FINEST) && item != null) {
                    logger.finest("Added " + item.getName() + " to my offer window.");
                }
            } else if (inventoryWindow == 3L) {
                if (logger.isLoggable(Level.FINEST) && item != null) {
                    logger.finest("Added " + item.getName() + " to his request window.");
                }
            } else if (inventoryWindow == 4L && logger.isLoggable(Level.FINEST) && item != null) {
                logger.finest("Added " + item.getName() + " to my request window.");
            }
        }

    }

    @Override
    void tradeChanged() {
        this.balanced = false;
        this.waiting = false;
    }

    @Override
    void addItemsToTrade() {
        if (this.trade != null) {
            TradingWindow myOffers = this.trade.getTradingWindow(1L);
            myOffers.startReceivingItems();
            if (this.ownerTrade) {
                for (Item item : this.creature.getInventory().getItems()) {
                    // Blocking price list, so owner can only get it when the buyer was auto-dismissed or killed.
                    if (!PriceList.isPriceList(item))
                        myOffers.addItem(item);
                }
            } else {
                for (TempItem item : priceList.getItems()) {
                    if (item.getPrice() != PriceList.unauthorised)
                        myOffers.addItem(item);
                }
            }
            myOffers.stopReceivingItems();
        }
    }

    @Override
    public int getTraderSellPriceForItem(Item item, TradingWindow window) {
        // Do nothing.  Should only ever use BuyPrice below.
        return 0;
    }

    @Override
    public int getTraderBuyPriceForItem(Item item) {
        PriceList.Entry entry = priceList.getEntryFor(item);
        if (entry == null)
            return 0;
        return getTraderBuyPriceForItem(entry, item);
    }

    private int getTraderBuyPriceForItem(PriceList.Entry entry, Item item) {
        int markedPrice = 0;

        if (item.getDamage() != 0) {
            return markedPrice;
        }

        markedPrice = entry.getPrice();

        // Could be 0 or unauthorised.
        if (markedPrice < 1)
            return markedPrice;

        float weightRatio = ((float)item.getWeightGrams()) / ((float)item.getTemplate().getWeightGrams());

        return Math.max(0, (int)(markedPrice * weightRatio));
    }

    private long getDiff() {
        if (this.ownerTrade) {
            return 0L;
        } else {
            // whatPlayerWants in TradeHandler is not needed as all coins are removed before working out diff.
            Item[] whatIWant = this.trade.getCreatureTwoRequestWindow().getItems();
            long myDemand = 0L;

            for (Item item : whatIWant) {
                myDemand += getTraderBuyPriceForItem(item);
            }

            return myDemand;
        }
    }

    public static int getMaxNumPersonalItems() {
        return maxPersonalItems;
    }

    private void suckInterestingItems() {
        TradingWindow offeredWindow = this.trade.getTradingWindow(2L);
        TradingWindow targetWindow = this.trade.getTradingWindow(4L);

        // Reset minimumRequired items for easier adding of new items that match the Entry.
        if (!minimumRequired.isEmpty()) {
            Set<Item> items = new HashSet<>(Arrays.asList(targetWindow.getItems()));

            for (Map.Entry<PriceList.Entry, HashSet<Item>> entries : minimumRequired.entrySet()) {
                HashSet<Item> list = entries.getValue();
                for (Item item : list.toArray(new Item[0])) {
                    if (items.contains(item)) {
                        targetWindow.removeItem(item);
                        offeredWindow.addItem(item);
                    } else {
                        list.remove(item);
                    }
                }
            }
        }

        Item[] offeredItems = offeredWindow.getItems();
        Item[] alreadyAcceptedItems = targetWindow.getItems();
        if (this.ownerTrade) {
            // hisReq in TradeHandler is not needed as the buyer can't accept items from owner.
            targetWindow.startReceivingItems();

            for (Item offeredItem : offeredItems) {
                if (offeredItem.isCoin() || PriceList.isPriceList(offeredItem)) {
                    offeredWindow.removeItem(offeredItem);
                    targetWindow.addItem(offeredItem);
                }
            }

            targetWindow.stopReceivingItems();
        } else {
            int size = 0;

            // This is correct for buyer as TradeHandler gets current total from window 1.
            for (Item item : this.creature.getInventory().getItems()) {
                // Removed PriceList check to save doing it on every item.
                // Also with the max_items option it already accounts for it.
                if (!item.isCoin()) {
                    ++size;
                }
            }

            size += alreadyAcceptedItems.length;
            if (size >= maxPersonalItems) {
                this.trade.creatureOne.getCommunicator().sendNormalServerMessage(this.creature.getName() + " says, 'I cannot add more items to my stock right now.'");
            } else {
                boolean anyNotAuthorised = false;
                boolean anyDamaged = false;
                boolean anyLocked = false;
                boolean personalItemsFull = false;
                targetWindow.startReceivingItems();

                for (Item offeredItem : offeredItems) {
                    if (size < maxPersonalItems) {
                        if (offeredItem.getDamage() > 0) {
                            anyDamaged = true;
                        } else if (offeredItem.isLockable() && offeredItem.isLocked()) {
                            anyLocked = true;
                        } else if ((offeredItem.isHollow() && !offeredItem.isEmpty(true)) || offeredItem.isSealedByPlayer()) {
                            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'Please empty the " + offeredItem.getName() + " first.'");
                        }
                        else if (!offeredItem.isCoin()) {
                            PriceList.Entry entry = priceList.getEntryFor(offeredItem);
                            if (entry != null) {
                                int price = getTraderBuyPriceForItem(entry, offeredItem);
                                if (price != PriceList.unauthorised) {
                                    if (entry.getMinimumPurchase() != 1) {
                                        if (!minimumRequired.containsKey(entry))
                                            minimumRequired.put(entry, new HashSet<>());
                                        minimumRequired.get(entry).add(offeredItem);
                                    } else {
                                        if (price == 0) {
                                            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I will not pay you anything, but will accept the " + offeredItem.getName() + " as a donation.'");
                                        }
                                        Item parent = offeredItem;

                                        try {
                                            parent = offeredItem.getParent();
                                        } catch (NoSuchItemException ignored) {
                                        }

                                        if (offeredItem == parent || parent.isViewableBy(this.creature)) {
                                            offeredWindow.removeItem(offeredItem);
                                            targetWindow.addItem(offeredItem);
                                            ++size;
                                        }
                                    }

                                } else
                                    anyNotAuthorised = true;

                            }
                        }
                    } else {
                        personalItemsFull = true;
                        break;
                    }
                }

                if (!minimumRequired.isEmpty()) {
                    Iterator<Map.Entry<PriceList.Entry, HashSet<Item>>> allEntries = minimumRequired.entrySet().iterator();
                    while (allEntries.hasNext()) {
                        Map.Entry<PriceList.Entry, HashSet<Item>> entries = allEntries.next();
                        PriceList.Entry entry = entries.getKey();
                        HashSet<Item> minimumRequiredItems = entries.getValue();

                        if (minimumRequiredItems.isEmpty()) {
                            allEntries.remove();
                            continue;
                        }

                        if (minimumRequiredItems.size() >= entry.getMinimumPurchase()) {
                            if (size + entry.getMinimumPurchase() <= maxPersonalItems) {
                                if (size + minimumRequiredItems.size() > maxPersonalItems)
                                    this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I do not have enough space to accept all of the " + entry.getItem().getTemplate().getPlural() + ".'");

                                int price = getTraderBuyPriceForItem(entry, minimumRequiredItems.iterator().next());
                                if (price == 0) {
                                    this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I will not pay you anything, but will accept the " + minimumRequiredItems.iterator().next().getTemplate().getPlural() + " as a donation.'");
                                }

                                // Don't know why this wouldn't be true, because of container test above.
                                if (minimumRequiredItems.stream().allMatch(item -> {
                                    Item parent = item;
                                    try {
                                        parent = item.getParent();
                                    } catch (NoSuchItemException ignored) {
                                    }

                                    return item == parent || parent.isViewableBy(this.creature);
                                })) {
                                    for (Item offeredItem : minimumRequiredItems) {
                                        if (size < maxPersonalItems) {
                                            offeredWindow.removeItem(offeredItem);
                                            targetWindow.addItem(offeredItem);
                                            ++size;
                                        }
                                    }
                                }
                            } else {
                                personalItemsFull = true;
                                break;
                            }
                        } else {
                            int numberRequired = entry.getMinimumPurchase() - minimumRequiredItems.size();
                            ItemTemplate template = entry.getItem().getTemplate();
                            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I will need " + numberRequired + " more " + (numberRequired == 1 ? template.getName() : template.getPlural()) + " in order to accept them.'");
                        }
                    }
                }

                targetWindow.stopReceivingItems();

                if (anyDamaged)
                    this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I don't accept damaged items.'");
                if (anyLocked)
                    this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I don't accept locked items any more. Sorry for the inconvenience.'");
                if (anyNotAuthorised)
                    this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I am not authorised to buy this item.'");
                if (personalItemsFull)
                    this.trade.creatureOne.getCommunicator().sendNormalServerMessage(this.creature.getName() + " says, 'I cannot add more items to my stock right now.'");
            }
        }
    }

    @Override
    void balance() {
        if (!this.balanced) {
            if (this.ownerTrade) {
                this.suckInterestingItems();
                this.trade.setSatisfied(this.creature, true, this.trade.getCurrentCounter());
                this.balanced = true;
            } else {
                if (this.shop.isPersonal() && !this.waiting) {
                    this.suckInterestingItems();
                    for (Item item : this.trade.getCreatureOneRequestWindow().getItems()) {
                        if (item.isCoin())
                            this.trade.getCreatureOneRequestWindow().removeItem(item);
                    }
                    long diff = this.getDiff();
                    if (diff > 0L) {
                        long withBuyersCut = (long)(diff * 1.1f);
                        if (!BuyerTradingWindow.freeMoney && withBuyersCut > this.shop.getMoney()) {
                            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " 'I am low on cash and can not purchase those items.'");
                            this.waiting = true;
                        } else {
                            // Note - These coins are never traded.
                            Item[] money = Economy.getEconomy().getCoinsFor(diff);
                            this.trade.getCreatureOneRequestWindow().startReceivingItems();

                            for (Item coin : money) {
                                this.trade.getCreatureOneRequestWindow().addItem(coin);
                            }

                            this.trade.getCreatureOneRequestWindow().stopReceivingItems();
                            this.trade.setSatisfied(this.creature, true, this.trade.getCurrentCounter());
                            this.balanced = true;
                        }
                    } else if (diff < 0L) {
                        logger.warning("Buyer has negative trade, this should never happen.");
                        this.balanced = false;
                    } else {
                        this.trade.setSatisfied(this.creature, true, this.trade.getCurrentCounter());
                        this.balanced = true;
                    }
                }
            }
        }
    }
}
