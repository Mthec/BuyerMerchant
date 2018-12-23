package mod.wurmunlimited.buyermerchant;

import com.google.common.base.Joiner;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.*;
import com.wurmonline.shared.exceptions.WurmServerException;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PriceList implements Iterable<PriceList.Entry> {
    @Deprecated
    private static final String PRICE_LIST_DESCRIPTION = "Price List";
    private static final String BUY_LIST_DESCRIPTION = "Buy List";
    private static final String SELL_LIST_DESCRIPTION = "Sell List";
    private static final Set<String> descriptions = new HashSet<>(Arrays.asList(
            PRICE_LIST_DESCRIPTION,
            BUY_LIST_DESCRIPTION,
            SELL_LIST_DESCRIPTION
    ));
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
    // Causes testIncorrectPriceListInscriptionRemovesEntry to fail if final.
    private static Logger logger = Logger.getLogger(PriceList.class.getName());

    public class Entry {
        int template;
        // Using 0 as substitute for Any.
        byte material;
        float minQL;
        int price;
        int minimumPurchase;

        Entry(String entry) throws NumberFormatException {
            String[] entries = entry.split(",");
            template = Integer.parseInt(entries[0]);
            if (template < 1)
                throw new NumberFormatException("Template id was " + entries[0]);
            material = Byte.parseByte(entries[1]);
            if (material < (byte)0)
                throw new NumberFormatException("Material id was " + entries[1]);
            minQL = Float.parseFloat(entries[2]);
            if (minQL < 0)
                throw new NumberFormatException("minQl was " + entries[2]);
            // -1 for bad price format in menu.  Saves having to go all the way through again.
            price = Integer.parseInt(entries[3]);
            if (price < -1)
                throw new NumberFormatException("Price was " + entries[3]);
            if (entries.length == 5) {
                minimumPurchase = Integer.parseInt(entries[4]);
                if (minimumPurchase < 0)
                    throw new NumberFormatException("minQl was " + entries[4]);
            }
            if (minimumPurchase == 0)
                minimumPurchase = 1;
        }

        Entry(int template, byte material, float minQL, int price, int minimumPurchase) {
            update(template, material, minQL, price, minimumPurchase);
        }

        private void update(int template, byte material, float minQL, int price, int minimumPurchase) {
            this.template = template;
            this.material = material;
            this.minQL = minQL;
            this.price = price;
            this.minimumPurchase = minimumPurchase;
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
            if (minimumPurchase != 1)
                return Joiner.on(",").join(template, material, minQL, price, minimumPurchase);
            return Joiner.on(",").join(template, material, minQL, price);
        }

        public void updateItemDetails(float minQL, int price, int minimumPurchase) throws PriceListFullException {
            updateItem(template, material, minQL, price, minimumPurchase);
        }

        public void updateItem(int template, byte material, float minQL, int price, int minimumPurchase) throws PriceListFullException {
            if (minQL < 0 || minQL > 100)
                minQL = this.minQL;
            int oldLength = toString().length();
            int newLength = new Entry(template, material, minQL, price, minimumPurchase).toString().length();
            if (currentInscriptionLength + (newLength - oldLength) > MAX_INSCRIPTION_LENGTH)
                throw new PriceListFullException("Not enough space for that update.");
            if (minimumPurchase == -1)
                minimumPurchase = this.minimumPurchase;
            update(template, material, minQL, price, minimumPurchase);
            currentInscriptionLength += newLength - oldLength;
        }

        public int getPrice() {
            return price;
        }

        public float getQualityLevel() {
            return minQL;
        }

        public int getMinimumPurchase() {
            return minimumPurchase;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Entry entry = (Entry)o;
            return template == entry.template &&
                           material == entry.material &&
                           Float.compare(entry.minQL, minQL) == 0 &&
                           minimumPurchase == entry.minimumPurchase;
        }

        @Override
        public int hashCode() {
            //noinspection ObjectInstantiationInEqualsHashCode
            return Objects.hash(template, material, minQL, minimumPurchase);
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
        // Rename old price lists.
        if (priceListPaper.getDescription().equals(PRICE_LIST_DESCRIPTION))
            priceListPaper.setDescription(BUY_LIST_DESCRIPTION);
        this.priceListPaper = priceListPaper;
        prices = new HashMap<>();
        InscriptionData inscription = priceListPaper.getInscription();
        if (inscription != null) {
            String inscriptionString = inscription.getInscription();
            currentInscriptionLength = inscriptionString.length();
            if (currentInscriptionLength > 0) {
                boolean error = false;
                for (String entry : inscriptionString.split("\n")) {
                    Entry newEntry = null;
                    try {
                        newEntry = new Entry(entry);
                    } catch (NumberFormatException e) {
                        error = true;
                        logger.warning("Bad Price List Entry - " + entry + " - Removing.");
                        e.printStackTrace();
                    }
                    if (newEntry != null)
                        prices.put(newEntry, null);
                }

                if (error) {
                    try {
                        savePriceList();
                    } catch (PriceListFullException e) {
                        logger.warning("PriceListFull for some reason.  This should never occur.");
                    }
                }
            }
        }
    }

    private static Item getNewPriceList() throws FailedException, NoSuchTemplateException {
        Item priceList = ItemFactory.createItem(ItemList.papyrusSheet, 10, null);
        priceList.setHasNoDecay(true);

        priceList.setInscription("", "");
        return priceList;
    }

    public static Item getNewBuyList() throws FailedException, NoSuchTemplateException {
        Item priceList = getNewPriceList();
        priceList.setDescription(BUY_LIST_DESCRIPTION);
        return priceList;
    }

    public static Item getNewSellList() throws FailedException, NoSuchTemplateException {
        Item priceList = getNewPriceList();
        priceList.setDescription(SELL_LIST_DESCRIPTION);
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

    private Optional<Entry> getEntry(Item item) {
        return prices.keySet().stream()
       .filter(priceItem -> item.getTemplateId() == priceItem.template && (priceItem.material == (byte)0 || item.getMaterial() == priceItem.material) && item.getQualityLevel() >= priceItem.minQL).max((o1, o2) -> Float.compare(o1.minQL, o2.minQL));
    }

    @Nullable
    public Entry getEntryFor(Item item) {
        return getEntry(item).orElse(null);
    }

    public int getPrice(Item item) {
        return getEntry(item).map(priceItem -> priceItem.price).orElse(-1);
    }

    private TempItem createItem(Entry item) throws IOException, NoSuchTemplateException {
        ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(item.template);
        TempItem newItem = new TempItem(ItemFactory.generateName(template, item.material) + (item.material == (byte)0 ? ", any" : "") + (item.minimumPurchase != 1 ? " - minimum " + item.minimumPurchase : ""), template, item.minQL, "PriceList");
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

    // TODO - Remove IOException and just make getItems check for nulls?
    public Entry addItem(int templateId, byte material) throws PriceListFullException, IOException, NoSuchTemplateException {
        return addItem(templateId, material, 1.0f, 1);
    }

    public Entry addItem(int templateId, byte material, float minQL, int price) throws PriceListFullException, IOException, NoSuchTemplateException {
        return addItem(templateId, material, minQL, price, 1);
    }

    public Entry addItem(int templateId, byte material, float minQL, int price, int minimumPurchase) throws PriceListFullException, IOException, NoSuchTemplateException {
        Entry item = new Entry(templateId, material, minQL, price, minimumPurchase);
        if (prices.containsKey(item)) {
            Entry alreadyListed = prices.keySet().stream().filter(entry -> entry.equals(item)).findAny().orElse(null);
            if (alreadyListed != null) {
                alreadyListed.price = price;
                TempItem maybeItem = prices.get(alreadyListed);
                if (maybeItem != null)
                    maybeItem.setPrice(price);
                return alreadyListed;
            }
        }

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
        return descriptions.contains(item.getDescription()) && item.getInscription() != null;
    }
}
