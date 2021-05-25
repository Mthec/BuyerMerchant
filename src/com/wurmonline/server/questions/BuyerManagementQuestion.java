//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
// Modified from version by Code Club AB.
//

package com.wurmonline.server.questions;

import com.wurmonline.server.*;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.creatures.BuyerHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.*;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.shared.util.StringUtilities;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.wurmonline.server.creatures.CreaturePackageCaller.saveCreatureName;
import static com.wurmonline.server.questions.QuestionParser.*;
import static mod.wurmunlimited.buyermerchant.BuyerMerchant.BUYER_NAME_PREFIX;

public class BuyerManagementQuestion extends QuestionExtension implements TimeConstants {
    private static final Logger logger = Logger.getLogger(BuyerManagementQuestion.class.getName());
    private final boolean isDismissing;
    private static Field moneySpentLife = null;

    // Contract
    public BuyerManagementQuestion(Creature aResponder, long aTarget) {
        super(aResponder, "Managing buyer.", "Set the options you prefer:", 22, aTarget);
        this.isDismissing = false;
    }

    // Buyer
    public BuyerManagementQuestion(Creature aResponder, Creature aTarget) {
        super(aResponder, "Dismiss trader", "Do you want to dismiss this buyer?", 22, aTarget.getWurmId());
        this.isDismissing = true;
    }

