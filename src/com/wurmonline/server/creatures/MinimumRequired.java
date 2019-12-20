package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Item;
import mod.wurmunlimited.Pair;
import mod.wurmunlimited.buyermerchant.PriceList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class MinimumRequired {

    public final PriceList.Entry entry;
    private Set<Item> individualItems = new HashSet<>();
    private Set<MinimumSet> otherItems = new HashSet<>();
    private boolean isOptimal;

    MinimumRequired(PriceList.Entry entry) {
        this.entry = entry;
        isOptimal = false;
    }

    public void addItem(Item item) {
        individualItems.add(item);
    }

    public void addItem(MinimumSet set) {
        otherItems.add(set);
    }

    public Iterable<Item> getItems() {
        return () -> new Iterator<Item>() {
            Iterator<Item> items = individualItems.iterator();
            Iterator<MinimumSet> others = otherItems.iterator();

            @Override
            public boolean hasNext() {
                return items.hasNext() || others.hasNext();
            }

            @Override
            public Item next() {
                if (items.hasNext())
                    return items.next();
                else {
                    return others.next().getItem();
                }
            }
        };
    }

    public Iterable<Pair<Item, Integer>> getItemsAndPrices() {
        return () -> new Iterator<Pair<Item, Integer>>() {
            Iterator<Item> items = individualItems.iterator();
            Iterator<MinimumSet> others = otherItems.iterator();
            final int price = entry.getPrice();

            @Override
            public boolean hasNext() {
                return items.hasNext() || others.hasNext();
            }

            @Override
            public Pair<Item, Integer> next() {
                if (items.hasNext())
                    return new Pair<>(items.next(), price);
                else {
                    MinimumSet set = others.next();
                    return new Pair<>(set.getItem(), set.getPrice());
                }
            }
        };
    }

    public Set<PriceList.Entry> getLinked() {
        return otherItems.stream().flatMap(m -> m.getLinked().stream()).collect(Collectors.toSet());
    }

    public int count() {
        return individualItems.size() + otherItems.stream().mapToInt(m -> m.countFor(entry)).sum();
    }

    public int itemCount() {
        return individualItems.size() + otherItems.size();
    }

    public int getTotalPrice() {
        if (entry.getPrice() == 0)
            return 0;
        return count() * entry.getPrice();
    }

    public Set<MinimumSet> getMinimumSets() {
        return otherItems;
    }

    public void removeInvalidSets(Set<MinimumSet> all) {
        otherItems.removeAll(all);
    }

    public boolean isOptimal() {
        return isOptimal;
    }

    public boolean shrinkToFit(int space) {
        isOptimal = true;
        int count = otherItems.size();

        if (count > space || space < entry.getMinimumPurchase()) {
            return false;
        }

        individualItems = individualItems.stream().limit(space - count).collect(Collectors.toSet());
        return true;
    }
}
