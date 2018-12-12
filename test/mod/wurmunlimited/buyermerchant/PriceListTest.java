package mod.wurmunlimited.buyermerchant;

import com.google.common.base.Joiner;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemsPackageFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.items.TempItem;
import com.wurmonline.shared.constants.ItemMaterials;
import mod.wurmunlimited.WurmObjectsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PriceListTest {

    public static final String one = "1,1,1.0,10";
    public static final String two = "2,2,2.1,20";
    private WurmObjectsFactory factory;

    public static Item createPriceList(String str) {
        return WurmObjectsFactory.getCurrent().createPriceList(str);
    }

    @BeforeEach
    void setUp() throws Throwable {
        factory = new WurmObjectsFactory();
    }

    @Test
    void testNewPriceListHasInscription() throws NoSuchTemplateException, FailedException {
        assertNotNull(PriceList.getNewPriceList().getInscription());
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
        assertEquals(priceList.new Entry(1, (byte)1, 1.0f, 10).toString(), priceList.iterator().next().toString());
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
    void testAddingAboveMaxSize() {
        StringBuilder stringBuilder = new StringBuilder(512);
        for (int i = 0; i <= 49; ++i)
            stringBuilder.append(one);
        String str = stringBuilder.toString();
        assert str.length() <= 500;
        PriceList priceList = new PriceList(createPriceList(str));
        assertThrows(PriceList.PriceListFullException.class, () -> priceList.addItem(1, (byte)1, 1.0f, 10));
    }

    @Test
    void testPriceTestIterator() {
        List<String> allItems = Arrays.asList(one, two, one + 100, two + 200);
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
        priceList.iterator().next().updateItem(2, (byte)2, 2, 10000000);
        assertEquals(oldString, priceList.toString());
    }

    @Test
    void testUpdateItemQLAndPrice () throws PriceList.PriceListFullException {
        PriceList priceList = new PriceList(createPriceList(one));
        String oldString = priceList.toString();
        priceList.iterator().next().updateItemQLAndPrice(2, 10000000);
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
        assertThrows(PriceList.PriceListFullException.class, () -> priceList.iterator().next().updateItem(2, (byte)2, 2, 10000000));
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

        item.updateItem(item.template, item.material, 101, item.price);
        assertEquals(ql, item.minQL);
        item.updateItem(item.template, item.material, -0.1f, item.price);
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
}