    // See QuestionParser.parseTraderManagementQuestion
    private static void parseBuyerManagementQuestion(BuyerManagementQuestion question) {
        Creature responder = question.getResponder();
        Shop shop = null;
        Item contract = null;
        Properties props = question.getAnswer();
        Creature trader = null;
        long traderId;

        try {
            contract = Items.getItem(question.getTarget());
            if (contract.getOwner() != responder.getWurmId()) {
                responder.getCommunicator().sendNormalServerMessage("You are no longer in possession of the " + contract.getName() + "!");
                return;
            }

            traderId = contract.getData();
            if (traderId != -1L) {
                trader = Server.getInstance().getCreature(traderId);
                shop = Economy.getEconomy().getShop(trader);
            }
        } catch (NoSuchItemException var20) {
            logger.log(Level.WARNING, responder.getName() + " contract is missing! Contract ID: " + question.getTarget());
            responder.getCommunicator().sendNormalServerMessage("You are no longer in possession of the contract!");
            return;
        } catch (NoSuchPlayerException var21) {
            logger.log(Level.WARNING, "Trader for " + responder.getName() + " is a player? Well it can't be found. Contract ID: " + question.getTarget());
            responder.getCommunicator().sendNormalServerMessage("The contract has been damaged by water. You can't read the letters!");
            contract.setData(-1, -1);

            return;
        } catch (NoSuchCreatureException var22) {
            logger.log(Level.WARNING, "Trader for " + responder.getName() + " can't be found. Contract ID: " + question.getTarget());
            responder.getCommunicator().sendNormalServerMessage("The contract has been damaged by water. You can't read the letters!");
            contract.setData(-1, -1);

            return;
        } catch (NotOwnedException var23) {
            responder.getCommunicator().sendNormalServerMessage("You are no longer in possession of the " + contract.getName() + "!");
            return;
        }

        String idName;
        String val;
        boolean stall;
        if (shop != null) {
            idName = traderId + "dismiss";
            val = props.getProperty(idName);
            if (Boolean.parseBoolean(val)) {
                if (!trader.isTrading()) {
                    Server.getInstance().broadCastAction(trader.getName() + " grunts, packs " + trader.getHisHerItsString() + " things and is off.", trader, 5);
                    responder.getCommunicator().sendNormalServerMessage("You dismiss " + trader.getName() + " from " + trader.getHisHerItsString() + " post.");
                    logger.log(Level.INFO, responder.getName() + " dismisses trader " + trader.getName() + " with Contract ID: " + question.getTarget());
                    trader.destroy();
                    contract.setData(-1, -1);
                } else {
                    responder.getCommunicator().sendNormalServerMessage(trader.getName() + " is trading. Try later.");
                }
            } else {
                idName = traderId + "manage";
                val = props.getProperty(idName);
                stall = Boolean.parseBoolean(val);
                if (stall) {
                    SetBuyerPricesQuestion mpm = new SetBuyerPricesQuestion(responder, traderId);
                    mpm.sendQuestion();
                }
            }
        } else {
            idName = props.getProperty("ptradername");
            byte sex = 0;
            if (props.getProperty("gender").equals("female")) {
                sex = 1;
            }

            if (idName != null && idName.length() > 0) {
                if (idName.length() < 3 || idName.length() > 20 || containsIllegalCharacters(idName)) {
                    if (sex == 0) {
                        idName = generateGuardMaleName();
                        responder.getCommunicator().sendSafeServerMessage("The name didn't fit the trader, so he chose another one.");
                    } else {
                        responder.getCommunicator().sendSafeServerMessage("The name didn't fit the trader, so she chose another one.");
                        idName = generateGuardFemaleName();
                    }
                }

                idName = StringUtilities.raiseFirstLetter(idName);
                idName = BUYER_NAME_PREFIX + idName;
                VolaTile tile = responder.getCurrentTile();
                if (tile != null) {
                    stall = false;

                    for (Item item : tile.getItems()) {
                        if (item.isMarketStall()) {
                            stall = true;
                            break;
                        }
                    }

                    if (!Methods.isActionAllowed(responder, Actions.MANAGE_TRADERS)) {
                        return;
                    }

                    Structure struct = tile.getStructure();
                    if (!stall && (struct == null || !struct.isFinished()) && responder.getPower() <= 1) {
                        responder.getCommunicator().sendNormalServerMessage("The trader will only set up shop inside a finished building or by a market stall.");
                    } else {
                        boolean notOk = false;

                        for (Creature creature : tile.getCreatures()) {
                            if (!creature.isPlayer()) {
                                notOk = true;
                                break;
                            }
                        }

                        if (!notOk) {
                            if (struct != null && !struct.mayPlaceMerchants(responder)) {
                                responder.getCommunicator().sendNormalServerMessage("You do not have permission to place a trader in this building.");
                            } else {
                                try {
                                    trader = Creature.doNew(9, (float)(tile.getTileX() << 2) + 2.0F, (float)(tile.getTileY() << 2) + 2.0F, 180.0F, responder.getLayer(), idName, sex, responder.getKingdomId());
                                    if (responder.getFloorLevel(true) != 0) {
                                        trader.pushToFloorLevel(responder.getFloorLevel());
                                    }

                                    Economy.getEconomy().createShop(trader.getWurmId(), responder.getWurmId());
                                    // Create Price List.
                                    trader.getInventory().insertItem(PriceList.getNewBuyList());
                                    contract.setData(trader.getWurmId());
                                    logger.info(responder.getName() + " created a trader: " + trader);
                                } catch (Exception var18) {
                                    responder.getCommunicator().sendAlertServerMessage("An error occurred in the rifts of the void. The trader was not created.");
                                    logger.log(Level.WARNING, responder.getName() + " failed to create trader.", var18);
                                }
                            }
                        } else {
                            responder.getCommunicator().sendNormalServerMessage("The trader will only set up shop where no other creatures except you are standing.");
                        }
                    }
                }
            }
        }

    }

