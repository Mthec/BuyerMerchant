package mod.wurmunlimited.buyermerchant;

import com.wurmonline.server.items.NoSuchTemplateException;

import java.io.IOException;

public class EntryBuilder {
    private final PriceList priceList;
    private int templateId = 1;
    private byte material = 0;
    private int weight = -1;
    private float ql = 1.0f;
    private int price = 0;
    private int remainingToPurchase = 0;
    private int minimumRequired = 1;
    private boolean acceptsDamaged = false;
    private final PriceList.Entry entry;

    public class EntryBuilderException extends Exception {

        private EntryBuilderException(Exception e) {
            this.initCause(e);
        }
    }

    private EntryBuilder(PriceList priceList, PriceList.Entry entry) {
        this.priceList = priceList;
        this.entry = entry;

        if (entry != null) {
            templateId = entry.template;
            material = entry.material;
            weight = entry.weight;
            ql = entry.minQL;
            price = entry.price;
            remainingToPurchase = entry.remainingToPurchase;
            minimumRequired = entry.minimumPurchase;
            acceptsDamaged = entry.acceptsDamaged;
        }
    }

    public static EntryBuilder addEntry(PriceList priceList) {
        return new EntryBuilder(priceList, null);
    }

    public static EntryBuilder update(PriceList.Entry entry) {
        return new EntryBuilder(null, entry);
    }

    public EntryBuilder templateId(int templateId) {
        this.templateId = templateId;
        return this;
    }

    public EntryBuilder material(byte material) {
        this.material = material;
        return this;
    }

    public EntryBuilder weight(int weight) {
        this.weight = weight;
        return this;
    }

    public EntryBuilder ql(float ql) {
        this.ql = ql;
        return this;
    }

    public EntryBuilder price(int price) {
        this.price = price;
        return this;
    }

    public EntryBuilder remainingToPurchase(int remainingToPurchase) {
        this.remainingToPurchase = remainingToPurchase;
        return this;
    }

    public EntryBuilder minimumRequired(int minimumRequired) {
        this.minimumRequired = minimumRequired;
        return this;
    }

    public EntryBuilder acceptsDamaged(boolean acceptsDamaged) {
        this.acceptsDamaged = true;
        return this;
    }

    public EntryBuilder acceptsDamaged() {
        return acceptsDamaged(true);
    }

    public void build() throws EntryBuilderException {
        try {
            if (priceList != null)
                priceList.addItem(templateId, material, weight, ql, price, remainingToPurchase, minimumRequired, acceptsDamaged);
            else if (entry != null)
                entry.updateItemDetails(weight, ql, price, remainingToPurchase, minimumRequired, acceptsDamaged);
            else
                throw new RuntimeException("Nothing set for EntryBuilder.");
        } catch (PriceList.PriceListFullException | NoSuchTemplateException | IOException e) {
            throw new EntryBuilderException(e);
        }
    }
}
