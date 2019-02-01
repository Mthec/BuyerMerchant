package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Item;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ContractMinimum implements MinimumSet {

    private final Item contract;
    private final List<PriceList.Entry> entries = new ArrayList<>();
    private final List<Integer> counts = new ArrayList<>();
    private int price;

    ContractMinimum(Item contract, Map<PriceList.Entry, Integer> entries) {
        this.contract = contract;

        for (Map.Entry<PriceList.Entry, Integer> category : entries.entrySet()) {
            PriceList.Entry entry = category.getKey();
            int value = category.getValue();
            this.entries.add(entry);
            counts.add(value);
            price += value * entry.getPrice();
        }
    }

    @Override
    public Item getItem() {
        return contract;
    }

    @Override
    public List<PriceList.Entry> getLinked() {
        return entries;
    }

    @Override
    public int countFor(PriceList.Entry entry) {
        return counts.get(entries.indexOf(entry));
    }

    @Override
    public int getPrice() {
        return price;
    }
}
