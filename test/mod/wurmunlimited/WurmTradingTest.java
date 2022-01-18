package mod.wurmunlimited;

import com.wurmonline.server.Constants;
import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.BuyerHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.items.BuyerTrade;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.items.WurmMail;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.questions.Questions;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.buyermerchant.BuyerMerchant;
import mod.wurmunlimited.buyermerchant.PriceList;
import mod.wurmunlimited.buyermerchant.db.BuyerScheduler;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class WurmTradingTest {
    protected Player player;
    protected Player owner;
    protected Creature buyer;
    protected Trade trade;
    protected WurmObjectsFactory factory;
    protected static final Pattern passthrough = Pattern.compile("passthrough\\{id=[\"']id[\"'];text=[\"']([\\d]+)[\"']}");
    protected static final Pattern defaultOption = Pattern.compile("default=\"([\\d]+)\";");
    protected static final Pattern itemAndMaterial = Pattern.compile("harray\\{label\\{text=\"([\\w\\s]+)\"}};harray\\{label\\{text=\"([\\w\\s]+)\"}};");

    @BeforeEach
    protected void setUp() throws Throwable {
        ActionEntryBuilder.init();
        WurmMail.resetStatic();
        Zones.resetStatic();
        ReflectionUtil.setPrivateField(null, Players.class.getDeclaredField("instance"), null);
        ReflectionUtil.setPrivateField(null, Questions.class.getDeclaredField("questions"), new HashMap<Integer, Question>());
        Constants.dbHost = ".";
        ReflectionUtil.<Map<Creature, BuyerScheduler.Update>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("toUpdate")).clear();
        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("freeMoney")).clear();
        ReflectionUtil.<Map<Long, Boolean>>getPrivateField(null, BuyerScheduler.class.getDeclaredField("destroyBoughtItems")).clear();
        ReflectionUtil.setPrivateField(null, BuyerScheduler.class.getDeclaredField("created"), false);
        ReflectionUtil.setPrivateField(null, BuyerScheduler.class.getDeclaredField("clock"), Clock.systemUTC());
        //noinspection ResultOfMethodCallIgnored
        Files.walk(Paths.get("./sqlite/")).filter(it -> !it.toFile().isDirectory()).forEach(it -> it.toFile().delete());
        ReflectionUtil.setPrivateField(null, BuyerMerchant.class.getDeclaredField("freeMoney"), false);
        ReflectionUtil.setPrivateField(null, BuyerMerchant.class.getDeclaredField("destroyBoughtItems"), false);
        BuyerHandler.defaultMaxPersonalItems = 51;
        ReflectionUtil.setPrivateField(null, TradeHandler.class.getDeclaredField("maxPersonalItems"), 50);
        factory = new WurmObjectsFactory();
        player = factory.createNewPlayer();
        player.setName("Player");
        owner = factory.createNewPlayer();
        owner.setName("Owner");
        buyer = factory.createNewBuyer(owner);
    }

    @AfterEach
    void deleteTraderLogs() {
        File folder = new File(".");
        File[] files = folder.listFiles((file, name) -> name.matches("trader[\\d]+\\.log"));

        if (files != null) {
            for (File file : files) {
                file.deleteOnExit();
            }
        }
    }

    protected void makeBuyerTrade() {
        try {
            trade = new BuyerTrade(player, buyer);
            BuyerHandler handler = new BuyerHandler(buyer, trade);
            ReflectionUtil.setPrivateField(buyer, Creature.class.getDeclaredField("tradeHandler"), handler);
        } catch (PriceList.NoPriceListOnBuyer | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        player.setTrade(trade);
        buyer.setTrade(trade);
    }

    protected void makeOwnerBuyerTrade() {
        try {
            trade = new BuyerTrade(owner, buyer);
            BuyerHandler handler = new BuyerHandler(buyer, trade);
            ReflectionUtil.setPrivateField(buyer, Creature.class.getDeclaredField("tradeHandler"), handler);
        } catch (PriceList.NoPriceListOnBuyer | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        owner.setTrade(trade);
        buyer.setTrade(trade);
    }

    protected void balance() {
        try {
            Method balance = TradeHandler.class.getDeclaredMethod("balance");
            balance.setAccessible(true);
            balance.invoke(buyer.getTradeHandler());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    protected void setSatisfied(Creature creature) {
        trade.setSatisfied(creature, true, trade.getCurrentCounter());
    }

    protected static int getPassThroughId(String bml) {
        Matcher match = passthrough.matcher(bml);
        if (match.find())
            return Integer.parseInt(match.group(1));
        throw new RuntimeException("Pass through value not found.");
    }

    protected static String removePassThrough(String bml) {
        return passthrough.matcher(bml).replaceAll("");
    }

    protected static String removePassThroughAndDefault(String bml) {
        return removePassThrough(defaultOption.matcher(bml).replaceAll(""));
    }
}
