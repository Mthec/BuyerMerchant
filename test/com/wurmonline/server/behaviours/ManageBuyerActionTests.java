package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.BuyerManagementQuestion;
import mod.wurmunlimited.WurmObjectsFactory;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mod.wurmunlimited.Assert.bmlEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ManageBuyerActionTests {
    private static ManageBuyerAction manage;
    private WurmObjectsFactory factory;
    private Player gm;
    private Player player;
    private Creature buyer;
    private Item writ;

    @BeforeAll
    public static void create() {
        ActionEntryBuilder.init();
        manage = new ManageBuyerAction();
        ModActions.registerAction(manage);
    }

    @BeforeEach
    void setUp() throws Throwable {
        factory = new WurmObjectsFactory();
        gm = factory.createNewPlayer();
        gm.setPower((byte)2);
        player = factory.createNewPlayer();
        buyer = factory.createNewBuyer(player);
        writ = factory.createWritFor(player, buyer);
        ManageBuyerAction.gmManagePowerRequired = 2;
    }

    // getBehavioursFor

    private boolean isBehaviour(List<ActionEntry> entries) {
        return entries.size() == 1 && entries.get(0).getActionString().equals("Manage traders");
    }

    private boolean isEmpty(List<ActionEntry> entries) {
        return entries.isEmpty();
    }

    @Test
    public void testGetBehavioursFor() {
        assertTrue(isBehaviour(manage.getBehavioursFor(gm, buyer)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, buyer)));
    }

    @Test
    public void testGetBehavioursForItem() {
        Item writ = factory.createWritFor(factory.createNewPlayer(), buyer);
        assertTrue(isBehaviour(manage.getBehavioursFor(gm, writ, buyer)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, writ, buyer)));
    }

    @Test
    public void testGetBehavioursForNotBuyer() {
        Creature notBuyer = factory.createNewCreature();
        assertTrue(isEmpty(manage.getBehavioursFor(gm, notBuyer)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, notBuyer)));
        assertTrue(isEmpty(manage.getBehavioursFor(gm, factory.createWritFor(gm, buyer), notBuyer)));
        assertTrue(isEmpty(manage.getBehavioursFor(player, writ, notBuyer)));
    }

    @Test
    public void testGetBehavioursForGmManagePower() {
        assert ManageBuyerAction.gmManagePowerRequired == 2;
        assertTrue(isBehaviour(manage.getBehavioursFor(gm, buyer)));
    }

    @Test
    public void testGetBehavioursForGmManagePowerTooHigh() {
        ManageBuyerAction.gmManagePowerRequired = 10;
        assertTrue(isEmpty(manage.getBehavioursFor(gm, buyer)));
    }

    // action

    private void sendQuestion() {
        new BuyerManagementQuestion(gm, writ.getWurmId()).sendQuestion();
        new BuyerManagementQuestion(player, writ.getWurmId()).sendQuestion();
    }

    @Test
    public void testAction() {
        Action action = mock(Action.class);
        assertTrue(manage.action(action, gm, buyer, manage.getActionId(), 0));
        assertTrue(manage.action(action, player, buyer, manage.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
        sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    public void testActionItem() {
        Action action = mock(Action.class);
        Item item = factory.createNewItem();
        assertTrue(manage.action(action, gm, item, buyer, manage.getActionId(), 0));
        assertTrue(manage.action(action, player, item, buyer, manage.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
        sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    public void testActionNotBuyer() {
        Action action = mock(Action.class);
        Creature notBuyer = factory.createNewCreature();
        Item writ = factory.createWritFor(player, notBuyer);
        assertTrue(manage.action(action, gm, notBuyer, manage.getActionId(), 0));
        assertTrue(manage.action(action, player, notBuyer, manage.getActionId(), 0));
        assertTrue(manage.action(action, gm, writ, notBuyer, manage.getActionId(), 0));
        assertTrue(manage.action(action, player, writ, notBuyer, manage.getActionId(), 0));

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }

    @Test
    public void testActionWrongActionId() {
        Action action = mock(Action.class);
        assertTrue(manage.action(action, gm, buyer, (short)(manage.getActionId() + 1), 0));
        assertTrue(manage.action(action, player, buyer, (short)(manage.getActionId() + 1), 0));

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
        assertEquals(0, factory.getCommunicator(player).getBml().length);
    }

    @Test
    public void testActionGmPowerRequired() {
        assert ManageBuyerAction.gmManagePowerRequired == 2;
        Action action = mock(Action.class);
        assertTrue(manage.action(action, gm, buyer, manage.getActionId(), 0));

        assertEquals(1, factory.getCommunicator(gm).getBml().length);
        sendQuestion();
        assertThat(gm, bmlEqual());
    }

    @Test
    public void testActionGmPowerRequiredTooHigh() {
        ManageBuyerAction.gmManagePowerRequired = 10;
        Action action = mock(Action.class);
        assertTrue(manage.action(action, gm, buyer, manage.getActionId(), 0));

        assertEquals(0, factory.getCommunicator(gm).getBml().length);
    }
}
