package mod.wurmunlimited.buyermerchant;

import com.google.common.base.Joiner;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.*;
import com.wurmonline.shared.exceptions.WurmServerException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PriceList implements Iterable<PriceList.Entry> {
    private static final String PRICE_LIST_DESCRIPTION = "Price List";
    // PapyrusBehaviour line 191 says 500 max chars for paper.
    private static final int MAX_INSCRIPTION_LENGTH = 500;
    private Item priceListPaper;
    private Map<Entry, TempItem> prices;
    private boolean createdItems = false;
    private int currentInscriptionLength;
    public static final String noPriceListFoundPlayerMessage = "The buyer fumbles in his pockets but fails to find his price list.";
    public static final String noSpaceOnPriceListPlayerMessage = "The buyer has run out of space on his price list and cannot record the changes.  Try removing some items from the list.";
    public static final String couldNotCreateItemPlayerMessage = "The buyer looks at you confused, as if not understanding what your saying.";
    public static int unauthorised = -1;
    private static final Logger logger = Logger.getLogger(PriceList.class.getName());

    public class Entry {
        int template;
        // Using 0 as substitute for Any.
        byte material;
        float minQL;
        int price;

        Entry(String entry) {
            String[] entries = entry.split(",");
            template = Integer.parseInt(entries[0]);
            material = Byte.parseByte(entries[1]);
            minQL = Float.parseFloat(entries[2]);
            price = Integer.parseInt(entries[3]);
        }

        Entry(int template, byte material, float minQL, int price) {
            update(template, material, minQL, price);
        }

        private void update(int template, byte material, float minQL, int price) {
            this.template = template;
            this.material = material;
            this.minQL = minQL;
            this.price = price;
        }

        public Item getItem() {
            if (!createdItems)
                getItems();
            return prices.get(this);
        }

        public String getName() {
            try {
                ItemTemplate temp = ItemTemplateFactory.getInstance().getTemplate(template);
                return ItemFactory.generateName(temp, material);
            } catch (NoSuchTemplateException e) {
                e.printStackTrace();
                return "UnknownItem";
            }
        }

        public String toString() {
            return Joiner.on(",").join(template, material, minQL, price);
        }

        public void updateItemQLAndPrice(float minQL, int price) throws PriceListFullException {
            updateItem(template, material, minQL, price);
        }

        public void updateItem(int template, byte material, float minQL, int price) throws PriceListFullException {
            if (minQL < 0 || minQL > 100)
                minQL = this.minQL;
            int oldLength = toString().length();
            int newLength = new Entry(template, material, minQL, price).toString().length();
            if (currentInscriptionLength + (newLength - oldLength) > MAX_INSCRIPTION_LENGTH)
                throw new PriceListFullException("Not enough space for that update.");
            update(template, material, minQL, price);
            currentInscriptionLength += newLength - oldLength;
        }

        public int getPrice() {
            return price;
        }

        public float getQualityLevel() {
            return minQL;
        }
    }

    public static class PriceListFullException extends WurmServerException {

        PriceListFullException(String message) {
            super(message);
        }
    }

    public static class NoPriceListOnBuyer extends IOException {

        NoPriceListOnBuyer(long buyerId) {
            super("Could not find price list on buyer. WurmId - " + buyerId);
        }
    }

    public PriceList(Item priceListPaper) {
        assert isPriceList(priceListPaper);
        this.priceListPaper = priceListPaper;
        prices = new HashMap<>();
        InscriptionData inscription = priceListPaper.getInscription();
        if (inscription != null) {
            String inscriptionString = inscription.getInscription();
            currentInscriptionLength = inscriptionString.length();
            if (currentInscriptionLength > 0)
                for (String entry : inscriptionString.split("\n")){
                    prices.put(new Entry(entry), null);
                }
        }
    }

    public static Item getNewPriceList() throws FailedException, NoSuchTemplateException {
        Item priceList = ItemFactory.createItem(ItemList.papyrusSheet, 10, null);
        priceList.setDescription(PRICE_LIST_DESCRIPTION);
        priceList.setHasNoDecay(true);

        priceList.setInscription("", "");
        return priceList;
    }

    public static PriceList getPriceListFromBuyer(Creature creature) throws NoPriceListOnBuyer {
        for (Item item : creature.getInventory().getItems()) {
            if (isPriceList(item)) {
                return new PriceList(item);
            }
        }
        throw new NoPriceListOnBuyer(creature.getWurmId());
    }

    @NotNull
    public Iterator<Entry> iterator() {
        return prices.keySet().iterator();
    }

    public Stream<Entry> stream() { return prices.keySet().stream(); }

    public int size() {
        return prices.size();
    }

    public Set<TempItem> getItems() {
        if (!createdItems) {
            for (Entry item : prices.keySet()) {
                try {
                    prices.put(item, createItem(item));
                } catch (IOException | NoSuchTemplateException e) {
                    logger.warning("Error when creating TempItem for trading.  Skipping entry.");
                    e.printStackTrace();
                }
            }
            createdItems = true;
        }

        return new HashSet<>(prices.values());
    }

    public int getPrice(Item item) {
        return prices.keySet().stream()
                .filter(priceItem -> item.getTemplateId() == priceItem.template && (priceItem.material == (byte)0 || item.getMaterial() == priceItem.material) && item.getQualityLevel() >= priceItem.minQL).max((o1, o2) -> Float.compare(o1.minQL, o2.minQL))
                .map(priceItem -> priceItem.price).orElse(-1);
    }

    private TempItem createItem(Entry item) throws IOException, NoSuchTemplateException {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(item.template);
        TempItem newItem = new TempItem(ItemFactory.generateName(template, item.material) + (item.material == (byte)0 ? ", any" : ""), template, item.minQL, "PriceList");
        newItem.setMaterial(item.material);
        newItem.setPrice(item.price);
        newItem.setOwnerId(priceListPaper.getOwnerId());
        return newItem;
    }

    public void destroyItems() {
        for (Map.Entry<Entry, TempItem> entry : prices.entrySet()) {
            Item tempItem = entry.getValue();
            if (tempItem != null)
                Items.destroyItem(tempItem.getWurmId());
            prices.replace(entry.getKey(), null);
        }
        createdItems = false;
    }

    public Entry addItem(int templateId, byte material) throws PriceListFullException, IOException, NoSuchTemplateException {
        return addItem(templateId, material, 1.0f, 1);
    }

    public Entry addItem(int templateId, byte material, float minQL, int price) throws PriceListFullException, IOException, NoSuchTemplateException {
        Entry item = new Entry(templateId, material, minQL, price);
        if (currentInscriptionLength + item.toString().length() > MAX_INSCRIPTION_LENGTH)
            throw new PriceListFullException("PriceList has too many items to inscribe.");
        if (createdItems)
            prices.put(item, createItem(item));
        else
            prices.put(item, null);
        return item;
    }

    public void removeItem(Entry item) {
        if (prices.containsKey(item)) {
            TempItem temp = prices.get(item);
            if (temp != null)
                Items.destroyItem(temp.getWurmId());
            currentInscriptionLength -= item.toString().length();
            prices.remove(item);
        }
    }

    public void savePriceList() throws PriceListFullException {
        String str = prices.keySet().stream().map(Entry::toString).collect(Collectors.joining("\n"));
        if (str.length() > MAX_INSCRIPTION_LENGTH)
            throw new PriceListFullException("PriceList data too long to inscribe.");
        priceListPaper.setInscription(str, "");
        currentInscriptionLength = str.length();
    }

    public static boolean isPriceList(Item item) {
        return item.getDescription().equals(PRICE_LIST_DESCRIPTION) && item.getInscription() != null;
    }
}
