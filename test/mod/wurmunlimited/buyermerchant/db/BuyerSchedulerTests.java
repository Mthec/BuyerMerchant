package mod.wurmunlimited.buyermerchant.db;

import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.Materials;
import mod.wurmunlimited.WurmObjectsFactory;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.ItemDetails;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class BuyerSchedulerTests extends WurmTradingTest {
    private ItemTemplate template = null;
    private static final byte material = Materials.MATERIAL_COTTON;
    private static final int weight = 1234;
    private static final int price = 1234567;
    private static final float minQL = 34f;
    private static final int remainingToPurchase = 1;
    private static final int minimumPurchase = 12;
    private static final boolean acceptsDamage = true;
    private static final int intervalHours = 1;

    @BeforeEach
    @Override
    protected void setUp() throws Throwable {
        super.setUp();
        if (template == null) {
            template = ItemTemplateFactory.getInstance().getTemplate(ItemList.pickAxe);
        }
    }

    private BuyerScheduler.Update createUpdate() {
        try {
            BuyerScheduler.addUpdateFor(buyer, template, material, weight, minQL, price, remainingToPurchase, minimumPurchase, acceptsDamage, intervalHours);
        } catch (SQLException | BuyerScheduler.UpdateAlreadyExists e) {
            throw new RuntimeException(e);
        }

        return BuyerScheduler.getUpdatesFor(buyer)[0];
    }

    @Test
    void testAddUpdateFor() throws BuyerScheduler.UpdateAlreadyExists, SQLException {
        BuyerScheduler.addUpdateFor(buyer, template, material, weight, minQL, price, remainingToPurchase, minimumPurchase, acceptsDamage, 1);
        BuyerScheduler.Update[] updates = BuyerScheduler.getUpdatesFor(buyer);

        assertEquals(1, updates.length);
        BuyerScheduler.Update update = updates[0];
        assertEquals(template, update.template);
        assertEquals(material, update.material);
        assertEquals(weight, update.weight);
        assertEquals(price, update.price);
        assertEquals(minQL, update.minQL);
        assertEquals(remainingToPurchase, update.remainingToPurchase);
        assertEquals(minimumPurchase, update.minimumPurchase);
        assertEquals(acceptsDamage, update.acceptsDamaged);
        assertEquals(intervalHours * TimeConstants.HOUR_MILLIS, update.interval);
    }

    @Test
    void testAddUpdateForAlreadyExists() throws BuyerScheduler.UpdateAlreadyExists, SQLException {
        BuyerScheduler.addUpdateFor(buyer, template, material, weight, minQL, price, remainingToPurchase, minimumPurchase, acceptsDamage, 1);
        assert BuyerScheduler.getUpdatesFor(buyer).length == 1;

        assertThrows(BuyerScheduler.UpdateAlreadyExists.class, () -> BuyerScheduler.addUpdateFor(buyer, template, material, weight, minQL, price, remainingToPurchase, minimumPurchase, acceptsDamage, 1));
    }

    @Test
    void testSetIntervalFor() throws SQLException {
        int newInterval = 123;
        BuyerScheduler.Update update = createUpdate();
        assert update.interval == intervalHours * TimeConstants.HOUR_MILLIS;

        BuyerScheduler.setIntervalFor(update, newInterval);
        assertEquals(newInterval, update.interval);
        assertEquals(newInterval, BuyerScheduler.getUpdatesFor(buyer)[0].interval);
    }

    @Test
    void testDeleteUpdateFor() throws PriceList.NoPriceListOnBuyer, BuyerScheduler.UpdateAlreadyExists, SQLException {
        BuyerScheduler.Update update = createUpdate();
        BuyerScheduler.addUpdateFor(buyer, template, material, weight + 10, minQL, price, remainingToPurchase, minimumPurchase, acceptsDamage, intervalHours);
        BuyerScheduler.updateBuyer(buyer);
        assert PriceList.getPriceListFromBuyer(buyer).asArray().length == 2;

        BuyerScheduler.deleteUpdateFor(buyer, update.id);
        assertEquals(1, BuyerScheduler.getUpdatesFor(buyer).length);
        assertEquals(1, PriceList.getPriceListFromBuyer(buyer).asArray().length);
    }

    @Test
    void testUpdateUpdateDetails() throws PriceList.NoPriceListOnBuyer, BuyerScheduler.UpdateAlreadyExists, SQLException {
        int intervalHours = 10;
        BuyerScheduler.Update update = createUpdate();
        assert update.getIntervalHours() != intervalHours;
        BuyerScheduler.updateBuyer(buyer);
        assert PriceList.getPriceListFromBuyer(buyer).asArray().length == 1;

        int newWeight = weight * 2;
        int newPrice = price * 2;
        float newMinQL = minQL * 2;
        int newRemainingToPurchase = remainingToPurchase * 3;
        int newMinimumPurchase = minimumPurchase * 3;
        boolean newAcceptsDamage = !acceptsDamage;
        ItemDetails details = new ItemDetails(newWeight, newMinQL, newPrice, newRemainingToPurchase, newMinimumPurchase, newAcceptsDamage);

        BuyerScheduler.updateUpdateDetails(buyer, update, details, intervalHours);
        assertEquals(1, BuyerScheduler.getUpdatesFor(buyer).length);
        PriceList.Entry[] entries = PriceList.getPriceListFromBuyer(buyer).asArray();
        assertEquals(template, update.template);
        assertEquals(material, update.material);
        assertEquals(newWeight, update.weight);
        assertEquals(newPrice, update.price);
        assertEquals(newMinQL, update.minQL);
        assertEquals(newRemainingToPurchase, update.remainingToPurchase);
        assertEquals(newMinimumPurchase, update.minimumPurchase);
        assertEquals(newAcceptsDamage, update.acceptsDamaged);
        assertEquals(intervalHours * TimeConstants.HOUR_MILLIS, update.interval);
        assertEquals(1, entries.length);
        assertEquals(template.getTemplateId(), entries[0].getTemplateId());
        assertEquals(material, entries[0].getMaterial());
        assertEquals(newWeight, entries[0].getWeight());
        assertEquals(newPrice, entries[0].getPrice());
        assertEquals(newMinQL, entries[0].getQualityLevel());
        assertEquals(newRemainingToPurchase, entries[0].getRemainingToPurchase());
        assertEquals(newMinimumPurchase, entries[0].getMinimumPurchase());
        assertEquals(newAcceptsDamage, entries[0].acceptsDamaged());
    }

    @Test
    void testUpdateUpdateDetailsOnlyPrice() throws PriceList.NoPriceListOnBuyer, BuyerScheduler.UpdateAlreadyExists, SQLException {
        BuyerScheduler.Update update = createUpdate();
        BuyerScheduler.updateBuyer(buyer);
        assert PriceList.getPriceListFromBuyer(buyer).asArray().length == 1;

        int newPrice = price * 2;
        ItemDetails details = new ItemDetails(update.weight, update.minQL, newPrice, update.remainingToPurchase, update.minimumPurchase, update.acceptsDamaged);

        BuyerScheduler.updateUpdateDetails(buyer, update, details, update.getIntervalHours());
        assertEquals(1, BuyerScheduler.getUpdatesFor(buyer).length);
        PriceList.Entry[] entries = PriceList.getPriceListFromBuyer(buyer).asArray();
        assertEquals(newPrice, update.price);
        assertEquals(1, entries.length);
        assertEquals(newPrice, entries[0].getPrice());
    }

    @Test
    void updateBuyer() throws NoSuchFieldException, PriceList.NoPriceListOnBuyer, PriceList.PageNotAdded, PriceList.PriceListFullException {
        BuyerScheduler.Update update = createUpdate();
        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        WurmObjectsFactory.setFinalField(null, BuyerScheduler.class.getDeclaredField("clock"), clock);

        assert PriceList.getPriceListFromBuyer(buyer).asArray().length == 0;
        BuyerScheduler.updateBuyer(buyer);

        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        assertEquals(1, priceList.asArray().length);
        PriceList.Entry entry = priceList.asArray()[0];
        assertEquals(remainingToPurchase, entry.getRemainingToPurchase());

        entry.subtractRemainingToPurchase(1);
        assertEquals(0, PriceList.getPriceListFromBuyer(buyer).asArray().length);

        WurmObjectsFactory.setFinalField(null, BuyerScheduler.class.getDeclaredField("clock"), Clock.fixed(clock.instant().plusMillis(update.interval + 1), clock.getZone()));
        BuyerScheduler.updateBuyer(buyer);

        priceList = PriceList.getPriceListFromBuyer(buyer);
        assertEquals(1, priceList.asArray().length);
        entry = priceList.asArray()[0];
        assertEquals(remainingToPurchase, entry.getRemainingToPurchase());
    }

    @Test
    void testLoadAll() throws NoSuchFieldException, IllegalAccessException, SQLException {
        BuyerScheduler.Update update = createUpdate();
        assert BuyerScheduler.getUpdatesFor(buyer).length == 1;
        BuyerScheduler.setFreeMoneyFor(buyer, true);
        BuyerScheduler.setDestroyBoughtItemsFor(buyer, false);

        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("toUpdate")).clear();
        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("freeMoney")).clear();
        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("destroyBoughtItems")).clear();

        BuyerScheduler.loadAll();

        assertEquals(1, BuyerScheduler.getUpdatesFor(buyer).length);
        assertEquals(update, BuyerScheduler.getUpdatesFor(buyer)[0]);

        assertTrue(Objects.requireNonNull(BuyerScheduler.getIsFreeMoneyFor(buyer)));
        assertFalse(Objects.requireNonNull(BuyerScheduler.getIsDestroyBoughtItemsFor(buyer)));
    }

    @Test
    void testGetNextUpdateFor() throws BuyerScheduler.UpdateAlreadyExists, SQLException {
        BuyerScheduler.addUpdateFor(buyer, template, material, weight, minQL, price, remainingToPurchase, minimumPurchase, acceptsDamage, 1);
        BuyerScheduler.Update[] updates = BuyerScheduler.getUpdatesFor(buyer);
        assert updates.length == 1;
        updates[0].lastUpdated = Clock.systemUTC().millis();

        assertEquals(TimeConstants.HOUR_MILLIS, BuyerScheduler.getNextUpdateFor(buyer));
    }

    @Test
    void testGetNextUpdateForNoUpdates() {
        assert BuyerScheduler.getUpdatesFor(buyer).length == 0;

        assertEquals(-1L, BuyerScheduler.getNextUpdateFor(buyer));
    }

    @Test
    void testSetFreeMoneyNullToTrue() throws SQLException, NoSuchFieldException, IllegalAccessException {
        assert BuyerScheduler.getIsFreeMoneyFor(buyer) == null;

        BuyerScheduler.setFreeMoneyFor(buyer, true);
        assertTrue(Objects.requireNonNull(BuyerScheduler.getIsFreeMoneyFor(buyer)));

        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("freeMoney")).clear();
        BuyerScheduler.loadAll();
        assertTrue(Objects.requireNonNull(BuyerScheduler.getIsFreeMoneyFor(buyer)));
    }

    @Test
    void testSetFreeMoneyTrueToFalse() throws SQLException, NoSuchFieldException, IllegalAccessException {
        BuyerScheduler.setFreeMoneyFor(buyer, true);

        BuyerScheduler.setFreeMoneyFor(buyer, false);
        assertFalse(Objects.requireNonNull(BuyerScheduler.getIsFreeMoneyFor(buyer)));

        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("freeMoney")).clear();
        BuyerScheduler.loadAll();
        assertFalse(Objects.requireNonNull(BuyerScheduler.getIsFreeMoneyFor(buyer)));
    }

    @Test
    void testSetFreeMoneyFalseToNull() throws SQLException, NoSuchFieldException, IllegalAccessException {
        BuyerScheduler.setFreeMoneyFor(buyer, false);

        BuyerScheduler.setFreeMoneyFor(buyer, null);
        assertNull(BuyerScheduler.getIsFreeMoneyFor(buyer));

        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("freeMoney")).clear();
        BuyerScheduler.loadAll();
        assertNull(BuyerScheduler.getIsFreeMoneyFor(buyer));
    }

    @Test
    void testSetDestroyBoughtItemsNullToTrue() throws SQLException, NoSuchFieldException, IllegalAccessException {
        assert BuyerScheduler.getIsDestroyBoughtItemsFor(buyer) == null;

        BuyerScheduler.setDestroyBoughtItemsFor(buyer, true);
        assertTrue(Objects.requireNonNull(BuyerScheduler.getIsDestroyBoughtItemsFor(buyer)));

        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("destroyBoughtItems")).clear();
        BuyerScheduler.loadAll();
        assertTrue(Objects.requireNonNull(BuyerScheduler.getIsDestroyBoughtItemsFor(buyer)));
    }

    @Test
    void testSetDestroyBoughtItemsTrueToFalse() throws SQLException, NoSuchFieldException, IllegalAccessException {
        BuyerScheduler.setDestroyBoughtItemsFor(buyer, true);

        BuyerScheduler.setDestroyBoughtItemsFor(buyer, false);
        assertFalse(Objects.requireNonNull(BuyerScheduler.getIsDestroyBoughtItemsFor(buyer)));

        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("destroyBoughtItems")).clear();
        BuyerScheduler.loadAll();
        assertFalse(Objects.requireNonNull(BuyerScheduler.getIsDestroyBoughtItemsFor(buyer)));
    }

    @Test
    void testSetDestroyBoughtItemsFalseToNull() throws SQLException, NoSuchFieldException, IllegalAccessException {
        BuyerScheduler.setDestroyBoughtItemsFor(buyer, false);

        BuyerScheduler.setDestroyBoughtItemsFor(buyer, null);
        assertNull(BuyerScheduler.getIsDestroyBoughtItemsFor(buyer));

        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("destroyBoughtItems")).clear();
        BuyerScheduler.loadAll();
        assertNull(BuyerScheduler.getIsDestroyBoughtItemsFor(buyer));
    }
}
