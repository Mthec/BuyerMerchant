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
import javafx.util.Pair;
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
    private Map<PriceList.Entry, MinimumRequired> minimumRequiredMap = new HashMap<>();
    private static int deliveryContractId = -10;
    private static final int unauthorisedItem = 1;
    private static final int notFullWeight = 2;
    private Map<PriceList.Entry, Set<Item>> remainingToPurchaseMap = new HashMap<>();
    private boolean tradeSuccessful;

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
            long money = this.shop.getMoney();
            if (BuyerTradingWindow.freeMoney) {
                this.trade.creatureOne.getCommunicator().sendSafeServerMessage(aCreature.getName() + " says, 'I do not require any money.  I have " + (money != 0 ? (new Change(money)).getChangeShortString() + " in" : "no") + " cash.'");
            } else {
                this.trade.creatureOne.getCommunicator().sendSafeServerMessage(aCreature.getName() + " says, 'I have " + (money != 0 ? (new Change(money)).getChangeShortString() : "no money") + ".'");
            }
        }
        priceList = PriceList.getPriceListFromBuyer(this.creature);
    }

    @Override
    void end() {
        if (tradeSuccessful) {
            for (Map.Entry<PriceList.Entry, Set<Item>> pair : remainingToPurchaseMap.entrySet()) {
                try {
                    PriceList.Entry entry = pair.getKey();
                    int count = pair.getValue().size();
                    //  Relying on validation in the trade, removing negatives as a precaution.
                    if (entry.getRemainingToPurchase() <= count) {
                        priceList.removeItem(entry);
                    } else {
                        entry.subtractRemainingToPurchase(count);
                    }
                    priceList.savePriceList();
                } catch (PriceList.PriceListFullException | PriceList.PageNotAdded e) {
                    logger.warning("Could not update Price List when updating Remaining to Purchase for some reason.");
                    e.printStackTrace();
                }
            }
        }
        minimumRequiredMap.clear();
        remainingToPurchaseMap.clear();

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

        if (item.getDamage() != 0 && !entry.acceptsDamaged()) {
            return markedPrice;
        }

        markedPrice = entry.getPrice();

        // Could be 0 or unauthorised.
        if (markedPrice < 1)
            return markedPrice;

        float weightRatio = ((float)item.getWeightGrams()) / ((float)entry.getWeight());

        return Math.max(0, (int)(markedPrice * weightRatio));
    }

    private int addContractToMinimumRequirement(Item contract) {
        assert contract.getTemplateId() == deliveryContractId;

        Map<PriceList.Entry, Integer> entries = new HashMap<>();

        for (Item item : contract.getItems()) {
            // TODO - More performant way to check?  What about ql differences?
            PriceList.Entry entry = priceList.getEntryFor(item);
            if (entry == null) {
                return unauthorisedItem;
            }
            if (entry.getMinimumPurchase() != 1 && item.getWeightGrams() < entry.getWeight()) {
                return notFullWeight;
            }

            entries.merge(entry, 1, Integer::sum);
        }

        ContractMinimum prices = new ContractMinimum(contract, entries);
        for (PriceList.Entry entry : prices.getLinked()) {
            MinimumRequired minimum = minimumRequiredMap.get(entry);
            if (minimum == null) {
                minimum = new MinimumRequired(entry);
                minimumRequiredMap.put(entry, minimum);
            }

            minimum.addItem(prices);
        }

        return 0;
    }

    public static int getMaxNumPersonalItems() {
        return maxPersonalItems;
    }

    private int suckInterestingItems() {
        TradingWindow offeredWindow = this.trade.getTradingWindow(2L);
        TradingWindow targetWindow = this.trade.getTradingWindow(4L);

        // Reset minimumRequiredMap items for easier adding of new items that match the Entry.
        if (!minimumRequiredMap.isEmpty()) {
            Set<Item> items = new HashSet<>(Arrays.asList(targetWindow.getItems()));

            for (MinimumRequired minimum : minimumRequiredMap.values()) {
                for (Item item : minimum.getItems()) {
                    if (items.contains(item)) {
                        targetWindow.removeItem(item);
                        offeredWindow.addItem(item);
                    }
                }
            }

            minimumRequiredMap.clear();
        }

        if (!remainingToPurchaseMap.isEmpty()) {
            Set<Item> items = new HashSet<>(Arrays.asList(targetWindow.getItems()));

            for (Set<Item> itemSet : remainingToPurchaseMap.values()) {
                for (Item item : itemSet) {
                    if (items.contains(item)) {
                        targetWindow.removeItem(item);
                        offeredWindow.addItem(item);
                    }
                }
            }

            remainingToPurchaseMap.clear();
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

            return 0;
        } else {
            // This is correct for buyer as TradeHandler gets current total from window 1.
            // With the max_items option PriceList is already accounted for.
            int size = creature.getNumberOfShopItems();
            int totalPrice;

            size += alreadyAcceptedItems.length;
            totalPrice = Arrays.stream(alreadyAcceptedItems).mapToInt(this::getTraderBuyPriceForItem).sum();

            if (size >= maxPersonalItems) {
                this.trade.creatureOne.getCommunicator().sendNormalServerMessage(this.creature.getName() + " says, 'I cannot add more items to my stock right now.'");
            } else {
                boolean anyNotAuthorised = false;
                boolean anyDamaged = false;
                boolean anyLocked = false;
                Set<PriceList.Entry> anyOverLimit = new HashSet<>();
                boolean anyMinimumNotFullWeight = false;
                boolean personalItemsFull = false;
                targetWindow.startReceivingItems();

                for (Item offeredItem : offeredItems) {
                    if (size < maxPersonalItems) {
                        if (offeredItem.isLockable() && offeredItem.isLocked()) {
                            anyLocked = true;
                        } else if ((offeredItem.isHollow() && !offeredItem.isEmpty(true)) || offeredItem.isSealedByPlayer()) {
                            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'Please empty the " + offeredItem.getName() + " first.'");
                        } else if (offeredItem.getTemplateId() == deliveryContractId) {
                            int contractResult = addContractToMinimumRequirement(offeredItem);
                            if (contractResult == unauthorisedItem) {
                                anyNotAuthorised = true;
                            } else if (contractResult == notFullWeight) {
                                anyMinimumNotFullWeight = true;
                            }
                        }
                        else if (!offeredItem.isCoin()) {
                            PriceList.Entry entry = priceList.getEntryFor(offeredItem);
                            if (entry != null) {
                                if (offeredItem.getDamage() > 0 && !entry.acceptsDamaged()) {
                                    anyDamaged = true;
                                    continue;
                                }
                                int price = getTraderBuyPriceForItem(entry, offeredItem);
                                if (price != PriceList.unauthorised) {
                                    if (entry.getMinimumPurchase() != 1) {
                                        if (offeredItem.getWeightGrams() == entry.getWeight()) {
                                            MinimumRequired minimum = minimumRequiredMap.get(entry);
                                            if (minimum == null) {
                                                minimum = new MinimumRequired(entry);
                                                minimumRequiredMap.put(entry, minimum);
                                            }

                                            minimum.addItem(offeredItem);
                                        } else {
                                            anyMinimumNotFullWeight = true;
                                        }
                                    } else {
                                        int remaining = entry.getRemainingToPurchase();
                                        if (remaining != 0) {
                                            Set<Item> alreadyAccepted = remainingToPurchaseMap.get(entry);
                                            if (alreadyAccepted != null && alreadyAccepted.size() + 1 > entry.getRemainingToPurchase()) {
                                                anyOverLimit.add(entry);
                                                continue;
                                            } else {
                                                if (alreadyAccepted == null) {
                                                    alreadyAccepted = new HashSet<>();
                                                    remainingToPurchaseMap.put(entry, alreadyAccepted);
                                                }
                                                alreadyAccepted.add(offeredItem);
                                            }
                                        }

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
                                            totalPrice += price;
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

                // Knapsack Problem Plus is hard!
                if (!minimumRequiredMap.isEmpty()) {
                    List<PriceList.Entry> entries = new LinkedList<>(minimumRequiredMap.keySet());
                    List<PriceList.Entry> confirmed = new LinkedList<>();
                    Set<MinimumSet> invalidSets = new HashSet<>();

                    // Filter out items that don't meet the requirement and remove MinimumSets from all counts.
                    while (entries.size() > 0) {
                        PriceList.Entry entry = entries.get(0);
                        MinimumRequired minimumRequired = minimumRequiredMap.get(entry);
                        minimumRequired.removeInvalidSets(invalidSets);

                        int count = minimumRequired.count();

                        if (count >= entry.getMinimumPurchase() && (entry.getRemainingToPurchase() == 0 || count <= entry.getRemainingToPurchase())) {
                            confirmed.add(entry);
                        } else if (count != 0) { // Prevent entries with removed sets from sending messages.
                            Set<PriceList.Entry> toReevaluate = minimumRequired.getLinked();
                            confirmed.removeAll(toReevaluate);
                            entries.addAll(toReevaluate);
                            invalidSets.addAll(minimumRequired.getMinimumSets());

                            int numberRequired = entry.getMinimumPurchase() - count;
                            ItemTemplate template = entry.getItem().getTemplate();
                            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I will need " + numberRequired + " more " + (numberRequired == 1 ? template.getName() : template.getPlural()) + " in order to accept them.'");
                        }

                        entries.remove(entry);
                    }

                    List<PriceList.Entry> accepted = new LinkedList<>();
                    invalidSets = new HashSet<>();
                    int itemCount = size;

                    while (confirmed.size() > 0) {
                        PriceList.Entry entry = confirmed.get(0);
                        MinimumRequired minimumRequired = minimumRequiredMap.get(entry);
                        minimumRequired.removeInvalidSets(invalidSets);

                        if (itemCount + minimumRequired.itemCount() <= maxPersonalItems) {
                            accepted.add(entry);
                            itemCount += minimumRequired.itemCount();
                        } else {
                            // Optimise set attempt
                            // One drawback, if set entry is removed later it won't get all of the items if they would now fit.
                            // Another drawback, can only remove non-sets as it gets way to complicated otherwise.
                            if (minimumRequired.shrinkToFit(maxPersonalItems - itemCount)) {
                                accepted.add(entry);
                                itemCount += minimumRequired.itemCount();
                                personalItemsFull = true;
                            } else {
                                Set<PriceList.Entry> toReevaluate = minimumRequired.getLinked();
                                accepted.removeAll(toReevaluate);
                                confirmed.addAll(toReevaluate);
                                invalidSets.addAll(minimumRequired.getMinimumSets());
                                personalItemsFull = true;
                            }
                        }

                        confirmed.remove(entry);
                    }

                    Set<Item> addedContracts = new HashSet<>();
                    // Finally, move windows.
                    for (PriceList.Entry entry : accepted) {
                        MinimumRequired minimumRequired = minimumRequiredMap.get(entry);
                        if (minimumRequired.isOptimal())
                            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I do not have enough space to accept all of the " + entry.getItem().getTemplate().getPlural() + ".'");

                        int setPrice = minimumRequired.getTotalPrice();
                        if (setPrice == 0) {
                            this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I will not pay you anything, but will accept the " + entry.getItem().getTemplate().getPlural() + " as a donation.'");
                        }

                        for (Pair<Item, Integer> itemPrice : minimumRequired.getItemsAndPrices()) {
                            Item offeredItem = itemPrice.getKey();
                            if (offeredItem.getTemplateId() == deliveryContractId) {
                                if (addedContracts.contains(offeredItem))
                                    continue;
                                else
                                    addedContracts.add(offeredItem);
                            }
                            if (entry.getRemainingToPurchase() != 0) {
                                Set<Item> alreadyAccepted = remainingToPurchaseMap.computeIfAbsent(entry, k -> new HashSet<>());
                                alreadyAccepted.add(offeredItem);
                            }

                            offeredWindow.removeItem(offeredItem);
                            targetWindow.addItem(offeredItem);
                            ++size;
                            totalPrice += itemPrice.getValue();
                        }
                    }
                }

                targetWindow.stopReceivingItems();

                if (anyDamaged)
                    this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I don't accept damaged items of that type.'");
                if (anyLocked)
                    this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I don't accept locked items any more. Sorry for the inconvenience.'");
                if (!anyOverLimit.isEmpty()) {
                    for (PriceList.Entry entry : anyOverLimit) {
                        int remaining = entry.getRemainingToPurchase();
                        this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I can only accept " + remaining + " more " + (remaining == 1 ? entry.getName() : entry.getPluralName()) + ".'");
                    }
                }
                if (anyMinimumNotFullWeight)
                    this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I can only accept full weight items when there is a minimum required amount.'");
                if (anyNotAuthorised)
                    this.trade.creatureOne.getCommunicator().sendSafeServerMessage(this.creature.getName() + " says, 'I am not authorised to buy " + (offeredWindow.getItems().length != 1 ? "these items" : "this item") + ".'");
                if (personalItemsFull)
                    this.trade.creatureOne.getCommunicator().sendNormalServerMessage(this.creature.getName() + " says, 'I cannot add more items to my stock right now.'");
            }

            return totalPrice;
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
                    for (Item item : this.trade.getCreatureOneRequestWindow().getItems()) {
                        if (item.isCoin())
                            this.trade.getCreatureOneRequestWindow().removeItem(item);
                    }
                    long diff = this.suckInterestingItems();
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

    public void setTradeSuccessful() {
        tradeSuccessful = true;
    }
}
