package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import mod.wurmunlimited.buyermerchant.db.BuyerScheduler;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class UpdateScheduleQuestion extends BuyerQuestionExtension {
    private final Creature buyer;
    private final Map<BuyerScheduler.Update, Integer> updatesMap = new HashMap<>();
    private final BuyerScheduler.Update[] updates;
    
    UpdateScheduleQuestion(Creature aResponder, Creature buyer) {
        super(aResponder, "Update Schedule", "", QuestionTypes.PRICEMANAGE, buyer.getWurmId());
        this.buyer = buyer;
        this.updates = BuyerScheduler.getUpdatesFor(buyer);
    }

    @Override
    public void answer(Properties properties) {
        Creature responder = getResponder();

        if (wasSelected("submit")) {
            if (buyer.isNpcTrader()) {
                Shop shop = Economy.getEconomy().getShop(buyer);
                if (shop == null) {
                    responder.getCommunicator().sendNormalServerMessage("No shop registered for that creature.");
                } else if (shop.getOwnerId() == responder.getWurmId()) {
                    for (BuyerScheduler.Update update : updates) {
                        int bid = updatesMap.get(update);
                        if (wasSelected(bid + "remove")) {
                            BuyerScheduler.deleteUpdateFor(buyer, update.id);
                        } else {
                            int interval = update.getIntervalHours();
                            String val = properties.getProperty(bid + "interval");
                            if (val != null && !val.isEmpty()) {
                                try {
                                    interval = Integer.parseInt(val);

                                    if (interval <= 0) {
                                        responder.getCommunicator().sendNormalServerMessage("Update interval for " + update.template.getName() + " must be greater than 0.");
                                        interval = update.getIntervalHours();
                                    }
                                } catch (NumberFormatException e) {
                                    responder.getCommunicator().sendNormalServerMessage("Failed to set the weight for " + update.template.getName() + ".");
                                }
                            }

                            try {
                                BuyerScheduler.updateUpdateDetails(buyer, update, SetBuyerPricesQuestion.getItemDetails(responder, getAnswer(), bid, update.template.getTemplateId(), update.template.getName(), update.getWeight()), interval);
                            } catch (SQLException e) {
                                logger.warning("Error when updating buyer update details:");
                                e.printStackTrace();
                                responder.getCommunicator().sendAlertServerMessage(buyer.getName() + " looks confused and forgets what they were doing.");
                            } catch (BuyerScheduler.UpdateAlreadyExists e) {
                                responder.getCommunicator().sendNormalServerMessage(buyer.getName() + " says 'I am already scheduling an item with those details'.");
                            }
                        }
                    }
                    responder.getCommunicator().sendNormalServerMessage("The updates are updated.");
                } else {
                    responder.getCommunicator().sendNormalServerMessage("You don't own that shop.");
                }
            }
        } else if (wasSelected("new")) {
            new AddItemToBuyerUpdateQuestion(responder, buyer.getWurmId()).sendQuestion();
        } else if (wasSelected("sort")) {
            Arrays.sort(updates);
            new SetBuyerPricesQuestion(getResponder(), getTarget()).sendQuestion();
        }
    }

    @Override
    public void sendQuestion() {
        int idx = 0;
        if (buyer.isNpcTrader()) {
            Shop shop = Economy.getEconomy().getShop(buyer);
            if (shop == null) {
                this.getResponder().getCommunicator().sendNormalServerMessage("No shop registered for that creature.");
            } else if (shop.getOwnerId() == this.getResponder().getWurmId()) {
                StringBuilder buf = new StringBuilder(this.getBmlHeaderWithScrollAndQuestion());
                DecimalFormat df = new DecimalFormat("#.##");
                buf.append("text{type=\"bold\";text=\"Scheduled Updates for ").append(buyer.getName()).append("\"}text{text=''}");
                buf.append("text{text=\"Limit restricts the Buyer from purchasing more than that number of items.  Entry will be reset to maximum on the scheduled interval.\"}");
                buf.append("text{text=\"Minimum Purchase restricts the Buyer from purchasing less than that number of items in a single trade.\"}");
                buf.append("table{rows=\"").append(updates.length + 1).append("\"; cols=\"11\";label{text=\"Item name\"};label{text=\"Weight\"};label{text=\"Min. QL\"};label{text=\"Gold\"};label{text=\"Silver\"};label{text=\"Copper\"};label{text=\"Iron\"}label{text=\"Limit\"};label{text=\"Min. Purchase\"};label{text=\"Accept Damaged\"};label{text=\"Interval\"};label{text=\"Remove?\"}");

                for (BuyerScheduler.Update update : updates) {
                    ++idx;
                    Change change = Economy.getEconomy().getChangeFor(update.getPrice());
                    buf.append(AddItemToBuyerQuestion.getTemplateString(update.template, update.material));
                    buf.append("harray{input{maxchars=\"8\"; id=\"").append(idx).append("weight\";text=\"").append(WeightString.toString(update.getWeight())).append("\"};label{text=\"kg \"}};");
                    buf.append("harray{input{maxchars=\"3\"; id=\"").append(idx).append("q\";text=\"").append(df.format(update.getMinQL())).append("\"};label{text=\" \"}};");
                    buf.append("harray{input{maxchars=\"3\"; id=\"").append(idx).append("g\";text=\"").append(change.getGoldCoins()).append("\"};label{text=\" \"}};");
                    buf.append("harray{input{maxchars=\"2\"; id=\"").append(idx).append("s\";text=\"").append(change.getSilverCoins()).append("\"};label{text=\" \"}};");
                    buf.append("harray{input{maxchars=\"2\"; id=\"").append(idx).append("c\";text=\"").append(change.getCopperCoins()).append("\"};label{text=\" \"}};");
                    buf.append("harray{input{maxchars=\"2\"; id=\"").append(idx).append("i\";text=\"").append(change.getIronCoins()).append("\"};label{text=\" \"}};");
                    buf.append("harray{input{maxchars=\"4\"; id=\"").append(idx).append("r\";text=\"").append(update.getRemainingToPurchase()).append("\"};label{text=\" \"}};");
                    buf.append("harray{input{maxchars=\"3\"; id=\"").append(idx).append("p\";text=\"").append(update.getMinimumPurchase()).append("\"};label{text=\" \"}};");
                    buf.append("harray{checkbox{id=\"").append(idx).append("d\"").append(update.getAcceptsDamaged() ? ";selected=\"true\"" : "").append("};label{text=\" \"}};");
                    buf.append("harray{input{maxchars=\"4\"; id=\"").append(idx).append("interval\";text=\"").append(update.getIntervalHours()).append("\"};label{text=\" \"}};");
                    buf.append("harray{checkbox{id=\"").append(idx).append("remove\"};label{text=\" \"}};");
                    this.updatesMap.put(update, idx);
                }

                buf.append("}");
                buf.append("text{text=\"\"}");
                buf.append("harray {button{text='Save Prices';id='submit'};label{text=\" \";id=\"spacedlxg\"};button{text='Schedule New';id='new'}label{text=\" \";id=\"spacedlxg\"};button{text='Sort';id='sort'}}}}null;null;}");
                this.getResponder().getCommunicator().sendBml(650, 300, true, true, buf.toString(), 200, 200, 200, this.title);
            } else {
                this.getResponder().getCommunicator().sendNormalServerMessage("You don't own that shop.");
            }
        }
    }
}
