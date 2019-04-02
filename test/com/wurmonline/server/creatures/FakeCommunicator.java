package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.util.ArrayList;
import java.util.List;

public class FakeCommunicator extends Communicator {

    private Creature me;
    public static final String empty = "EMPTY";
    public String lastNormalServerMessage = empty;
    private List<String> messages = new ArrayList<>(5);
    public String lastBmlContent = empty;
    private List<String> bml = new ArrayList<>(5);
    public boolean tradeWindowClosed = false;
    public Boolean tradeAgreed = null;
    public Item sentToInventory;
    public int sentToInventoryPrice;
    public boolean sentStartTrading = false;

    public FakeCommunicator(Creature creature) {
        me = creature;
    }

    public String[] getMessages() {
        return messages.toArray(new String[0]);
    }

    @Override
    public void sendNormalServerMessage(String message) {
        System.out.println(message);
        lastNormalServerMessage = message;
        messages.add(message);
    }

    @Override
    public void sendNormalServerMessage(String message, byte messageType) {
        sendNormalServerMessage(message);
    }

    @Override
    public void sendSafeServerMessage(String message) {
        System.out.println(message);
        messages.add(message);
    }

    @Override
    public void sendSafeServerMessage(String message, byte messageType) {
        sendSafeServerMessage(message);
    }

    @Override
    public void sendAlertServerMessage(String message) {
        System.out.println(message);
        messages.add(message);
    }

    @Override
    public void sendAlertServerMessage(String message, byte messageType) {
        sendAlertServerMessage(message);
    }

    public String[] getBml() {
        return bml.toArray(new String[0]);
    }

    @Override
    public void sendBml(int width, int height, boolean resizeable, boolean closeable, String content, int r, int g, int b, String title) {
        lastBmlContent = content;
        bml.add(content);
    }

    @Override
    public void sendCloseTradeWindow() {
        try {
            if (!(me instanceof Player) && ReflectionUtil.getPrivateField(me, Creature.class.getDeclaredField("tradeHandler")) != null)
                me.endTrade();
            tradeWindowClosed = true;
            System.out.println("Trade window closed.");
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendTradeAgree(Creature creature, boolean satisfied) {
        if (creature == me)
            tradeAgreed = satisfied;
    }

    @Override
    public void sendAddToInventory(Item item, long inventoryWindow, long rootid, int price) {
        sentToInventory = item;
        sentToInventoryPrice = price;
    }

    @Override
    public void sendUpdateInventoryItem(Item item) {

    }

    @Override
    public void sendStartTrading(Creature opponent) {
        sentStartTrading = true;
    }
}