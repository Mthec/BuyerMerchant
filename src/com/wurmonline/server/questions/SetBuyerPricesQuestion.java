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
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.BuyerTradingWindow;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetBuyerPricesQuestion extends QuestionExtension {
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
            new AddItemToBuyerQuestion(this.getResponder(), this.target).sendQuestion();
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
                        else
                            setItemDetails(item, bid, props, responder);
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

    static void setItemDetails(PriceList.Entry item, int id, Properties answers, Creature responder) throws PriceList.PriceListFullException {
        int price = 0;
        boolean badPrice = false;
        float ql = -1;
        int minimumPurchase = -1;
        String stringId;
        if (id == -1)
            stringId = "";
        else
            stringId = Integer.toString(id);

        String val = answers.getProperty(stringId + "q");
        if (val != null && val.length() > 0) {
            try {
                ql = Float.parseFloat(val);
                if (ql > 100 || ql < 0)
                    throw new NumberFormatException("Quality level out of range");
            } catch (NumberFormatException var21) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set the minimum quality level for " + item.getName() + ".");
                ql = -1;
            }
        }

        val = answers.getProperty(stringId + "g");
        if (val != null && val.length() > 0) {
            try {
                price = Integer.parseInt(val) * MonetaryConstants.COIN_GOLD;
            } catch (NumberFormatException var21) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set the gold price for " + item.getName() + ". Note that a coin value is in whole numbers, no decimals.");
                badPrice = true;
            }
        }

        val = answers.getProperty(stringId + "s");
        if (val != null && val.length() > 0) {
            try {
                price += Integer.parseInt(val) * MonetaryConstants.COIN_SILVER;
            } catch (NumberFormatException var20) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set a silver price for " + item.getName() + ". Note that a coin value is in whole numbers, no decimals.");
                badPrice = true;
            }
        }

        val = answers.getProperty(stringId + "c");
        if (val != null && val.length() > 0) {
            try {
                price += Integer.parseInt(val) * MonetaryConstants.COIN_COPPER;
            } catch (NumberFormatException var19) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set a copper price for " + item.getName() + ". Note that a coin value is in whole numbers, no decimals.");
                badPrice = true;
            }
        }

        val = answers.getProperty(stringId + "i");
        if (val != null && val.length() > 0) {
            try {
                price += Integer.parseInt(val);
            } catch (NumberFormatException var18) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set an iron price for " + item.getName() + ". Note that a coin value is in whole numbers, no decimals.");
                badPrice = true;
            }
        }

        if (price < PriceList.unauthorised) {
            badPrice = true;
            responder.getCommunicator().sendNormalServerMessage("Failed to set a negative price for " + item.getName() + ". It will remain on the list but will not be authorised until the price is changed.");
        }
        if (badPrice) {
            price = PriceList.unauthorised;
        }

        val = answers.getProperty(stringId + "p");
        if (val != null && val.length() > 0) {
            try {
                minimumPurchase = Integer.parseInt(val);
                if (minimumPurchase < 1)
                    throw new NumberFormatException("Minimum purchase cannot be less than 1.");
            } catch (NumberFormatException var21) {
                responder.getCommunicator().sendNormalServerMessage("Failed to set the minimum purchase amount for " + item.getName() + ".");
                minimumPurchase = -1;
            }
        }
        item.updateItemDetails(ql, price, minimumPurchase);
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

                    StringBuilder buf = new StringBuilder(this.getBmlHeader());
                    DecimalFormat df = new DecimalFormat("#.##");
                    if (!BuyerTradingWindow.destroyBoughtItems)
                        buf.append("text{text=\"" + trader.getName() + " has inventory space for " + (BuyerHandler.getMaxNumPersonalItems() - trader.getNumberOfShopItems()) + " more items.\"}");
                    buf.append("text{type=\"bold\";text=\"Prices for " + trader.getName() + "\"}text{text=''}");
                    buf.append("table{rows=\"" + (priceList.size() + 1) + "\"; cols=\"9\";label{text=\"Item name\"};label{text=\"Weight\"};label{text=\"Min. QL\"};label{text=\"Gold\"};label{text=\"Silver\"};label{text=\"Copper\"};label{text=\"Iron\"}label{text=\"Min. Amount\"};label{text=\"Remove?\"}");

                    for(PriceList.Entry item : priceList) {
                        ++idx;
                        Change change = Economy.getEconomy().getChangeFor((long)item.getPrice());
                        buf.append(itemNameWithColorByRarity(item.getItem()).replaceFirst(" - minimum [\\d]+", ""));
                        buf.append("harray{label{text=\"" + df.format(item.getItem().getWeightGrams() / 1000.0f) + "kg\"}};");
                        buf.append("harray{input{maxchars=\"3\"; id=\"" + idx + "q\";text=\"" + df.format((double)item.getQualityLevel()) + "\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"3\"; id=\"" + idx + "g\";text=\"" + change.getGoldCoins() + "\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"2\"; id=\"" + idx + "s\";text=\"" + change.getSilverCoins() + "\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"2\"; id=\"" + idx + "c\";text=\"" + change.getCopperCoins() + "\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"2\"; id=\"" + idx + "i\";text=\"" + change.getIronCoins() + "\"};label{text=\" \"}};");
                        buf.append("harray{input{maxchars=\"3\"; id=\"" + idx + "p\";text=\"" + item.getMinimumPurchase() + "\"};label{text=\" \"}};");
                        buf.append("harray{checkbox{id=\"" + idx + "remove\"};label{text=\" \"}};");
                        this.itemMap.put(item, idx);
                    }

                    buf.append("}");
                    buf.append("text{text=\"\"}");
                    buf.append("harray {button{text='Save Prices';id='submit'};label{text=\" \";id=\"spacedlxg\"};button{text='Add New';id='new'}label{text=\" \";id=\"spacedlxg\"};button{text='Sort';id='sort'}}}}null;null;}");
                    this.getResponder().getCommunicator().sendBml(525, 300, true, true, buf.toString(), 200, 200, 200, this.title);
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
