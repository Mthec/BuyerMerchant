//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
// Modified from version by Code Club AB.
//

package com.wurmonline.server.questions;

import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.BuyerHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import mod.wurmunlimited.buyermerchant.BuyerMerchant;
import mod.wurmunlimited.buyermerchant.ItemDetails;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetBuyerPricesQuestion extends BuyerQuestionExtension {
    private static final Logger logger = Logger.getLogger(SetBuyerPricesQuestion.class.getName());
    private final Map<PriceList.Entry, Integer> itemMap = new HashMap<>();
    private PriceList priceList;

    SetBuyerPricesQuestion(Creature aResponder, long aTarget) {
        super(aResponder, "Price management", "Set prices for items", 23, aTarget);
    }

    public void answer(Properties answers) {
        this.setAnswer(answers);
        parseSetBuyerPricesQuestion();
    }

    private void parseSetBuyerPricesQuestion() {
        Creature responder = this.getResponder();
        long target = this.getTarget();
        Properties props = this.getAnswer();

        if (wasSelected("new")) {
            new AddItemToBuyerInstantQuestion(responder, this.target).sendQuestion();
            return;
        }
        if (wasSelected("schedule")) {
            try {
                new UpdateScheduleQuestion(responder, Creatures.getInstance().getCreature(this.target)).sendQuestion();
            } catch (NoSuchCreatureException e) {
                logger.warning("Could not find buyer from wurmId.");
                e.printStackTrace();
                responder.getCommunicator().sendAlertServerMessage("Something went wrong, is the buyer really there?");
            }
            return;
        }
        if (wasSelected("sort")) {
            try {
                priceList.sortAndSave();
            } catch (PriceList.PriceListFullException | PriceList.PageNotAdded e) {
                responder.getCommunicator().sendNormalServerMessage(PriceList.noPriceListFoundPlayerMessage);
                logger.warning("Price List was not sorted correctly.");
                e.printStackTrace();
            }
            new SetBuyerPricesQuestion(getResponder(), getTarget()).sendQuestion();
        }

        try {
            Creature trader = Server.getInstance().getCreature(target);
            if (trader.isNpcTrader()) {
                Shop shop = Economy.getEconomy().getShop(trader);
                if (shop == null) {
                    responder.getCommunicator().sendNormalServerMessage("No shop registered for that creature.");
                } else if (shop.getOwnerId() == responder.getWurmId()) {
                    for (PriceList.Entry item : priceList.asArray()) {
                        int bid = itemMap.get(item);
                        if (wasSelected(bid + "remove"))
                            priceList.removeItem(item);
                        else {
                            try {
                                setItemDetails(item, bid, props, responder);
                            } catch (PriceList.PriceListDuplicateException e) {
                                responder.getCommunicator().sendNormalServerMessage(PriceList.wouldResultInDuplicateMessage);
                            }
                        }
                    }
                    priceList.savePriceList();
                    responder.getCommunicator().sendNormalServerMessage("The prices are updated.");
                } else {
                    responder.getCommunicator().sendNormalServerMessage("You don't own that shop.");
                }
            }
        } catch (NoSuchCreatureException | NoSuchPlayerException e) {
            responder.getCommunicator().sendNormalServerMessage("No such creature.");
            logger.log(Level.WARNING, responder.getName(), e);
        } catch (PriceList.PriceListFullException e) {
            responder.getCommunicator().sendNormalServerMessage(PriceList.noSpaceOnPriceListPlayerMessage + "  Some of the prices may have been updated.");
        } catch (PriceList.PageNotAdded e) {
            responder.getCommunicator().sendNormalServerMessage(PriceList.noPriceListFoundPlayerMessage);
            e.printStackTrace();
        }
    }

    static ItemDetails getItemDetails(Creature responder, Properties answers, int id, int templateId, String name, int currentWeight) {
        boolean badPrice = false;
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplateOrNull(templateId);
        int weight = -1;
        float ql = 1;
        int price = 0;
        int remainingToPurchase = 0;
        int minimumPurchase = 1;
        boolean acceptsDamaged;
        String stringId;
        if (id == -1)
            stringId = "";
        else
            stringId = Integer.toString(id);

        String val = answers.getProperty(stringId + "weight");
        if (val != null && !val.isEmpty()) {
            try {
                weight = WeightString.toInt(val);
                if (weight < 0)
                    throw new NumberFormatException("Weight cannot be negative.");
                if (template == null || weight == template.getWeightGrams())
                    weight = -1;
            } catch (NumberFormatException var21) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set the weight for " + name + ".");
                weight = currentWeight;
            }
        }
        val = answers.getProperty(stringId + "q");
        if (val != null && !val.isEmpty()) {
            try {
                ql = Float.parseFloat(val);
                if (ql > 100 || ql < 0)
                    throw new NumberFormatException("Quality level out of range.");
            } catch (NumberFormatException var21) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set the minimum quality level for " + name + ".");
                ql = 1;
            }
        }

        val = answers.getProperty(stringId + "g");
        if (val != null && !val.isEmpty()) {
            try {
                price = Integer.parseInt(val) * MonetaryConstants.COIN_GOLD;
            } catch (NumberFormatException var21) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set the gold price for " + name + ". Note that a coin value is in whole numbers, no decimals.");
                badPrice = true;
            }
        }

        val = answers.getProperty(stringId + "s");
        if (val != null && !val.isEmpty()) {
            try {
                price += Integer.parseInt(val) * MonetaryConstants.COIN_SILVER;
            } catch (NumberFormatException var20) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set a silver price for " + name + ". Note that a coin value is in whole numbers, no decimals.");
                badPrice = true;
            }
        }

        val = answers.getProperty(stringId + "c");
        if (val != null && !val.isEmpty()) {
            try {
                price += Integer.parseInt(val) * MonetaryConstants.COIN_COPPER;
            } catch (NumberFormatException var19) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set a copper price for " + name + ". Note that a coin value is in whole numbers, no decimals.");
                badPrice = true;
            }
        }

        val = answers.getProperty(stringId + "i");
        if (val != null && !val.isEmpty()) {
            try {
                price += Integer.parseInt(val);
            } catch (NumberFormatException var18) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set an iron price for " + name + ". Note that a coin value is in whole numbers, no decimals.");
                badPrice = true;
            }
        }

        if (price < PriceList.unauthorised) {
            badPrice = true;
            responder.getCommunicator().sendNormalServerMessage("Failed to set a negative price for " + name + ". It will remain on the list but will not be authorised until the price is changed.");
        }
        if (badPrice) {
            price = PriceList.unauthorised;
        }

        val = answers.getProperty(stringId + "r");
        if (val != null && !val.isEmpty()) {
            try {
                remainingToPurchase = Integer.parseInt(val);
                if (remainingToPurchase < 0)
                    throw new NumberFormatException("Remaining amount to purchase cannot be less than 0.");
            } catch (NumberFormatException var21) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set the remaining to purchase amount for " + name + ".");
                remainingToPurchase = 0;
            }
        }

        val = answers.getProperty(stringId + "p");
        if (val != null && !val.isEmpty()) {
            try {
                minimumPurchase = Integer.parseInt(val);
                if (minimumPurchase < 1)
                    throw new NumberFormatException("Minimum purchase cannot be less than 1.");
            } catch (NumberFormatException var21) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set the minimum purchase amount for " + name + ".");
                minimumPurchase = -1;
            }
        }

        if (remainingToPurchase != 0 && remainingToPurchase < minimumPurchase)
            responder.getCommunicator().sendNormalServerMessage("Purchase limit is less than minimum purchase amount.  Players will not be able to sell " + name + ".");

        val = answers.getProperty(stringId + "d");
        acceptsDamaged = val != null && val.equals("true");

        return new ItemDetails(weight, ql, price, remainingToPurchase, minimumPurchase, acceptsDamaged);
    }

    static void setItemDetails(PriceList.Entry item, int id, Properties answers, Creature responder) throws PriceList.PriceListFullException, PriceList.PriceListDuplicateException {
        ItemDetails details = getItemDetails(responder, answers, id, item.getTemplateId(), item.getName(), item.getWeight());

        item.updateItemDetails(details.weight, details.minQL, details.price, details.remainingToPurchase, details.minimumPurchase, details.acceptsDamaged);
    }

    public void sendQuestion() {
        try {
            int idx = 0;
            Creature trader = Server.getInstance().getCreature(this.target);
            if (trader.isNpcTrader()) {
                Shop shop = Economy.getEconomy().getShop(trader);
                if (shop == null) {
                    this.getResponder().getCommunicator().sendNormalServerMessage("No shop registered for that creature.");
                } else if (shop.getOwnerId() == this.getResponder().getWurmId()) {
                    priceList = PriceList.getPriceListFromBuyer(trader);

                    StringBuilder buf = new StringBuilder(this.getBmlHeaderWithScrollAndQuestion());
                    DecimalFormat df = new DecimalFormat("#.##");
                    if (!BuyerMerchant.isDestroyBoughtItems(trader)) {
                        buf.append("text{text=\"").append(trader.getName()).append(" has inventory space for ").append(BuyerHandler.getMaxNumPersonalItems() - trader.getNumberOfShopItems()).append(" more items.\"}");
                    }
                    buf.append("text{type=\"bold\";text=\"Prices for ").append(trader.getName()).append("\"}text{text=''}");
                    buf.append("text{text=\"Limit restricts the Buyer from purchasing more than that number of items.  Entry will be removed once it reaches 0.  Set to 0 to accept any amount.\"}");
                    buf.append("text{text=\"Minimum Purchase restricts the Buyer from purchasing less than that number of items in a single trade.\"}");
                    buf.append("table{rows=\"").append(priceList.size() + 1).append("\"; cols=\"11\";label{text=\"Item name\"};label{text=\"Weight\"};label{text=\"Min. QL\"};label{text=\"Gold\"};label{text=\"Silver\"};label{text=\"Copper\"};label{text=\"Iron\"}label{text=\"Limit\"};label{text=\"Min. Purchase\"};label{text=\"Accept Damaged\"};label{text=\"Remove?\"}");

                    for(PriceList.Entry item : priceList) {
                        ++idx;
                        Change change = Economy.getEconomy().getChangeFor(item.getPrice());
                        buf.append(itemNameWithColorByRarity(item.getItem()).replaceFirst(" - minimum [\\d]+", ""));
                        buf.append("harray{input{maxchars=\"8\"; id=\"").append(idx).append("weight\";text=\"").append(WeightString.toString(item.getWeight())).append("\"};label{text=\"kg \"}};");
                        buf.append("harray{input{maxchars=\"3\"; id=\"").append(idx).append("q\";text=\"").append(df.format(item.getQualityLevel())).append("\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"3\"; id=\"").append(idx).append("g\";text=\"").append(change.getGoldCoins()).append("\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"2\"; id=\"").append(idx).append("s\";text=\"").append(change.getSilverCoins()).append("\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"2\"; id=\"").append(idx).append("c\";text=\"").append(change.getCopperCoins()).append("\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"2\"; id=\"").append(idx).append("i\";text=\"").append(change.getIronCoins()).append("\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"4\"; id=\"").append(idx).append("r\";text=\"").append(item.getRemainingToPurchase()).append("\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"3\"; id=\"").append(idx).append("p\";text=\"").append(item.getMinimumPurchase()).append("\"};label{text=\" \"}};");
                        buf.append("harray{checkbox{id=\"").append(idx).append("d\"").append(item.acceptsDamaged() ? ";selected=\"true\"" : "").append("};label{text=\" \"}};");
                        buf.append("harray{checkbox{id=\"").append(idx).append("remove\"};label{text=\" \"}};");
                        this.itemMap.put(item, idx);
                    }

                    buf.append("}");
                    buf.append("text{text=\"\"}");
                    buf.append("harray {button{text='Save Prices';id='submit'};label{text=\" \";id=\"spacedlxg\"};button{text='Add New';id='new'};label{text=\" \";id=\"spacedlxg\"};button{text='Sort';id='sort'};label{text=\" \";id=\"spacedlxg\"};button{text='Schedule';id='schedule'}}}}null;null;}");
                    this.getResponder().getCommunicator().sendBml(650, 300, true, true, buf.toString(), 200, 200, 200, this.title);
                } else {
                    this.getResponder().getCommunicator().sendNormalServerMessage("You don't own that shop.");
                }
            }
        } catch (NoSuchCreatureException | NoSuchPlayerException e1) {
            this.getResponder().getCommunicator().sendNormalServerMessage("No such creature.");
            logger.log(Level.WARNING, this.getResponder().getName(), e1);
        } catch (PriceList.NoPriceListOnBuyer e2){
            this.getResponder().getCommunicator().sendNormalServerMessage(PriceList.noPriceListFoundPlayerMessage);
            logger.log(Level.WARNING, this.getResponder().getName(), e2);
        }
    }
}