    public static void dismissMerchant(@Nullable Creature dismisser, long target) {
        try {
            int templateId = ItemTemplateFactory.getInstance().getTemplate("personal buyer contract").getTemplateId();
            Creature trader = Creatures.getInstance().getCreature(target);
            if (trader != null) {
                if (!trader.isTrading()) {
                    Server.getInstance().broadCastAction(trader.getName() + " grunts, packs " + trader.getHisHerItsString() + " things and is off.", trader, 5);
                    if (dismisser != null) {
                        dismisser.getCommunicator().sendNormalServerMessage("You dismiss " + trader.getName() + " from " + trader.getHisHerItsString() + " post.");
                        logger.log(Level.INFO, dismisser.getName() + " dismisses trader " + trader.getName() + " with WurmID: " + target);
                    } else {
                        logger.log(Level.INFO, "Buyer " + trader.getName() + " with WurmID: " + target + " dismissed by timeout");
                    }

                    for (Item item : Items.getAllItems()) {
                        if (item.getTemplateId() == templateId && item.getData() == target) {
                            item.setData(-1, -1);
                            break;
                        }
                    }

                    Shop shop = Economy.getEconomy().getShop(trader);
                    if (shop != null) {
                        try {
                            Item backPack = ItemFactory.createItem(1, 10.0F + Server.rand.nextFloat() * 10.0F, trader.getName());
                            backPack.setDescription("Due to poor business I have moved on. Thank you for your time. " + trader.getName());
                            ArrayList<Item> largeItems = new ArrayList<>();

                            for (Item realItem : trader.getInventory().getAllItems(false)) {
                                if (!backPack.insertItem(realItem, false)) {
                                    largeItems.add(realItem);
                                } else if (PriceList.isPriceList(realItem)) {
                                    realItem.setHasNoDecay(false);
                                }
                            }

                            createMailFor(shop, backPack);

                            for (Item i : largeItems) {
                                createMailFor(shop, i);
                            }
                        } catch (FailedException | NoSuchTemplateException var12) {
                            logger.log(Level.WARNING, var12.getMessage() + " " + trader.getName() + " at " + trader.getTileX() + ", " + trader.getTileY(), var12);
                        }
                    } else {
                        logger.log(Level.WARNING, "No shop when dismissing trader " + trader.getName() + " " + trader.getWurmId());
                    }

                    trader.destroy();
                } else if (dismisser != null) {
                    dismisser.getCommunicator().sendNormalServerMessage(trader.getName() + " is trading. Try later.");
                }
            } else if (dismisser != null) {
                dismisser.getCommunicator().sendNormalServerMessage("An error occurred on the server while dismissing the trader.");
            }
        } catch (NoSuchCreatureException var13) {
            if (dismisser != null) {
                dismisser.getCommunicator().sendNormalServerMessage("The buyer can not be dismissed now.");
            }
        }

    }

    private static void createMailFor(Shop shop, Item item) {
        WurmMail mail = new WurmMail((byte)0, item.getWurmId(), shop.getOwnerId(), shop.getOwnerId(), 0L, System.currentTimeMillis() + 60000L, System.currentTimeMillis() + (Servers.isThisATestServer() ? 3600000L : 14515200000L), Servers.localServer.id, false, false);
        WurmMail.addWurmMail(mail);
        mail.createInDatabase();
        item.putInVoid();
        item.setMailed(true);
        item.setMailTimes((byte)(item.getMailTimes() + 1));
    }

    public void answer(Properties answers) {
        this.setAnswer(answers);

        if (this.isDismissing) {
            String key = "dism";
            String val = answers.getProperty(key);
            if (Boolean.parseBoolean(val)) {
                dismissMerchant(this.getResponder(), this.target);
            } else {
                this.getResponder().getCommunicator().sendNormalServerMessage("You decide not to dismiss the buyer.");
            }
        } else {
            if (wasSelected("confirm")) {
                String name = getStringProp("ptradername");
                if (name != null && !name.isEmpty()) {
                    try {
                        Creature buyer = Creatures.getInstance().getCreature(Items.getItem(target).getData());

                        String fullName = BUYER_NAME_PREFIX + StringUtilities.raiseFirstLetter(name);
                        if (QuestionParser.containsIllegalCharacters(name)) {
                            getResponder().getCommunicator().sendNormalServerMessage("The buyer didn't like that name, so they shall remain " + buyer.getName() + ".");
                        } else if (!fullName.equals(buyer.getName())) {
                            try {
                                saveCreatureName(buyer, fullName);
                                buyer.refreshVisible();
                                getResponder().getCommunicator().sendNormalServerMessage("The buyer will now be known as " + buyer.getName() + ".");
                            } catch (IOException e) {
                                logger.warning("Failed to set name (" + fullName + ") for creature (" + buyer.getWurmId() + ").");
                                getResponder().getCommunicator().sendNormalServerMessage("The buyer looks confused, what exactly is a database?");
                                e.printStackTrace();
                            }
                        }
                    } catch (NoSuchItemException | NoSuchCreatureException ignored) {}

                }
            } else if (wasSelected("add")) {
                try {
                    AddItemToBuyerQuestion addItem = new AddItemToBuyerQuestion(this.getResponder(), Items.getItem(target).getData());
                    addItem.sendQuestion();
                } catch (NoSuchItemException ignored) {
                    // Pass to parseBuyerManagementQuestion error handling.
                }
            }

            parseBuyerManagementQuestion(this);
        }

    }

