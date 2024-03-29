package com.wurmonline.server.questions;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.creatures.FakeCreatureStatus;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.WurmMail;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.BuyerMerchant;
import mod.wurmunlimited.buyermerchant.db.BuyerScheduler;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BuyerManagementQuestionTest extends WurmTradingTest {
    private Item contract;
    private Item placedContract;
    private Properties answers;
    private BuyerManagementQuestion question;
    private final String name = "George";

    @Override
    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        contract = factory.createBuyerContract();
        owner.getInventory().insertItem(contract);
        placedContract = factory.createBuyerContract();
        placedContract.setData(buyer.getWurmId());
        owner.getInventory().insertItem(placedContract);
        Zones.marketStall = factory.createMarketStallAtCreature(owner);
        Zones.creature = null;
    }

    @BeforeEach
    void createNewAnswers() {
        answers = new Properties();
    }

    private void askManageQuestion() {
        question = new BuyerManagementQuestion(owner, placedContract.getWurmId());
        question.sendQuestion();
    }

    private void askQuestion() {
        question = new BuyerManagementQuestion(owner, contract.getWurmId());
        question.sendQuestion();
    }

    private void placeBuyer() {
        askQuestion();
        answers.setProperty("ptradername", name);
        answers.setProperty("gender", "male");
        question.answer(answers);
    }

    @Test
    void testCorrectButtonsAdded() {
        askManageQuestion();
        String bml = factory.getCommunicator(owner).lastBmlContent;
        assertTrue(bml.contains("button{text='Confirm';id='confirm'}"), bml);
        assertTrue(bml.contains("button{text='Manage Prices';id='" + buyer.getWurmId() + "manage'}"), bml);
        assertTrue(bml.contains("button{text='Add Item To List';id='add'}"), bml);
        assertTrue(bml.contains("button{text='Schedule';id='schedule'}"), bml);
    }

    @Test
    void testBuyerContractOpensWindow() {
        askQuestion();
        assertNotEquals(FakeCommunicator.empty, factory.getCommunicator(owner).lastBmlContent);
    }

    @Test
    void testDismissingGivesDifferentBml() {
        askQuestion();
        new BuyerManagementQuestion(owner, buyer).sendQuestion();
        String[] bml = factory.getCommunicator(owner).getBml();
        assertNotEquals(bml[0], bml[1]);
    }

    @Test
    void testBuyerNameIsSet() {
        placeBuyer();

        assertDoesNotThrow(() -> factory.getCreature("Buyer_" + name));
    }

    @Test
    void testIllegalNames() {
        String maleName = "*George";
        String femaleName = "?Georgette";

        askQuestion();
        answers.setProperty("ptradername", maleName);
        answers.setProperty("gender", "male");
        question.answer(answers);

        askQuestion();
        createNewAnswers();
        answers.setProperty("ptradername", femaleName);
        answers.setProperty("gender", "female");
        question.answer(answers);

        assertThrows(NoSuchCreatureException.class, () -> factory.getCreature("Buyer_" + maleName));
        assertThrows(NoSuchCreatureException.class, () -> factory.getCreature("Buyer_" + femaleName));

        Collection<Creature> creatures = factory.getAllCreatures();
        final int[] buyerCount = {0};
        creatures.forEach(creature -> {
            if (creature.getName().startsWith("Buyer_"))
                ++buyerCount[0];
        });

        assertEquals(3, buyerCount[0]);

        assertThat(owner, receivedMessageContaining(" he chose another"));
        assertThat(owner, receivedMessageContaining(" she chose another"));
    }

    @Test
    void testContractDataIsSet() throws NoSuchCreatureException {
        placeBuyer();

        assertEquals(contract.getData(), factory.getCreature("Buyer_" + name).getWurmId());
    }

    @Test
    void testOtherCreaturesBlock() {
        Zones.creature = buyer;
        placeBuyer();

        assertTrue(factory.getCommunicator(owner).lastBmlContent.contains("no other creatures"));
        assertThrows(NoSuchCreatureException.class, () -> factory.getCreature("Buyer_" + name));
    }

    @Test
    void testNameChange() throws NoSuchCreatureException {
        placeBuyer();
        Creature buyer = factory.getCreature("Buyer_" + name);
        assert buyer != null;
        String newName = "Bob";

        question = new BuyerManagementQuestion(owner, contract.getWurmId());
        question.sendQuestion();
        answers.setProperty("ptradername", newName);
        answers.setProperty("confirm", "true");
        question.answer(answers);

        assertEquals("Buyer_" + newName, factory.getCreature(contract.getData()).getName());
        assertEquals("Buyer_" + newName, ((FakeCreatureStatus)buyer.getStatus()).savedCreatureName);
    }

    @Test
    void testNameChangeNotSentOnSameName() throws NoSuchCreatureException {
        placeBuyer();
        Creature buyer = factory.getCreature("Buyer_" + name);
        assert buyer != null;

        question = new BuyerManagementQuestion(owner, contract.getWurmId());
        question.sendQuestion();
        answers.setProperty("ptradername", name);
        answers.setProperty("confirm", "true");
        question.answer(answers);

        assertEquals("Buyer_" + name, factory.getCreature(contract.getData()).getName());
        assertEquals("UNSET", ((FakeCreatureStatus)buyer.getStatus()).savedCreatureName);
    }

    @Test
    void testOpensSetBuyerPricesQuestion() {
        askManageQuestion();
        answers.setProperty(buyer.getWurmId() + "manage", "true");
        question.answer(answers);

        new SetBuyerPricesQuestion(owner, buyer.getWurmId()).sendQuestion();

        String[] bml = factory.getCommunicator(owner).getBml();
        assertEquals(removePassThrough(bml[2]), removePassThrough(bml[1]));
    }

    @Test
    void testOpensAddItemToBuyerQuestion() {
        askManageQuestion();
        answers.setProperty("add", "true");
        question.answer(answers);

        new AddItemToBuyerInstantQuestion(owner, buyer.getWurmId()).sendQuestion();

        String[] bml = factory.getCommunicator(owner).getBml();
        assertEquals(removePassThrough(bml[2]), removePassThrough(bml[1]));
    }

    @Test
    void testOpensSchedule() {
        askManageQuestion();
        answers.setProperty("schedule", "true");
        question.answer(answers);

        new UpdateScheduleQuestion(owner, buyer).sendQuestion();

        String[] bml = factory.getCommunicator(owner).getBml();
        assertEquals(removePassThrough(bml[2]), removePassThrough(bml[1]));
    }

    @Test
    void testBuyerDismissed() {
        askManageQuestion();
        answers.setProperty(buyer.getWurmId() + "dismiss", "true");
        question.answer(answers);

        assertTrue(factory.getCommunicator(owner).lastNormalServerMessage.startsWith("You dismiss"));
        assertThrows(NoSuchCreatureException.class, () -> factory.getCreature(buyer.getWurmId()));
        assertTrue(buyer.isDead());
    }

    @Test
    void testBuyerMayorDismissed() {
        question = new BuyerManagementQuestion(player, buyer);
        question.sendQuestion();
        answers.setProperty("dism", "true");
        question.answer(answers);

        assertTrue(factory.getCommunicator(player).lastNormalServerMessage.startsWith("You dismiss"));
        assertThrows(NoSuchCreatureException.class, () -> factory.getCreature(buyer.getWurmId()));
        assertTrue(buyer.isDead());
    }

    @Test
    void testBuyerMayorDismissedChangeMind() {
        question = new BuyerManagementQuestion(player, buyer);
        question.sendQuestion();
        answers.setProperty("dism", "false");
        question.answer(answers);

        assertTrue(factory.getCommunicator(player).lastNormalServerMessage.startsWith("You decide not to dismiss"));
        assertDoesNotThrow(() -> factory.getCreature(buyer.getWurmId()));
        assertFalse(buyer.isDead());
    }

    @Test
    void testBuyerRemoteDismissed() {
        BuyerManagementQuestion.dismissMerchant(null, buyer.getWurmId());

        assertThrows(NoSuchCreatureException.class, () -> factory.getCreature(buyer.getWurmId()));
        assertTrue(buyer.isDead());
    }

    @Test
    void testItemsSentInMailOnForceDismiss() {
        Item item = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(item);

        BuyerManagementQuestion.dismissMerchant(player, buyer.getWurmId());
        assertEquals(1, WurmMail.allMail.size());
        WurmMail mail = WurmMail.allMail.get(0);
        Item backpackMaybe = assertDoesNotThrow(() -> Items.getItem(mail.itemId));

        assertEquals(owner.getWurmId(), mail.ownerId);
        Iterator<Item> backpackItems = backpackMaybe.getItems().iterator();
        Item coin = backpackItems.next();
        assertEquals(item, coin.isCoin() ? coin : backpackItems.next());
    }

    @Test
    void testPriceListLosesNoDecayOnForceDismiss() {
        assert buyer.getInventory().getFirstContainedItem().hasNoDecay();
        BuyerManagementQuestion.dismissMerchant(player, buyer.getWurmId());
        assertEquals(1, WurmMail.allMail.size());
        WurmMail mail = WurmMail.allMail.get(0);
        Item backpackMaybe = assertDoesNotThrow(() -> Items.getItem(mail.itemId));

        assertEquals(owner.getWurmId(), mail.ownerId);
        Item priceList = backpackMaybe.getItems().iterator().next();
        assertFalse(priceList.hasNoDecay());
    }

    @Test
    void testLargeItemsIndividuallySentInMailOnForceDismiss() {
        Item item = factory.createNewCopperCoin();
        buyer.getInventory().insertItem(item);
        Item item1 = factory.createNewItem();
        buyer.getInventory().insertItem(item1);
        Item item2 = factory.createNewItem();
        buyer.getInventory().insertItem(item2);

        BuyerManagementQuestion.dismissMerchant(player, buyer.getWurmId());
        assertEquals(3, WurmMail.allMail.size());

        Set<Item> items = new HashSet<>(3);
        for (WurmMail mail : WurmMail.allMail) {
            items.add(factory.getItem(mail.itemId));
            assertEquals(owner.getWurmId(), mail.ownerId);
        }

        assertTrue(items.contains(item1));
        assertTrue(items.contains(item2));
    }

    @Test
    void testNoLongerOwnsContract() {
        assert contract.getOwnerId() == owner.getWurmId();
        contract.setOwnerId(player.getWurmId());
        assert contract.getOwnerId() != owner.getWurmId();
        placeBuyer();

        assertThat(owner, receivedMessageContaining("no longer in possession"));
    }

    @Test
    void testAddItemTargetSetCorrectlyByBuyerManagement() throws NoSuchQuestionException {
        askManageQuestion();
        answers.setProperty("add", "true");
        question.answer(answers);
        AddItemToBuyerQuestion add = (AddItemToBuyerQuestion)Questions.getQuestion(getPassThroughId(factory.getCommunicator(owner).lastBmlContent));
        assertEquals(buyer.getWurmId(), add.getTarget());
    }

    @Test
    void testSetIsFreeMoneyTrue() throws IOException {
        assert !BuyerMerchant.isFreeMoney(buyer);

        owner.setPower((byte)2);
        Properties properties = new Properties();
        properties.setProperty("free_money", "y");
        askManageQuestion();
        question.answer(properties);

        assertTrue(BuyerMerchant.isFreeMoney(buyer));
    }

    @Test
    void testSetIsFreeMoneyFalse() throws NoSuchFieldException, IllegalAccessException, IOException {
        ReflectionUtil.setPrivateField(null, BuyerMerchant.class.getDeclaredField("freeMoney"), true);
        assert BuyerMerchant.isFreeMoney(buyer);

        owner.setPower((byte)2);
        Properties properties = new Properties();
        properties.setProperty("free_money", "n");
        askManageQuestion();
        question.answer(properties);

        assertFalse(BuyerMerchant.isFreeMoney(buyer));
    }

    @Test
    void testSetIsFreeMoneyDefault() throws SQLException, IOException {
        BuyerScheduler.setFreeMoneyFor(buyer, true);

        owner.setPower((byte)2);
        Properties properties = new Properties();
        properties.setProperty("free_money", "d");
        askManageQuestion();
        question.answer(properties);

        assertNull(BuyerScheduler.getIsFreeMoneyFor(buyer));
    }

    @Test
    void testSetIsFreeMoneyTrueNotGM() {
        assert !BuyerMerchant.isFreeMoney(buyer);
        assert owner.getPower() < 2;

        Properties properties = new Properties();
        properties.setProperty("free_money", "y");
        askManageQuestion();
        question.answer(properties);

        assertFalse(BuyerMerchant.isFreeMoney(buyer));
    }

    @Test
    void testSetIsFreeMoneyTrueBuyerNotCreated() throws IOException {
        assert !BuyerMerchant.isFreeMoney(buyer);

        owner.setPower((byte)2);
        Creature blocker = factory.createNewCreature();
        Zones.creature = blocker;
        blocker.getStatus().setPosition(owner.getStatus().getPosition());
        Properties properties = new Properties();
        properties.setProperty("free_money", "y");
        properties.setProperty("ptradername", "Bob");
        properties.setProperty("gender", "male");
        askQuestion();
        question.answer(properties);

        assertEquals(-1, contract.getData());
    }

    @Test
    void testSetIsDestroyBoughtItemsTrue() throws IOException {
        assert !BuyerMerchant.isDestroyBoughtItems(buyer);

        owner.setPower((byte)2);
        Properties properties = new Properties();
        properties.setProperty("destroy_items", "y");
        askManageQuestion();
        question.answer(properties);

        assertTrue(BuyerMerchant.isDestroyBoughtItems(buyer));
    }

    @Test
    void testSetIsDestroyBoughtItemsFalse() throws NoSuchFieldException, IllegalAccessException, IOException {
        ReflectionUtil.setPrivateField(null, BuyerMerchant.class.getDeclaredField("destroyBoughtItems"), true);
        assert BuyerMerchant.isDestroyBoughtItems(buyer);

        owner.setPower((byte)2);
        Properties properties = new Properties();
        properties.setProperty("destroy_items", "n");
        askManageQuestion();
        question.answer(properties);

        assertFalse(BuyerMerchant.isDestroyBoughtItems(buyer));
    }

    @Test
    void testSetIsDestroyBoughtItemsDefault() throws SQLException, IOException {
        BuyerScheduler.setDestroyBoughtItemsFor(buyer, true);

        owner.setPower((byte)2);
        Properties properties = new Properties();
        properties.setProperty("destroy_items", "d");
        askManageQuestion();
        question.answer(properties);

        assertNull(BuyerScheduler.getIsDestroyBoughtItemsFor(buyer));
    }

    @Test
    void testSetIsDestroyBoughtItemsTrueNotGM() {
        assert !BuyerMerchant.isDestroyBoughtItems(buyer);
        assert owner.getPower() < 2;

        Properties properties = new Properties();
        properties.setProperty("destroy_items", "y");
        askManageQuestion();
        question.answer(properties);

        assertFalse(BuyerMerchant.isDestroyBoughtItems(buyer));
    }
}
