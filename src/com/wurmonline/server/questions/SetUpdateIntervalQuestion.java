package com.wurmonline.server.questions;

import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.ItemTemplate;
import mod.wurmunlimited.buyermerchant.ItemDetails;
import mod.wurmunlimited.buyermerchant.db.BuyerScheduler;

import java.sql.SQLException;
import java.util.Properties;

public class SetUpdateIntervalQuestion extends BuyerQuestionExtension {
    private final Creature buyer;
    private final ItemTemplate template;
    private final byte material;
    private final ItemDetails details;
    private final BuyerScheduler.Update update;
    private final int currentInterval;

    SetUpdateIntervalQuestion(Creature responder, Creature buyer, ItemTemplate template, byte material, ItemDetails details) {
        super(responder, "Set Interval For Update", "", QuestionTypes.PRICEMANAGE, buyer.getWurmId());
        this.buyer = buyer;
        this.template = template;
        this.material = material;
        this.details = details;
        this.update = null;
        this.currentInterval = 24;
    }

    SetUpdateIntervalQuestion(Creature responder, Creature buyer, BuyerScheduler.Update update) {
        super(responder, "Set Interval For Update", "", QuestionTypes.PRICEMANAGE, buyer.getWurmId());
        this.buyer = buyer;
        this.template = update.template;
        this.material = update.material;
        details = null;
        this.update = update;
        this.currentInterval = update.getIntervalHours();
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();

        if (wasSelected("add")) {
            String val = answers.getProperty("interval");
            if (val == null || val.isEmpty()) {
                responder.getCommunicator().sendNormalServerMessage("Invalid interval received - " + val + ".");
                resendQuestion();
            } else {
                try {
                    int interval = Integer.parseInt(val);

                    if (interval < 1) {
                        responder.getCommunicator().sendNormalServerMessage("Interval must be greater than 0.");
                        resendQuestion();
                    } else {
                        if (details != null) {
                            try {
                                BuyerScheduler.addUpdateFor(buyer, template, material, details.weight,
                                        details.minQL, details.price, details.remainingToPurchase, details.minimumPurchase,
                                        details.acceptsDamaged, interval);
                                responder.getCommunicator().sendNormalServerMessage(buyer.getName() + " added the item to their schedule.");
                            } catch (SQLException e) {
                                e.printStackTrace();
                                responder.getCommunicator().sendAlertServerMessage("Something went wrong and the update was not set.");
                            } catch (BuyerScheduler.UpdateAlreadyExists e) {
                                responder.getCommunicator().sendNormalServerMessage(buyer.getName() + " says 'I am already scheduling an item with those details'.");
                            }
                        } else if (update != null) {
                            try {
                                BuyerScheduler.setIntervalFor(update, interval * TimeConstants.HOUR_MILLIS);
                                responder.getCommunicator().sendNormalServerMessage(buyer.getName() + " updated their schedule.");
                            } catch (SQLException e) {
                                e.printStackTrace();
                                responder.getCommunicator().sendAlertServerMessage("Something went wrong and the interval was not changed.");
                            }
                        } else {
                            logger.warning("'details' and 'update' were both null, please report.");
                            responder.getCommunicator().sendAlertServerMessage("Something went wrong and the update was not set.");
                        }
                    }
                } catch (NumberFormatException e) {
                    responder.getCommunicator().sendNormalServerMessage("Invalid interval received - " + val + ".");
                    resendQuestion();
                }
            }
        }
    }

    private void resendQuestion() {
        if (details != null) {
            new SetUpdateIntervalQuestion(getResponder(), buyer, template, material, details).sendQuestion();
        } else if (update != null) {
            new SetUpdateIntervalQuestion(getResponder(), buyer, update).sendQuestion();
        }
    }

    @Override
    public void sendQuestion() {
        String buf = this.getBmlHeaderWithScrollAndQuestion() + "text{text=\"How often do you want the buyer to restock this item?\"}" +
                             "text{text=\"\"}" +
                             "label{text=\"Item:  " +
                             AddItemToBuyerQuestion.getTemplateString(template, (material != 0) ? material : template.getMaterial()) +
                             "\"}" +
                             "harray{label{text=\"Interval (Hours):  \"};input{maxchars=\"8\";id=\"interval\";text=\"" + currentInterval + "\"}}" +
                             "label{text=\"Day - 24, Week - 168, Month (28 days) - 672\"}" +
                             "text{text=\"\"}" +
                             "harray {button{text=\"Submit\";id=\"add\"};label{text=\" \";id=\"spacedlxg\"};button{text=\"Cancel\";id=\"cancel\"}}}}null;null;}";
        this.getResponder().getCommunicator().sendBml(300, 300, true, true, buf, 200, 200, 200, this.title);
    }
}