    public void sendQuestion() {
        StringBuilder buf = new StringBuilder();
        if (this.isDismissing) {
            buf.append(this.mayorDismissingQuestion());
            buf.append(this.createAnswerButton3());
        } else {
            buf.append(this.contractQuestion());
        }
        if (this.isDismissing) {
            this.getResponder().getCommunicator().sendBml(250, 200, true, true, buf.toString(), 200, 200, 200, this.title);
        } else {
            this.getResponder().getCommunicator().sendBml(500, 400, true, true, buf.toString(), 200, 200, 200, this.title);
        }

    }

    private long getMoneySpentLife(Shop shop) {
        try {
            if (moneySpentLife == null) {
                moneySpentLife = Shop.class.getDeclaredField("moneySpentLife");
                moneySpentLife.setAccessible(true);
            }

            return moneySpentLife.getLong(shop);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.warning("Error when attempting to get MoneySpentLife.");
            e.printStackTrace();
            return 0;
        }
    }

    private String contractQuestion() {
        StringBuilder buf = new StringBuilder();
        Item contract = null;
        Creature trader = null;
        Shop shop = null;
        long traderId = -1L;

        try {
            contract = Items.getItem(this.target);
            traderId = contract.getData();
            if (traderId != -1L) {
                trader = Server.getInstance().getCreature(traderId);
                if (trader.isNpcTrader()) {
                    shop = Economy.getEconomy().getShop(trader);
                }
            }
        } catch (NoSuchItemException var17) {
            logger.log(Level.WARNING, this.getResponder().getName() + " contract is missing!");
        } catch (NoSuchPlayerException var18) {
            logger.log(Level.WARNING, "Trader for " + this.getResponder().getName() + " is a player? Well it can't be found.");
            contract.setData(-10L);
        } catch (NoSuchCreatureException var19) {
            logger.log(Level.WARNING, "Trader for " + this.getResponder().getName() + " can't be found.");
            contract.setData(-10L);
        }

        if (shop != null) {
            buf.append(this.getBmlHeaderWithScroll());
        } else {
            buf.append(this.getBmlHeader());
        }

        buf.append("text{type=\"bold\";text=\"Trader information:\"}");
        buf.append("text{type=\"italic\";text=\"A personal buyer tries to buy anything from other player from a list you assign. Then you can come back and collect the goods.\"}");
        buf.append("text{text=\"Buyers will only appear by market stalls or in finished structures where no other creatures but you stand.\"}");
        buf.append("text{type=\"bold\";text=\"Note that if you change kingdom for any reason, you will lose this contract since the buyer stays in the old kingdom.\"}");
        buf.append("text{text=\"If you are away for several months the buyer may leave or be forced to leave with all the items and coins in his inventory.\"}");
        if (shop != null) {
            buf.append("text{type=\"bold\";text=\"Last sold\"};text{text=\"is the number of days, hours and minutes since a personal buyer last bought an item.\"}");
            buf.append("table{rows=\"2\";cols=\"5\";label{text=\"Name\"};label{text=\"Last bought\"};label{text=\"Bought month\"};label{text=\"Bought life\"};label{text=\"Free slots\"}");
            long timeLeft = System.currentTimeMillis() - shop.getLastPolled();
            long daysLeft = timeLeft / DAY_MILLIS;
            long hoursLeft = (timeLeft - daysLeft * DAY_MILLIS) / HOUR_MILLIS;
            long minutesLeft = (timeLeft - daysLeft * DAY_MILLIS - hoursLeft * HOUR_MILLIS) / MINUTE_MILLIS;
            String times = "";
            if (daysLeft > 0L) {
                times = times + daysLeft + " days";
            }

            String aft;
            if (hoursLeft > 0L) {
                aft = "";
                if (daysLeft > 0L && minutesLeft > 0L) {
                    times = times + ", ";
                    aft = aft + " and ";
                } else if (daysLeft > 0L) {
                    times = times + " and ";
                } else if (minutesLeft > 0L) {
                    aft = aft + " and ";
                }

                times = times + hoursLeft + " hours" + aft;
            }

            if (minutesLeft > 0L) {
                times = times + minutesLeft + " minutes";
            }

            String name = trader.getName();
            if (name.startsWith(BUYER_NAME_PREFIX)) {
                buf.append("harray{label{text=\"" + BUYER_NAME_PREFIX + "\"};input{text=\"").append(name.substring(BUYER_NAME_PREFIX.length())).append("\"id=\"ptradername\";maxchars=\"20\"}}");
            } else {
                buf.append("label{text=\"").append(trader.getName()).append("\"};");
            }
            buf.append("label{text=\"").append(times).append("\"}");
            buf.append("label{text=\"").append((new Change(shop.getMoneySpentMonth())).getChangeShortString()).append("\"}");
            buf.append("label{text=\"").append((new Change(getMoneySpentLife(shop))).getChangeShortString()).append("\"}");
            if (BuyerTradingWindow.destroyBoughtItems)
                buf.append("label{text=\"N/A\"}}");
            else
                buf.append("label{text=\"").append(BuyerHandler.getMaxNumPersonalItems() - trader.getNumberOfShopItems()).append("\"}}");
            buf.append("text{type=\"bold\";text=\"Dismissing\"};text{text=\"if you dismiss a buyer they will take all items with them!\"}");
            buf.append("harray{label{text=\"Dismiss\"};checkbox{id=\"").append(traderId).append("dismiss\";selected=\"false\";text=\" \"}}");

            buf.append("harray {button{text='Confirm';id='confirm'};label{text=' ';id='spacedlxg'};button{text='Manage Prices';id='").append(traderId).append("manage'};label{text=' ';id='spacedlxg'};button{text='Add Item To List';id='add'}}}};null;null;null;null;}");
        } else {
            buf.append("text{type=\"bold\";text=\"Hire personal buyer:\"}");
            buf.append("text{text=\"By using this contract a personal buyer will appear.\"}");
            buf.append("text{text=\"The buyer will appear where you stand, if the tile contains no other creature.\"}");
            buf.append("text{text=\"Every trade he does he will charge one tenth (10%) of the value bought.\"}");
            buf.append("text{text=\"You add money to his inventory, and retrieve items he has bought by trading with him.\"}");
            buf.append("text{text=\"Gender: \"}");
            if (this.getResponder().getSex() == 1) {
                buf.append("radio{ group=\"gender\"; id=\"male\";text=\"Male\"}");
                buf.append("radio{ group=\"gender\"; id=\"female\";text=\"Female\";selected=\"true\"}");
            } else {
                buf.append("radio{ group=\"gender\"; id=\"male\";text=\"Male\";selected=\"true\"}");
                buf.append("radio{ group=\"gender\"; id=\"female\";text=\"Female\"}");
            }

            buf.append("harray{label{text=\"The buyer shalt be called " + BUYER_NAME_PREFIX + "\"};input{id=\"ptradername\";maxchars=\"20\"};label{text=\"!\"}}");
            buf.append(this.createAnswerButton3());
        }

        return buf.toString();
    }

    private String mayorDismissingQuestion() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.getBmlHeader());

        try {
            Creature trader = Creatures.getInstance().getCreature(this.target);
            if (trader.isNpcTrader()) {
                Shop shop = Economy.getEconomy().getShop(trader);
                buf.append("text{text=\"You may dismiss this buyer now, since ");
                if (shop.getMoney() == 0 && shop.howLongEmpty() > MONTH_MILLIS) {
                    buf.append("it has not had any money for a long time and is bored.\"}");
                } else {
                    buf.append("the person controlling it is long gone.\"}");
                }

                buf.append("text{text=\"Will you dismiss this buyer?\"}");
                buf.append("radio{ group=\"dism\";id=\"true\";text=\"Yes\"}");
                buf.append("radio{ group=\"dism\";id=\"false\";text=\"No\";selected=\"true\"}");
            } else {
                buf.append("text{text=\"Not a buyer?\"}");
            }
        } catch (NoSuchCreatureException var4) {
            logger.log(Level.WARNING, "Buyer for " + this.getResponder().getName() + " can't be found.");
            buf.append("label{text=\"Missing buyer?\"}");
        }

        return buf.toString();
    }
}
