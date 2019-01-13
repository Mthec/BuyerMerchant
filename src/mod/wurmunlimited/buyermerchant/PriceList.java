package mod.wurmunlimited.buyermerchant;

import com.google.common.base.Joiner;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.*;
import com.wurmonline.shared.exceptions.WurmServerException;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PriceList implements Iterable<PriceList.Entry> {
    @Deprecated
    private static final String PRICE_LIST_DESCRIPTION = "Price List";
    private static final String BUY_LIST_DESCRIPTION = "Buy List";
    private static final String SELL_LIST_DESCRIPTION = "Sell List";
    private static final String BUY_LIST_PAGE_PREFIX = "Buy List Page ";
    private static final String SELL_LIST_PAGE_PREFIX = "Sell List Page ";
    private static final Set<String> descriptions = new HashSet<>(Arrays.asList(
            PRICE_LIST_DESCRIPTION,
            BUY_LIST_DESCRIPTION,
            SELL_LIST_DESCRIPTION
    ));
    // PapyrusBehaviour line 191 says 500 max chars for paper.
    private static final int MAX_INSCRIPTION_LENGTH = 500;
    private static final int MAX_PAGES_IN_BOOK = 10;
    private Item priceListItem;
    private int pageCount = 1;
    private Map<Entry, TempItem> prices = new HashMap<>();
    private List<Entry> pricesOrder = new ArrayList<>();
    private boolean createdItems = false;
    private int lastInscriptionLength;
    public static final String noPriceListFoundPlayerMessage = "The buyer fumbles in their pockets but fails to find their price list.";
    public static final String noSpaceOnPriceListPlayerMessage = "The buyer has run out of space on their price list and cannot record the changes.  Try removing some items from the list.";
    public static final String couldNotCreateItemPlayerMessage = "The buyer looks at you confused, as if not understanding what your saying.";
    public static int unauthorised = -1;
    private static final Pattern pageName = Pattern.compile("[\\w\\s]*(Buy|Sell) List Page (\\d+)");
    // Causes testIncorrectPriceListInscriptionRemovesEntry to fail if final.
    private static Logger logger = Logger.getLogger(PriceList.class.getName());

    public class Entry implements Comparable<Entry> {
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
            if (lastInscriptionLength + (newLength - oldLength) > MAX_INSCRIPTION_LENGTH)
                throw new PriceListFullException("Not enough space for that update.");
            if (minimumPurchase == -1)
                minimumPurchase = this.minimumPurchase;
            update(template, material, minQL, price, minimumPurchase);
            lastInscriptionLength += newLength - oldLength;
        }

        public int getTemplateId() {
            return template;
        }

        public float getQualityLevel() {
            return minQL;
        }

        public int getPrice() {
            return price;
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

        /**
         * @param other Another entry.
         * @return Compared on template name, then minQL descending order.
         */
        @Override
        public int compareTo(@NotNull Entry other) {
            ItemTemplateFactory factory = ItemTemplateFactory.getInstance();
            int compare = 0;
            try {
                compare = getName().compareTo(ItemFactory.generateName(factory.getTemplate(other.template), other.material));
                if (compare == 0) {
                    compare = Float.compare(other.minQL, minQL);
                }
            } catch (NoSuchTemplateException e) {
                logger.warning("Template not found during Entry comparison.");
                e.printStackTrace();
            }

            return compare;
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

    public static class PageNotAdded extends WurmServerException {

        WurmServerException cause;

        PageNotAdded(long priceListId, WurmServerException e) {
            super("Could not add new page to price list. Price List WurmId - " + priceListId);
            cause = e;
        }

        public WurmServerException getCause() {
            return cause;
        }
    }

    PriceList(Item priceList) {
        assert isPriceList(priceList);
        this.priceListItem = priceList;
        // Rename old price lists.
        if (priceList.getDescription().equals(PRICE_LIST_DESCRIPTION))
            priceList.setDescription(BUY_LIST_DESCRIPTION);

        List<Item> pages = new ArrayList<>(priceList.getItems());

        if (pages.size() == 0)
            return;

        pageCount = pages.size();
        pages.sort((item1, item2) -> {
            Matcher page1 = pageName.matcher(item1.getDescription());
            Matcher page2 = pageName.matcher(item2.getDescription());
            if (!page1.find() || !page2.find()) {
                logger.warning("Bad page in price list - either " + item1.getName() + " or " + item2.getName());
                return 0;
            }

            if (!page1.group(1).equals(page2.group(1)))
                return page1.group(1).compareTo(page2.group(1));

            return page1.group(2).compareTo(page2.group(2));
        });

        boolean error = false;
        for (Item page : pages) {
            InscriptionData inscription = page.getInscription();
            if (inscription != null) {
                String inscriptionString = inscription.getInscription();
                lastInscriptionLength = inscriptionString.length();
                if (lastInscriptionLength > 0) {
                    for (String entry : inscriptionString.split("\n")) {
                        Entry newEntry = null;
                        try {
                            newEntry = new Entry(entry);
                        } catch (NumberFormatException e) {
                            error = true;
                            logger.warning("Bad Price List Entry - " + entry + " - Removing.");
                            e.printStackTrace();
                        }
                        if (newEntry != null) {
                            prices.put(newEntry, null);
                            pricesOrder.add(newEntry);
                        }
                    }
                }
            }
        }

        if (error) {
            try {
                savePriceList();
            } catch (PriceListFullException e) {
                logger.warning("PriceListFull for some reason.  This should never occur.");
            } catch (PageNotAdded e) {
                logger.warning("Error creating new page in Price List.");
                e.printStackTrace();
            }
        }
    }

    private static Item getNewPriceList() throws FailedException, NoSuchTemplateException {
        Item priceList = ItemFactory.createItem(ItemList.book, 10, null);
        priceList.setHasNoDecay(true);
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

    private void addPageToPriceList(Item priceListItem) throws NoSuchTemplateException, FailedException {
        Item newPage = ItemFactory.createItem(ItemList.papyrusSheet, 10, null);
        priceListItem.insertItem(newPage);
    }

    public static PriceList getPriceListFromBuyer(Creature creature) throws NoPriceListOnBuyer {
        for (Item item : creature.getInventory().getItems()) {
            if (isPriceList(item)) {
                return new PriceList(item);
            } else if (isOldPriceList(item)) {
                try {
                    return new PriceList(replaceOldPriceList(item));
                } catch (NoSuchTemplateException | FailedException | NoSuchItemException e) {
                    logger.warning("Old Price List not replaced.  Reason follows:");
                    e.printStackTrace();
                }
            }
        }
        throw new NoPriceListOnBuyer(creature.getWurmId());
    }

    @NotNull
    public Iterator<Entry> iterator() {
        return pricesOrder.iterator();
    }

    public Entry[] asArray() {
        return pricesOrder.toArray(new Entry[0]);
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
        newItem.setOwnerId(priceListItem.getOwnerId());
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

        // Plus one for newline.
        int newLength = item.toString().length() + 1;
        if (lastInscriptionLength + newLength > MAX_INSCRIPTION_LENGTH) {
            if (pageCount < MAX_PAGES_IN_BOOK) {
                pageCount += 1;
                lastInscriptionLength = 0;
            }
            else {
                throw new PriceListFullException("PriceList has too many items to inscribe.");
            }
        }
        lastInscriptionLength += newLength;
        if (createdItems)
            prices.put(item, createItem(item));
        else
            prices.put(item, null);
        pricesOrder.add(item);
        return item;
    }

    public void removeItem(Entry item) {
        if (prices.containsKey(item)) {
            TempItem temp = prices.get(item);
            if (temp != null)
                Items.destroyItem(temp.getWurmId());
            lastInscriptionLength -= item.toString().length();
            if (lastInscriptionLength < 0) {
                // Just assume it will be full?
                lastInscriptionLength = 500;
                pageCount -= 1;
            }
            prices.remove(item);
            pricesOrder.remove(item);
        }
    }

    private void renamePage(Item page, int number) {
        if (priceListItem.getDescription().equals(BUY_LIST_DESCRIPTION))
            page.setDescription(BUY_LIST_PAGE_PREFIX + number);
        else
            page.setDescription(SELL_LIST_PAGE_PREFIX + number);
    }

    public void savePriceList() throws PriceListFullException, PageNotAdded {
        List<String> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (Entry entry : pricesOrder) {
            String str = entry.toString();
            if (sb.length() + str.length() > MAX_INSCRIPTION_LENGTH) {
                lines.add(sb.toString());
                sb = new StringBuilder();
            }
            sb.append(str).append("\n");
        }
        lines.add(sb.toString());

        int numberOfPages = lines.size();
        if (numberOfPages > MAX_PAGES_IN_BOOK)
            throw new PriceListFullException("PriceList data too long to inscribe.");

        while(numberOfPages > priceListItem.getItems().size()) {
            try {
                addPageToPriceList(priceListItem);
            } catch (NoSuchTemplateException | FailedException e) {
                throw new PageNotAdded(priceListItem.getWurmId(), e);
            }
        }

        Iterator<Item> pages = priceListItem.getItems().iterator();
        int number = 1;
        for (String line : lines) {
            Item page = pages.next();
            renamePage(page, number);
            page.setInscription(line, "");
        }

        List<Item> toRemove = new ArrayList<>();
        pages.forEachRemaining(toRemove::add);
        toRemove.forEach(item -> Items.destroyItem(item.getWurmId()));
        lastInscriptionLength = lines.get(numberOfPages - 1).length();
        pageCount = lines.size();
    }

    public void sortAndSave() throws PriceListFullException, PageNotAdded {
        pricesOrder.sort(Entry::compareTo);
        savePriceList();
    }

    public static boolean isPriceList(Item item) {
        return descriptions.contains(item.getDescription()) && item.getTemplateId() == ItemList.book;
    }

    static boolean isOldPriceList(Item item) {
        return descriptions.contains(item.getDescription()) && item.getInscription() != null
           && (item.getTemplateId() == ItemList.papyrusSheet || item.getTemplateId() == ItemList.paperSheet)
           && (item.getParentOrNull() == null || item.getParentOrNull().getTemplateId() != ItemList.book);
    }

    static Item replaceOldPriceList(Item item) throws NoSuchTemplateException, FailedException, NoSuchItemException {
        Item newPriceList = getNewBuyList();
        item.getParent().insertItem(newPriceList);
        newPriceList.insertItem(item);
        item.setDescription(BUY_LIST_PAGE_PREFIX + 1);
        return newPriceList;
    }
}
