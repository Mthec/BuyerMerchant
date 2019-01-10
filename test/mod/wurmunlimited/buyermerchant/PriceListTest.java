package mod.wurmunlimited.buyermerchant;

import com.google.common.base.Joiner;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.*;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.WurmObjectsFactory;
import mod.wurmunlimited.WurmTradingTest;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PriceListTest {

    public static final String one = "1,1,1.0,10";
    public static final String two = "2,2,2.1,20";
    private WurmObjectsFactory factory;

    private Item createPriceList(String str) {
        return factory.createPriceList(str);
    }

    @BeforeEach
    void setUp() throws Throwable {
        factory = new WurmObjectsFactory();
    }

    @Test
    void testNotFailOnEmptyPriceList() {
        Item priceList = createPriceList("");
        assertDoesNotThrow(() -> new PriceList(priceList));
    }

    @Test
    void testLoadPriceListOneLine() {
        assertEquals(1, new PriceList(createPriceList(one)).size());
    }

    @Test
    void testLoadPriceListTwoLine() {
        assertEquals(2, new PriceList(createPriceList(Joiner.on("\n").join(one, two))).size());
    }

    @Test
    void testLoadPriceListItem() {
        PriceList priceList = new PriceList(createPriceList(one));
        assertEquals(priceList.new Entry(1, (byte)1, 1.0f, 10, 1).toString(), priceList.iterator().next().toString());
    }

    @Test
    void testPriceListOnBuyer() {
        Creature buyer = factory.createNewBuyer(factory.createNewPlayer());
        assertDoesNotThrow(() -> PriceList.getPriceListFromBuyer(buyer));
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    void testNoPriceListException() {
        Creature buyer = factory.createNewBuyer(factory.createNewPlayer());
        try {
            ItemsPackageFactory.removeItem(buyer, buyer.getInventory().getItems().stream().filter(PriceList::isPriceList).findFirst().get());
        } catch (NullPointerException e) {
            throw new RuntimeException(e);
        }
        assertThrows(PriceList.NoPriceListOnBuyer.class, () -> PriceList.getPriceListFromBuyer(buyer));
    }

    @Test
    void testSaveAndLoad() throws PriceList.PriceListFullException {
        Item priceListItem = createPriceList(one);
        PriceList priceList = new PriceList(priceListItem);
        priceList.savePriceList();
        PriceList priceList2 = new PriceList(priceListItem);
        assertEquals(priceList.iterator().next().toString(), priceList2.iterator().next().toString());
    }

    @Test
    void testDifferent() {
        PriceList priceList = new PriceList(createPriceList(one));
        assertEquals(priceList.new Entry(one).toString(), priceList.iterator().next().toString());
    }

    @Test
    void testAddingAboveMaxPageSize() throws PriceList.PriceListFullException, NoSuchTemplateException, IOException {
        StringBuilder stringBuilder = new StringBuilder(512);
        for (int i = 1; i <= 46; i++)
            stringBuilder.append(i).append(",1,1.0,1\n");
        String str = stringBuilder.toString();
        System.out.println(stringBuilder.length());
        assert str.length() <= 500;
        Item priceListItem = createPriceList(str);
        PriceList priceList = new PriceList(priceListItem);
        assertEquals(1, priceListItem.getItemCount());
        priceList.addItem(1, (byte)2, 1.0f, 10);
        priceList.savePriceList();
        assertEquals(2, priceListItem.getItemCount());
    }

    @Test
    void testAddingAboveMaxBookSize() {
        Item priceListItem = createPriceList("");
        StringBuilder stringBuilder = new StringBuilder(512);
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j <= 49; j++) {
                stringBuilder.append(1).append(i).append(j).append(",1,1.0,1\n");
            }
            Item newPage = factory.createNewItem(ItemList.papyrusSheet);
            priceListItem.insertItem(newPage);
            newPage.setName("Buy List Page " + i);
            newPage.setInscription(stringBuilder.toString(), "");
            stringBuilder = new StringBuilder();
        }

        assert priceListItem.getItemCount() == 10;
        PriceList priceList = new PriceList(priceListItem);
        assertThrows(PriceList.PriceListFullException.class, () -> priceList.addItem(1, (byte)2, 1.0f, 10));
    }

    @Test
    void testPriceTestIterator() {
        List<String> allItems = Arrays.asList(one, two, one + "," + 100, two + "," + 200);
        Collections.sort(allItems);
        PriceList priceList = new PriceList(createPriceList(Joiner.on("\n").join(allItems)));
        List<String> newItems = new ArrayList<>(4);
        priceList.stream().forEach(priceItem -> newItems.add(priceItem.toString()));
        Collections.sort(newItems);
        assertEquals(allItems, newItems);
    }

    // PriceList.Entry tests.
    @Test
    void testUpdate () throws PriceList.PriceListFullException {
        PriceList priceList = new PriceList(createPriceList(one));
        String oldString = priceList.toString();
        priceList.iterator().next().updateItem(2, (byte)2, 2, 10000000, 1);
        assertEquals(oldString, priceList.toString());
    }

    @Test
    void testUpdateDetails () throws PriceList.PriceListFullException {
        PriceList priceList = new PriceList(createPriceList(one));
        String oldString = priceList.toString();
        priceList.iterator().next().updateItemDetails(2, 10000000, 1);
        assertEquals(oldString, priceList.toString());
    }

    @Test
    void testUpdateAboveMaxSize() {
        StringBuilder stringBuilder = new StringBuilder(512);
        for (int i = 0; i <= 49; ++i)
            stringBuilder.append(one);
        String str = stringBuilder.toString();
        assert str.length() <= 500;
        PriceList priceList = new PriceList(createPriceList(str));
        assertThrows(PriceList.PriceListFullException.class, () -> priceList.iterator().next().updateItem(2, (byte)2, 2, 10000000, 1));
    }

    @Test
    void testGetPrice() {
        PriceList priceList = new PriceList(createPriceList(one));
        Item item = factory.createNewItem();
        item.setTemplateId(1);
        item.setMaterial((byte)1);
        item.setQualityLevel(1.0f);
        assertEquals(10, priceList.getPrice(item));
    }

    @Test
    void testGetPriceGetsMax() {
        PriceList priceList = new PriceList(createPriceList("1,1,1.0f,1\n1,1,50.0f,10"));
        Item item = factory.createNewItem();
        item.setTemplateId(1);
        item.setMaterial((byte)1);
        item.setQualityLevel(60.0f);
        assertEquals(10, priceList.getPrice(item));
    }

    @Test
    void testGetPriceWrongTemplate() {
        PriceList priceList = new PriceList(createPriceList(one));
        Item item = factory.createNewItem();
        item.setTemplateId(2);
        item.setMaterial((byte)1);
        item.setQualityLevel(1.0f);
        assertEquals(PriceList.unauthorised, priceList.getPrice(item));
    }

    @Test
    void testGetPriceWrongMaterial() {
        PriceList priceList = new PriceList(createPriceList(one));
        Item item = factory.createNewItem();
        item.setTemplateId(1);
        item.setMaterial((byte)2);
        item.setQualityLevel(1.0f);
        assertEquals(PriceList.unauthorised, priceList.getPrice(item));
    }

    @Test
    void testGetPriceUnderMinQL() {
        PriceList priceList = new PriceList(createPriceList("1,1,10.0f,10"));
        Item item = factory.createNewItem();
        item.setTemplateId(2);
        item.setMaterial((byte)1);
        item.setQualityLevel(1.0f);
        assertEquals(PriceList.unauthorised, priceList.getPrice(item));
    }

    @Test
    void testIsPriceList() {
        Item item = factory.createPriceList();
        assertTrue(PriceList.isPriceList(item));
    }

    @Test
    void testIsNotPriceList() {
        Item item = factory.createNewItem();
        item.setDescription("Price List");
        assertFalse(PriceList.isPriceList(item));
    }

    @Test
    void testPriceItemUsesMinQLOnInvalidValue() throws PriceList.PriceListFullException {
        PriceList priceList = new PriceList(createPriceList(one));
        PriceList.Entry item = priceList.iterator().next();
        float ql = item.minQL;

        item.updateItem(item.template, item.material, 101, item.price, 1);
        assertEquals(ql, item.minQL);
        item.updateItem(item.template, item.material, -0.1f, item.price, 1);
        assertEquals(ql, item.minQL);
    }

    @Test
    void testCreateItem() {
        int templateId = 7;
        byte material = 42;
        float ql = 97.5f;
        int price = 33445566;
        PriceList priceList = new PriceList(createPriceList(Joiner.on(",").join(templateId, material, ql, price)));
        assertEquals(1, priceList.getItems().size());
        TempItem item = priceList.getItems().iterator().next();

        assertAll(
                () -> assertEquals(templateId, item.getTemplateId()),
                () -> assertEquals(material, item.getMaterial()),
                () -> assertEquals(ql, item.getQualityLevel()),
                () -> assertEquals(price, item.getPrice()));
    }

    @Test
    void testPriceItemGetName() {
        // TODO - When will I need material as well?
        // Apple-wood hatchet
        PriceList priceList = new PriceList(createPriceList("7,42,1.0,10"));
        assertEquals("hatchet", priceList.iterator().next().getName());
    }

    @Test
    void testGetItems() {
        PriceList priceList = new PriceList(createPriceList("7,42,1.0,10\n1,0,2.0,20"));
        TempItem[] items = priceList.getItems().toArray(new TempItem[0]);
        TempItem item1;
        TempItem item2;
        if (items[0].getTemplateId() == 7) {
            item1 = items[0];
            item2 = items[1];
        } else {
            item1 = items[1];
            item2 = items[0];
        }

        assertAll(
                () -> assertEquals(2, items.length),

                () -> assertEquals(7, item1.getTemplateId()),
                () -> assertEquals((byte)42, item1.getMaterial()),
                () -> assertEquals(1.0f, item1.getQualityLevel()),

                () -> assertEquals(1, item2.getTemplateId()),
                () -> assertEquals((byte)0, item2.getMaterial()),
                () -> assertEquals(2.0f, item2.getQualityLevel())
        );
    }

    @Test
    void testDestroyItems() {
        PriceList priceList = new PriceList(createPriceList("7,42,1.0,10\n1,0,2.0,20"));
        TempItem[] items = priceList.getItems().toArray(new TempItem[2]);
        TempItem item1;
        TempItem item2;
        if (items[0].getTemplateId() == 7) {
            item1 = items[0];
            item2 = items[1];
        } else {
            item1 = items[1];
            item2 = items[0];
        }

        priceList.destroyItems();

        TempItem[] items2 = priceList.getItems().toArray(new TempItem[2]);
        TempItem item3;
        TempItem item4;
        if (items2[0].getTemplateId() == 7) {
            item3 = items2[0];
            item4 = items2[1];
        } else {
            item3 = items2[1];
            item4 = items2[0];
        }

        assertThrows(NoSuchItemException.class, () -> Items.getItem(item1.getWurmId()));
        assertThrows(NoSuchItemException.class, () -> Items.getItem(item2.getWurmId()));
        assertNotSame(item1, item3);
        assertNotSame(item2, item4);
    }

    @Test
    void testNewItemsCreatedIfAlreadyCalledGetItems() throws PriceList.PriceListFullException, IOException, NoSuchTemplateException {
        PriceList priceList = new PriceList(createPriceList("7,42,1.0,10\n1,0,2.0,20"));
        Set<TempItem> firstItems = priceList.getItems();
        int templateId = 50;
        byte material = (byte)5;
        float ql = 2.5f;
        int price = 100;
        priceList.addItem(templateId, material, ql, price);
        TempItem[] secondItems = priceList.getItems().toArray(new TempItem[0]);
        TempItem newItem = null;
        for (TempItem item : secondItems) {
            if (!firstItems.contains(item)) {
                newItem = item;
                break;
            }
        }
        assert newItem != null;

        TempItem finalNewItem = newItem;
        assertAll(
                () -> assertEquals(templateId, finalNewItem.getTemplateId()),
                () -> assertEquals(material, finalNewItem.getMaterial()),
                () -> assertEquals(ql, finalNewItem.getQualityLevel())
        );
    }

    @Test
    void testRemoveItem() {
        PriceList priceList = new PriceList(createPriceList(one + "\n" + two));
        assertEquals(2, priceList.getItems().size());
        PriceList.Entry item = priceList.stream().filter(e -> e.template == 1).findAny().orElseThrow(RuntimeException::new);

        priceList.removeItem(item);

        assertEquals(1, priceList.getItems().size());
        assertEquals(2, priceList.getItems().iterator().next().getTemplateId());
    }

    @Test
    void testAnyMaterialGetsPriceCorrectly() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException {
        Item item = factory.createNewItem(factory.getIsWoodId());
        Creature buyer = factory.createNewBuyer(factory.createNewPlayer());
        int price = 101;
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        priceList.addItem(item.getTemplateId(), (byte)0, 1.0f, price);
        priceList.savePriceList();

        for (byte i = 0; i < ItemMaterials.MATERIAL_MAX; ++i) {
            item.setMaterial(i);
            assertEquals(price, priceList.getPrice(item));
        }
    }

    @Test
    void testOldPriceListsRenamedOnLoad() throws PriceList.NoPriceListOnBuyer {
        Creature buyer = factory.createNewBuyer(factory.createNewPlayer());
        Item priceList = buyer.getInventory().getFirstContainedItem();
        priceList.setDescription("Price List");

        assert PriceList.isPriceList(priceList);

        PriceList.getPriceListFromBuyer(buyer);
        assertEquals("Buy List", priceList.getDescription());
    }

    @Test
    void testMinimumPurchaseOption() {
        PriceList priceList = new PriceList(createPriceList(Joiner.on("\n").join(one, two + ",100")));

        PriceList.Entry first;
        PriceList.Entry second;
        List<PriceList.Entry> entries = new ArrayList<>(2);
        priceList.iterator().forEachRemaining(entries::add);
        if (entries.get(0).template == 1) {
            first = entries.get(0);
            second = entries.get(1);
        } else {
            first = entries.get(1);
            second = entries.get(0);
        }

        assertEquals(1, first.minimumPurchase);
        assertEquals(100, second.minimumPurchase);
    }

    @Test
    void testMinimumPurchaseItemStrings() {
        int hatchetId = 7;
        String str = ",0,1.0,1";
        PriceList priceList = new PriceList(createPriceList(Joiner.on("\n").join(hatchetId + str, factory.getIsWoodId() + str + ",100")));

        Item first;
        Item second;
        List<PriceList.Entry> entries = new ArrayList<>(2);
        priceList.iterator().forEachRemaining(entries::add);
        if (entries.get(0).template == hatchetId) {
            first = entries.get(0).getItem();
            second = entries.get(1).getItem();
        } else {
            first = entries.get(1).getItem();
            second = entries.get(0).getItem();
        }

        assertEquals("hatchet, any", first.getName());
        assertEquals("log, any - minimum 100", second.getName());
    }

    @Test
    void testGetMinimumRequirement() {
        PriceList priceList = new PriceList(createPriceList(Joiner.on("\n").join(one, two + ",100")));

        PriceList.Entry first;
        PriceList.Entry second;
        List<PriceList.Entry> entries = new ArrayList<>(2);
        priceList.iterator().forEachRemaining(entries::add);
        if (entries.get(0).template == 1) {
            first = entries.get(0);
            second = entries.get(1);
        } else {
            first = entries.get(1);
            second = entries.get(0);
        }

        assertEquals(1, first.getMinimumPurchase());
        assertEquals(100, second.getMinimumPurchase());
    }

    @Test
    void testMinimumRequirementAddItem() {
        PriceList priceList = new PriceList(createPriceList(""));

        assertDoesNotThrow(() -> priceList.addItem(factory.getIsMetalId(), ItemMaterials.MATERIAL_IRON, 1.0f, 1, 100));
        assertDoesNotThrow(priceList::savePriceList);

        assertEquals(100, priceList.iterator().next().minimumPurchase);
    }

    @Test
    void testIncorrectPriceListInscriptionRemovesEntry() throws NoSuchFieldException, IllegalAccessException {
        // Good values.
        String template = "1";
        String material = "0";
        String ql = "1.0";
        String price = "1";
        String[] badValues = new String[] {
                "-2", "abc", "1.0"
        };

        Field logger = PriceList.class.getDeclaredField("logger");
        logger.setAccessible(true);
        Logger mockLogger = mock(Logger.class);
        logger.set(null, mockLogger);

        int categories = 4 + 1;
        for (int i = 0; i < categories; ++i) {
            for (String badValue : badValues) {
                StringBuilder builder = new StringBuilder();
                switch (i) {
                    case 0:
                        builder.append(badValue).append(",").append(material).append(",").append(ql).append(",").append(price).append("\n");
                        break;
                    case 1:
                        builder.append(template).append(",").append(badValue).append(",").append(ql).append(",").append(price).append("\n");
                        break;
                    case 2:
                        if (badValue.equals("1.0"))
                            continue;
                        builder.append(template).append(",").append(material).append(",").append(badValue).append(",").append(price).append("\n");
                        break;
                    case 3:
                        builder.append(template).append(",").append(material).append(",").append(ql).append(",").append(badValue).append("\n");
                        break;
                    case 4:
                        builder.append(template).append(",").append(material).append(",").append(ql).append(",").append(price).append(",").append(badValue).append("\n");
                        break;
                }

                String str = builder.toString();
                PriceList priceList = new PriceList(createPriceList(str));
                assertEquals(0, priceList.size(), "Bad Price List Entry ignored - " + str);
                verify(mockLogger).warning("Bad Price List Entry - " + str.trim() + " - Removing.");
            }
        }
    }

    @Test
    void testMinusOneWorksForPrice() throws NoSuchFieldException, IllegalAccessException {
        // Good values.
        String template = "1";
        String material = "0";
        String ql = "1.0";
        String price = "-1";

        Field logger = PriceList.class.getDeclaredField("logger");
        logger.setAccessible(true);
        Logger mockLogger = mock(Logger.class);
        logger.set(null, mockLogger);



        String str = template + "," + material + "," + ql + "," + price + "\n";
        PriceList priceList = new PriceList(createPriceList(str));
        assertEquals(1, priceList.size(), "Good Price List Entry ignored - " + str);
        verify(mockLogger, never()).warning("Bad Price List Entry - " + str.trim() + " - Removing.");
    }

    @Test
    void testDuplicatePriceListEntryUpdatesInstead() throws PriceList.PriceListFullException, NoSuchTemplateException, IOException {
        PriceList priceList = new PriceList(createPriceList(""));

        priceList.addItem(1, (byte)1, 1.0f, 1);
        assertEquals(1, priceList.iterator().next().getPrice());

        priceList.addItem(1, (byte)1, 1.0f, 1);
        priceList.addItem(1, (byte)1, 1.0f, 2);
        assertEquals(1, priceList.size());
        assertEquals(2, priceList.iterator().next().getPrice());
    }

    @Test
    void testDuplicatePriceListEntryUpdatesItemToo() throws PriceList.PriceListFullException, NoSuchTemplateException, IOException {
        PriceList priceList = new PriceList(createPriceList(""));

        priceList.addItem(1, (byte)1, 1.0f, 1);
        assertEquals(1, priceList.getItems().iterator().next().getPrice());

        priceList.addItem(1, (byte)1, 1.0f, 1);
        priceList.addItem(1, (byte)1, 1.0f, 2);
        assertEquals(1, priceList.size());
        assertEquals(2, priceList.getItems().iterator().next().getPrice());
    }

    @Test
    void testPriceListSorting() throws PriceList.PriceListFullException {
        String list =  ItemList.icecream + ",1,1.0,10\n" +
                       ItemList.hatchet + ",1,25.0,10\n" +
                       ItemList.hatchet + ",1,55.0,10\n" +
                       ItemList.acorn + ",1,1.0,10\n";
        PriceList priceList = new PriceList(createPriceList(list));
        PriceList.Entry[] unsortedArray = priceList.asArray();
        assertEquals(ItemList.icecream, unsortedArray[0].template);
        assertEquals(ItemList.hatchet, unsortedArray[1].template);
        assertEquals(ItemList.hatchet, unsortedArray[2].template);
        assertEquals(55.0f, unsortedArray[2].minQL);
        assertEquals(ItemList.acorn, unsortedArray[3].template);

        priceList.sortAndSave();

        assertArrayEquals(new PriceList.Entry[] {
                unsortedArray[3],
                unsortedArray[2],
                unsortedArray[1],
                unsortedArray[0]
        }, priceList.asArray());
    }

    private Item createOldPriceList() {
        Item oldPriceList = factory.createNewItem(ItemList.papyrusSheet);
        oldPriceList.setDescription("Buy List");
        oldPriceList.setInscription(one, "");
        return oldPriceList;
    }

    @Test
    void testReplaceOldPriceList() throws NoSuchTemplateException, FailedException, NoSuchItemException {
        Item inventory = factory.createNewItem(ItemList.inventory);
        Item oldPriceList = createOldPriceList();
        inventory.insertItem(oldPriceList);

        Item newPriceList = PriceList.replaceOldPriceList(oldPriceList);
        assertEquals(oldPriceList.getParent().getTemplateId(), ItemList.book);
        assertEquals(one, new PriceList(newPriceList).iterator().next().toString());
        assertEquals(1, inventory.getItemCount());
        assertTrue(inventory.getItems().contains(newPriceList));
    }

    @Test
    void testOldPriceListConvertedProperlyOnLoad() throws NoSuchItemException, PriceList.NoPriceListOnBuyer {
        Item oldPriceList = createOldPriceList();
        Creature buyer = factory.createNewBuyer(factory.createNewPlayer());
        Items.destroyItem(buyer.getInventory().getFirstContainedItem().getWurmId());
        buyer.getInventory().insertItem(oldPriceList);

        PriceList pricelist = PriceList.getPriceListFromBuyer(buyer);

        assertEquals(oldPriceList.getParent().getTemplateId(), ItemList.book);
        assertEquals(one, pricelist.iterator().next().toString());
        assertEquals(ItemList.book, buyer.getInventory().getFirstContainedItem().getTemplateId());
    }

    @Test
    void testIsOldPriceList() throws NoSuchTemplateException, FailedException {
        Item oldPriceList = createOldPriceList();

        Item newPriceList = PriceList.getNewBuyList();

        assertTrue(PriceList.isOldPriceList(oldPriceList));
        assertFalse(PriceList.isOldPriceList(newPriceList));
        assertFalse(PriceList.isPriceList(oldPriceList));
        assertTrue(PriceList.isPriceList(newPriceList));
    }

    // Think this may only apply to my old buyer created in early testing.
    @Test
    void testIsVeryOldPriceList() throws NoSuchTemplateException, FailedException {
        Item oldPriceList = createOldPriceList();
        oldPriceList.setTemplateId(ItemList.paperSheet);

        Item newPriceList = PriceList.getNewBuyList();

        assertTrue(PriceList.isOldPriceList(oldPriceList));
        assertFalse(PriceList.isOldPriceList(newPriceList));
        assertFalse(PriceList.isPriceList(oldPriceList));
        assertTrue(PriceList.isPriceList(newPriceList));
    }

    @Test
    void testCreatePageBuyList() throws NoSuchTemplateException, FailedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Item priceListItem = PriceList.getNewBuyList();
        PriceList priceList = new PriceList(priceListItem);

        assertEquals(0, priceListItem.getItemCount());

        Method createPage = PriceList.class.getDeclaredMethod("createPage");
        createPage.setAccessible(true);
        createPage.invoke(priceList);

        assertEquals(1, priceListItem.getItemCount());
        assertEquals(ItemList.papyrusSheet, priceListItem.getFirstContainedItem().getTemplateId());
        assertEquals("Buy List Page 1", priceListItem.getFirstContainedItem().getDescription());
        assertNotNull(priceListItem.getFirstContainedItem().getInscription());
    }

    @Test
    void testCreatePageSellList() throws NoSuchTemplateException, FailedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Item priceListItem = PriceList.getNewSellList();
        PriceList priceList = new PriceList(priceListItem);

        assertEquals(0, priceListItem.getItemCount());

        Method createPage = PriceList.class.getDeclaredMethod("createPage");
        createPage.setAccessible(true);
        createPage.invoke(priceList);

        assertEquals(1, priceListItem.getItemCount());
        assertEquals(ItemList.papyrusSheet, priceListItem.getFirstContainedItem().getTemplateId());
        assertEquals("Sell List Page 1", priceListItem.getFirstContainedItem().getDescription());
    }
}
