package mod.wurmunlimited.buyermerchant.db;

import com.wurmonline.server.Constants;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import mod.wurmunlimited.buyermerchant.ItemDetails;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
public class BuyerScheduler {
    public static class Update implements Comparable<Update> {
        private static int ids = 0;
        public final int id;
        long lastUpdated;
        long interval;

        public final ItemTemplate template;
        public final byte material;
        int weight;
        float minQL;
        int price;
        int remainingToPurchase;
        int minimumPurchase;
        boolean acceptsDamaged;
        private final String name;

        private Update(int id, ItemTemplate template, byte material, int weight, float minQL, int price, int remainingToPurchase, int minimumPurchase, boolean acceptsDamaged, long interval, long lastUpdated) {
            this.id = id;
            if (id > ids) {
                ids = id + 1;
            }
            this.template = template;
            this.material = material;
            this.weight = weight;
            this.minQL = minQL;
            this.price = price;
            this.remainingToPurchase = remainingToPurchase;
            this.minimumPurchase = minimumPurchase;
            this.acceptsDamaged = acceptsDamaged;
            this.interval = interval;
            this.name = ItemFactory.generateName(template, material);
            this.lastUpdated = lastUpdated;
        }

        private Update(ItemTemplate template, byte material, int weight, float minQL, int price, int remainingToPurchase, int minimumPurchase, boolean acceptsDamaged, long interval) {
            this(++ids, template, material, weight, minQL, price, remainingToPurchase, minimumPurchase, acceptsDamaged, interval, 0);
        }

        public int getWeight() {
            if (weight == -1) {
                return template.getWeightGrams();
            } else {
                return weight;
            }
        }

        public float getMinQL() {
            return minQL;
        }

        public int getPrice() {
            return price;
        }

        public int getRemainingToPurchase() {
            return remainingToPurchase;
        }

        public int getMinimumPurchase() {
            return minimumPurchase;
        }

        public boolean getAcceptsDamaged() {
            return acceptsDamaged;
        }

