package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Item;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.util.List;

public interface MinimumSet {

    Item getItem();

    List<PriceList.Entry> getLinked();

    int countFor(PriceList.Entry entry);

    int getPrice();
}
