package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Trade;

public class CreaturesPackageFactory {
    public static TradeHandler createTradeHandler(Creature buyer, Trade trade) {
        return new TradeHandler(buyer, trade);
    }

    public static void addItemsToTrade(Creature buyer) {
        buyer.getTradeHandler().addItemsToTrade();
    }
}