        public int getIntervalHours() {
            return (int)(interval / TimeConstants.HOUR_MILLIS);
        }
        
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Update)) {
                return false;
            }
            Update u = (Update)other;
            return u.template.getTemplateId() == template.getTemplateId() &&
                    u.material == material && 
                    u.weight == weight && 
                    Float.compare(u.minQL, minQL) == 0 &&
                    u.price == price &&
                    u.remainingToPurchase == remainingToPurchase &&
                    u.minimumPurchase == minimumPurchase &&
                    u.acceptsDamaged == acceptsDamaged &&
                    u.interval == interval;
        }

        /**
         * @param other Another update.
         * @return Compared on template name, then minQL descending order.
         */
        @SuppressWarnings("DuplicatedCode")
        @Override
        public int compareTo(@NotNull Update other) {
            int compare = name.compareTo(ItemFactory.generateName(other.template, other.material));
            if (compare == 0) {
                compare = Integer.compare(other.weight, weight);
            }
            if (compare == 0) {
                compare = Float.compare(other.minQL, minQL);
            }
            if (compare == 0) {
                compare = Integer.compare(other.remainingToPurchase, remainingToPurchase);
            }
            if (compare == 0) {
                compare = Integer.compare(other.minimumPurchase, minimumPurchase);
            }
            if (compare == 0) {
                compare = Boolean.compare(other.acceptsDamaged, acceptsDamaged);
            }

            return compare;
        }
        
        boolean matches(ItemTemplate otherTemplate, byte otherMaterial, ItemDetails details) {
            return otherTemplate == template && otherMaterial == material && details.weight == weight &&
                           Float.compare(details.minQL, minQL) == 0 && details.remainingToPurchase == remainingToPurchase &&
                           details.minimumPurchase == minimumPurchase && details.acceptsDamaged == acceptsDamaged;
        }
    }
    
    public static class UpdateAlreadyExists extends Exception {}

    private static final Logger logger = Logger.getLogger(BuyerScheduler.class.getName());
    private static final long fiveMinutes = TimeConstants.MINUTE_MILLIS * 5;
    private static final String dbName = "buyer";
    private static String dbString = "";
    private static boolean created = false;
    @SuppressWarnings("FieldMayBeFinal")
    private static Clock clock = Clock.systemUTC();
    private static final Map<Creature, List<Update>> toUpdate = new HashMap<>();
    private static final Map<Creature, Long> lastChecked = new HashMap<>();

    public interface Execute {
        void run(Connection db) throws SQLException;
    }

    private static void init(Connection conn) throws SQLException {
        conn.prepareStatement("CREATE TABLE IF NOT EXISTS updates (" +
                                                             "buyerId INTEGER NOT NULL," +
                                                             "id INTEGER NOT NULL UNIQUE," +
                                                             "templateId INTEGER NOT NULL," +
                                                             "material INTEGER NOT NULL," +
                                                             "weight INTEGER NOT NULL," +
                                                             "minQL REAL NOT NULL," +
                                                             "price INTEGER NOT NULL," +
                                                             "remainingToPurchase INTEGER NOT NULL," +
                                                             "minimumPurchase INTEGER NOT NULL," +
                                                             "acceptsDamaged INTEGER NOT NULL," +
                                                             "interval INTEGER NOT NULL," +
                                                             "lastUpdated INTEGER NOT NULL" +
                                                             ");").executeUpdate();

        created = true;
    }

    private static void execute(Execute execute) throws SQLException {
        Connection db = null;
        try {
            if (dbString.isEmpty())
                dbString = "jdbc:sqlite:" + Constants.dbHost + "/sqlite/" + dbName + ".db";
            db = DriverManager.getConnection(dbString);
            if (!created) {
                init(db);
            }
            execute.run(db);
        } finally {
            try {
                if (db != null)
                    db.close();
            } catch (SQLException e1) {
                logger.warning("Could not close connection to database.");
                e1.printStackTrace();
            }
        }
    }

    private static Optional<PriceList.Entry> findEntry(PriceList priceList, Update update) {
        return priceList.stream().filter(
                it -> it.getTemplateId() == update.template.getTemplateId() &&
                              it.getMaterial() == update.material &&
                              it.getWeight() == update.weight &&
                              Float.compare(it.getQualityLevel(), update.minQL) == 0 &&
                              it.getPrice() == update.price &&
                              it.getMinimumPurchase() == update.minimumPurchase &&
                              it.acceptsDamaged() == update.acceptsDamaged

        ).findAny();
    }

    public static Update[] getUpdatesFor(Creature buyer) {
        List<Update> toReturn = toUpdate.get(buyer);
        if (toReturn == null)
            return new Update[0];
        return toReturn.toArray(new Update[0]);
    }

    public static long getNextUpdateFor(Creature buyer) {
        List<Update> toReturn = toUpdate.get(buyer);
        if (toReturn == null)
            return -1L;
        long now = clock.millis();
        return toReturn.stream().mapToLong(u -> u.interval - now + u.lastUpdated).min().orElse(-1L);
    }
    
    public static void addUpdateFor(Creature buyer, ItemTemplate template, byte material, int weight, float ql, int price, int remainingToPurchase, int minimumPurchase, boolean acceptsDamaged, long intervalHours) throws SQLException, UpdateAlreadyExists {
        ItemDetails details = new ItemDetails(weight, ql, price, remainingToPurchase, minimumPurchase, acceptsDamaged);
        List<Update> currentUpdates = toUpdate.computeIfAbsent(buyer, k -> new ArrayList<>());
        if (currentUpdates.stream().anyMatch(it -> it.matches(template, material, details))) {
            throw new UpdateAlreadyExists();
        }
        long interval = intervalHours * TimeConstants.HOUR_MILLIS;

        Update update = new Update(template, material, weight, ql, price, remainingToPurchase, minimumPurchase, acceptsDamaged, interval);
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("INSERT INTO updates VALUES(?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setLong(1, buyer.getWurmId());
            ps.setInt(2, update.id);
            ps.setInt(3, template.getTemplateId());
            ps.setByte(4, material);
            ps.setInt(5, weight);
            ps.setFloat(6, ql);
            ps.setInt(7, price);
            ps.setInt(8, remainingToPurchase);
            ps.setInt(9, minimumPurchase);
            ps.setBoolean(10, acceptsDamaged);
            ps.setLong(11, interval);
            ps.setLong(12, clock.millis());
            ps.executeUpdate();
        });
        currentUpdates.add(update);

        lastChecked.remove(buyer);
    }

    public static void setIntervalFor(Update update, long newInterval) throws SQLException {
        assert newInterval > 0;
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("UPDATE updates SET interval=? WHERE id=?;");
            ps.setLong(1, newInterval);
            ps.setInt(2, update.id);
            ps.executeUpdate();
        });
        update.interval = newInterval;
    }

    public static void deleteUpdateFor(Creature buyer, int updateId) {
        List<Update> updates = toUpdate.get(buyer);
        if (updates != null) {
            List<Update> toRemove = updates.stream().filter(it -> it.id == updateId).collect(Collectors.toList());
            try {
                PriceList priceList = null;
                try {
                    priceList = PriceList.getPriceListFromBuyer(buyer);
                } catch (PriceList.NoPriceListOnBuyer e) {
                    logger.warning("No PriceList found when deleting update.");
                    e.printStackTrace();
                }
                for (Update update : toRemove) {
                    execute(db -> {
                        PreparedStatement ps = db.prepareStatement("DELETE FROM updates WHERE id=?;");
                        ps.setLong(1, update.id);
                        ps.executeUpdate();
                    });
                    updates.remove(update);

                    if (priceList != null) {
                        Optional<PriceList.Entry> entry = findEntry(priceList, update);
                        if (entry.isPresent()) {
                            priceList.removeItem(entry.get());
                        }
                    }
                }

                if (priceList != null) {
                    priceList.savePriceList();
                }
            } catch (SQLException e) {
                logger.warning("Error when attempting to delete update:");
                e.printStackTrace();
            } catch (PriceList.PageNotAdded | PriceList.PriceListFullException e) {
                logger.warning("Error saving PriceList after update deletion.");
                e.printStackTrace();
            }
        }
    }

    public static void updateUpdateDetails(Creature buyer, Update update, ItemDetails details, int intervalHours) throws SQLException, UpdateAlreadyExists {
        if (update.matches(update.template, update.material, details)) {
            return;
        }
        List<Update> currentUpdates = toUpdate.get(buyer);
        if (currentUpdates != null && currentUpdates.stream().anyMatch(it -> it.id != update.id && it.matches(update.template, update.material, details))) {
            throw new UpdateAlreadyExists();
        }

        long interval = intervalHours * TimeConstants.HOUR_MILLIS;
        long now = clock.millis();
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("UPDATE updates SET weight=?, minQL=?, price=?, remainingToPurchase=?, minimumPurchase=?, acceptsDamaged=?, interval=?, lastUpdated=? WHERE id=?;");
            ps.setInt(1, details.weight);
            ps.setFloat(2, details.minQL);
            ps.setInt(3, details.price);
            ps.setInt(4, details.remainingToPurchase);
            ps.setInt(5, details.minimumPurchase);
            ps.setBoolean(6, details.acceptsDamaged);
            ps.setLong(7, interval);
            ps.setLong(8, now);
            ps.setInt(9, update.id);
            ps.executeUpdate();
        });

        try {
            PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
            Optional<PriceList.Entry> entry = findEntry(priceList, update);
            if (entry.isPresent()) {
                entry.get().updateItemDetails(details.weight, details.minQL, details.price, details.remainingToPurchase, details.minimumPurchase, details.acceptsDamaged);
                priceList.savePriceList();
            }
        } catch (PriceList.PageNotAdded | PriceList.PriceListFullException | PriceList.NoPriceListOnBuyer e) {
            logger.warning("Failed to update PriceList when updating buyer update details.");
            e.printStackTrace();
        }

        update.weight = details.weight;
        update.minQL = details.minQL;
        update.price = details.price;
        update.remainingToPurchase = details.remainingToPurchase;
        update.minimumPurchase = details.minimumPurchase;
        update.acceptsDamaged = details.acceptsDamaged;
        update.interval = interval;
        update.lastUpdated = now;

        if (currentUpdates != null) {
            currentUpdates.sort(Update::compareTo);
        }
    }

    public static void updateBuyer(Creature buyer) {
        long lastUpdate = lastChecked.computeIfAbsent(buyer, k -> 0L);
        long now = clock.millis();
        if (now - lastUpdate < fiveMinutes) {
            return;
        }
        lastChecked.put(buyer, now);

        List<Update> updates = toUpdate.get(buyer);
        if (updates != null && !updates.isEmpty()) {
            try {
                PriceList priceList = PriceList.getPriceListFromBuyer(buyer);

                for (Update update : updates) {
                    if (now - update.lastUpdated > update.interval) {
                        Optional<PriceList.Entry> entry = findEntry(priceList, update);
                        if (entry.isPresent()) {
                            if (entry.get().getRemainingToPurchase() != update.remainingToPurchase) {
                                entry.get().setRemainingToPurchase(update.remainingToPurchase);
                                priceList.savePriceList();
                            }
                        } else {
                            priceList.addItem(update.template.getTemplateId(), update.material, update.weight, update.minQL,
                                    update.price, update.remainingToPurchase, update.minimumPurchase, update.acceptsDamaged);
                            priceList.savePriceList();
                        }

                        update.lastUpdated = now;
                    }
                }
            } catch (PriceList.NoPriceListOnBuyer e) {
                logger.warning("PriceList not found for " + buyer.getName() + "(" + buyer.getWurmId() + ").");
                e.printStackTrace();
            } catch (PriceList.PriceListFullException | PriceList.PageNotAdded ignored) {

            } catch (NoSuchTemplateException e) {
                logger.warning("No such template exists:");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadAll() throws SQLException {
        execute(db -> {
            toUpdate.clear();
            ResultSet rs = db.prepareStatement("SELECT * FROM updates;").executeQuery();
            Map<Long, Creature> creatures = new HashMap<>();
            ItemTemplateFactory factory = ItemTemplateFactory.getInstance();

            while (rs.next()) {
                long buyerId = rs.getLong(1);
                if (!creatures.containsKey(buyerId)) {
                    try {
                        creatures.put(buyerId, Creatures.getInstance().getCreature(buyerId));
                    } catch (NoSuchCreatureException e) {
                        logger.warning("Unknown buyer found (" + buyerId + "):");
                        e.printStackTrace();
                        creatures.put(buyerId, null);
                        continue;
                    }
                }
                Creature buyer = creatures.get(buyerId);

                if (buyer != null) {
                    try {
                        List<Update> updates = toUpdate.computeIfAbsent(buyer, k -> new ArrayList<>());
                        updates.add(new Update(rs.getInt(2),
                                factory.getTemplate(rs.getInt(3)),
                                rs.getByte(4),
                                rs.getInt(5),
                                rs.getFloat(6),
                                rs.getInt(7),
                                rs.getInt(8),
                                rs.getInt(9),
                                rs.getBoolean(10),
                                rs.getLong(11),
                                rs.getLong(12)));
                    } catch (NoSuchTemplateException e) {
                        logger.warning("Unknown ItemTemplate for update (" + rs.getInt(3) + "):");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static void remove(Creature buyer) {
        List<Update> updates = toUpdate.remove(buyer);
        if (updates != null) {
            for (Update update : updates) {
                deleteUpdateFor(buyer, update.id);
            }
        }
    }
}
