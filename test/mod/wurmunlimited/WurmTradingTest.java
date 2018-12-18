package mod.wurmunlimited;

import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.BuyerHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.items.BuyerTrade;
import com.wurmonline.server.items.BuyerTradingWindow;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.items.WurmMail;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.questions.Questions;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.internal.util.reflection.FieldSetter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class WurmTradingTest {

    protected Player player;
    protected Player owner;
    protected Creature buyer;
    protected Trade trade;
    protected WurmObjectsFactory factory;
    protected static final Pattern passthrough = Pattern.compile("passthrough\\{id=\"id\";text=\"([\\d]+)\"}");
    protected static final Pattern defaultOption = Pattern.compile("default=\"([\\d]+)\";");
    protected static final Pattern itemAndMaterial = Pattern.compile("harray\\{label\\{text=\"([\\w\\s]+)\"}};harray\\{label\\{text=\"([\\w\\s]+)\"}};");

    @BeforeEach
    protected void setUp() throws Throwable {
        WurmMail.resetStatic();
        Zones.resetStatic();
        FieldSetter.setField(null, Players.class.getDeclaredField("instance"), null);
        FieldSetter.setField(null, Questions.class.getDeclaredField("questions"), new HashMap<Integer, Question>(10));
        BuyerTradingWindow.freeMoney = false;
        BuyerTradingWindow.destroyBoughtItems = false;
        BuyerHandler.maxPersonalItems = 50;
        factory = new WurmObjectsFactory();
        player = factory.createNewPlayer();
        player.setName("Player");
        owner = factory.createNewPlayer();
        owner.setName("Owner");
        buyer = factory.createNewBuyer(owner);
    }

    protected void makeBuyerTrade() {
        try {
            trade = new BuyerTrade(player, buyer);
            BuyerHandler handler = new BuyerHandler(buyer, trade);
            FieldSetter.setField(buyer, Creature.class.getDeclaredField("tradeHandler"), handler);
        } catch (PriceList.NoPriceListOnBuyer | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        player.setTrade(trade);
        buyer.setTrade(trade);
    }

    protected void makeOwnerBuyerTrade() {
        try {
            trade = new BuyerTrade(owner, buyer);
            BuyerHandler handler = new BuyerHandler(buyer, trade);
            FieldSetter.setField(buyer, Creature.class.getDeclaredField("tradeHandler"), handler);
        } catch (PriceList.NoPriceListOnBuyer | NoSuchFieldException e) {
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
