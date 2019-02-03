package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemsPackageFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import mod.wurmunlimited.WurmTradingTest;
import mod.wurmunlimited.buyermerchant.PriceList;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CopyPriceListActionTest extends WurmTradingTest {

    private Item contract;
    private Creature copyToBuyer;
    private CopyPriceListAction action;
    private short id;
    private Action act;

    @Override
    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        contract = factory.createBuyerContract();
        copyToBuyer = factory.createNewBuyer(owner);
        contract.setData(copyToBuyer.getWurmId());
        ActionEntryBuilder.init();
        action = new CopyPriceListAction(contract.getTemplateId());
        id = action.getActionId();
        act = mock(Action.class);
        when(act.getSubjectId()).thenReturn(contract.getWurmId());
    }

    private Item createNotContract() {
        Item notContract = factory.createNewItem();
        assert notContract.getTemplateId() != contract.getTemplateId();
        when(act.getSubjectId()).thenReturn(notContract.getWurmId());
        return notContract;
    }

    // getBehaviourFor

    @Test
    void testGetBehaviourForIsContract() {
        assertNotNull(action.getBehavioursFor(owner, contract, buyer));
        assertNull(action.getBehavioursFor(owner, createNotContract(), buyer));
    }

    @Test
    void testGetBehaviourForIsBuyer() {
        assertNotNull(action.getBehavioursFor(owner, contract, buyer));
        assertNull(action.getBehavioursFor(owner, contract, player));
    }

    @Test
    void testGetBehaviourForIsOwner() {
        assertNotNull(action.getBehavioursFor(owner, contract, buyer));
        assertNull(action.getBehavioursFor(player, contract, buyer));
    }

    @Test
    void testGetBehaviourForIsSameBuyer() {
        assertNotNull(action.getBehavioursFor(owner, contract, buyer));
        assertNull(action.getBehavioursFor(player, contract, copyToBuyer));
    }

    // action

    @Test
    void testActionWrongActionId() {
        assertFalse(action.action(act, owner, contract, buyer, (short)(id + 1), 0));
    }

    @Test
    void testActionIsNotContract() {
        assertFalse(action.action(act, owner, createNotContract(), buyer, id, 0));
    }

    @Test
    void testActionIsNotBuyer() {
        assertFalse(action.action(act, owner, contract, player, id, 0));
    }

    @Test
    void testActionIsSameBuyer() {
        assertFalse(action.action(act, owner, contract, copyToBuyer, id, 0));
    }

    @Test
    void testActionBuyerHasNoShop() {
        Creature newBuyer = factory.createNewCreature(CreatureTemplate.SALESMAN_CID);
        newBuyer.setName("Buyer_Fred");
        newBuyer.getInventory().insertItem(factory.createPriceList());
        assert newBuyer.getShop() == null;

        assertTrue(action.action(act, owner, contract, newBuyer, id, 0));

        assertThat(owner, receivedMessageContaining("does not have a shop"));
    }

    @Test
    void testActionIsNotOwner() {
        assertTrue(action.action(act, player, contract, buyer, id, 0));

        assertThat(player, receivedMessageContaining("the owner"));
    }

    @Test
    void testActionIsNotContractBuyerOwner() {
        Creature notOwnedBuyer = factory.createNewBuyer(player);
        contract.setData(notOwnedBuyer.getWurmId());
        assertTrue(action.action(act, owner, contract, buyer, id, 0));

        assertThat(owner, receivedMessageContaining("the owner"));
    }

    @Test
    void testActionContractDataNotSet() {
        contract.setData(-10);
        assertTrue(action.action(act, owner, contract, buyer, id, 0));

        assertThat(owner, receivedMessageContaining("does not know who"));
    }

    @Test
    void testActionContractBuyerHasNoPriceList() {
        ItemsPackageFactory.removeItem(copyToBuyer, copyToBuyer.getInventory().getFirstContainedItem());
        assertTrue(action.action(act, owner, contract, buyer, id, 0));

        assertThat(owner, receivedMessageContaining("does not know who"));
    }

    @Test
    void testActionTargetBuyerHasNoPriceList() {
        ItemsPackageFactory.removeItem(buyer, buyer.getInventory().getFirstContainedItem());

        assertTrue(action.action(act, owner, contract, buyer, id, 0));
        assertThat(owner, receivedMessageContaining("does not know who"));
    }

    @Test
    void testActionCopiedSuccessfully() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException, PriceList.PageNotAdded {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        for (int i = 1; i < 100; i++) {
            priceList.addItem(i, (byte)1);
        }
        priceList.savePriceList();
        Item oldPriceList = copyToBuyer.getInventory().getFirstContainedItem();

        assertEquals(3, buyer.getInventory().getFirstContainedItem().getItemCount());

        assertTrue(action.action(act, owner, contract, buyer, id, 0));
        assertThat(owner, receivedMessageContaining("successfully copied"));

        PriceList copiedList = PriceList.getPriceListFromBuyer(copyToBuyer);
        List<PriceList.Entry> sortedEntries = new ArrayList<>();
        copiedList.iterator().forEachRemaining(sortedEntries::add);
        for (int i = 0; i < 99; i++) {
            assertEquals(i + 1, sortedEntries.get(i).getTemplateId());
        }

        assertFalse(copyToBuyer.getInventory().getItems().contains(oldPriceList));
    }

    @Test
    void testActionFailedCopyHandling() {
        Item priceList = spy(factory.createPriceList());
        int templateId = buyer.getInventory().getFirstContainedItem().getTemplateId();
        ItemsPackageFactory.removeItem(buyer, buyer.getInventory().getFirstContainedItem());
        buyer.getInventory().insertItem(priceList);
        Item oldPriceList = copyToBuyer.getInventory().getFirstContainedItem();
        when(priceList.getTemplateId()).thenReturn(templateId, -1);

        assertTrue(action.action(act, owner, contract, buyer, id, 0));
        assertThat(owner, receivedMessageContaining("looks confused"));

        assertTrue(copyToBuyer.getInventory().getItems().contains(oldPriceList));
    }

    @Test
    void testIsBuyer() {
        assertAll(
                () -> assertTrue(CopyPriceListAction.isBuyer(buyer)),
                () -> assertFalse(CopyPriceListAction.isBuyer(player)),
                () -> assertFalse(CopyPriceListAction.isBuyer(factory.createNewMerchant(owner))),
                () -> assertFalse(CopyPriceListAction.isBuyer(factory.createNewTrader()))
        );
    }
}
