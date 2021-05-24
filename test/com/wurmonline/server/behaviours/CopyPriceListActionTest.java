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
    private Creature targetBuyer;
    private CopyBuyerPriceListAction copyBuyerAction;
    private CopyContractPriceListAction copyContractAction;
    private Action act;

    @Override
    @BeforeEach
    protected void setUp() throws Throwable {
        super.setUp();
        contract = factory.createBuyerContract();
        targetBuyer = factory.createNewBuyer(owner);
        contract.setData(targetBuyer.getWurmId());
        ActionEntryBuilder.init();
        CopyPriceListAction.contractTemplateId = contract.getTemplateId();
        CopyPriceListAction.actionEntries.clear();
        copyBuyerAction = new CopyBuyerPriceListAction();
        copyContractAction = new CopyContractPriceListAction();
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
        assertNotNull(copyBuyerAction.getBehavioursFor(owner, contract, buyer));
        assertNull(copyBuyerAction.getBehavioursFor(owner, createNotContract(), buyer));
    }

    @Test
    void testGetBehaviourForIsBuyer() {
        assertNotNull(copyBuyerAction.getBehavioursFor(owner, contract, buyer));
        assertNull(copyBuyerAction.getBehavioursFor(owner, contract, player));
    }

    @Test
    void testGetBehaviourForIsOwner() {
        assertNotNull(copyBuyerAction.getBehavioursFor(owner, contract, buyer));
        assertNull(copyBuyerAction.getBehavioursFor(player, contract, buyer));
    }

    @Test
    void testGetBehaviourForIsSameBuyer() {
        assertNotNull(copyBuyerAction.getBehavioursFor(owner, contract, buyer));
        assertNull(copyBuyerAction.getBehavioursFor(player, contract, targetBuyer));
    }

    @Test
    void testBothActionsIncluded() {
        List<ActionEntry> entries = copyBuyerAction.getBehavioursFor(owner, contract, buyer);
        assertEquals(-2, entries.get(0).getNumber());
        assertEquals(copyBuyerAction.getActionId(), entries.get(1).getNumber());
        assertEquals(copyContractAction.getActionId(), entries.get(2).getNumber());
    }

    // action

    @Test
    void testActionWrongActionId() {
        short wrongId = (short)(copyBuyerAction.getActionId() + 10);
        assert wrongId != copyContractAction.getActionId();
        assertFalse(copyBuyerAction.action(act, owner, buyer, wrongId, 0));
    }

    @Test
    void testActionIsNotContract() {
        createNotContract();
        assertFalse(copyBuyerAction.action(act, owner, buyer, copyBuyerAction.getActionId(), 0));
    }

    @Test
    void testActionIsNotBuyer() {
        assertFalse(copyBuyerAction.action(act, owner, player, copyBuyerAction.getActionId(), 0));
    }

    @Test
    void testActionIsSameBuyer() {
        assertFalse(copyBuyerAction.action(act, owner, targetBuyer, copyBuyerAction.getActionId(), 0));
    }

    @Test
    void testActionBuyerHasNoShop() {
        Creature newBuyer = factory.createNewCreature(CreatureTemplate.SALESMAN_CID);
        newBuyer.setName("Buyer_Fred");
        newBuyer.getInventory().insertItem(factory.createPriceList());
        assert newBuyer.getShop() == null;

        assertTrue(copyBuyerAction.action(act, owner, newBuyer, copyBuyerAction.getActionId(), 0));

        assertThat(owner, receivedMessageContaining("does not have a shop"));
    }

    @Test
    void testActionIsNotOwner() {
        assertTrue(copyBuyerAction.action(act, player, buyer, copyBuyerAction.getActionId(), 0));

        assertThat(player, receivedMessageContaining("the owner"));
    }

    @Test
    void testActionIsNotContractBuyerOwner() {
        Creature notOwnedBuyer = factory.createNewBuyer(player);
        contract.setData(notOwnedBuyer.getWurmId());
        assertTrue(copyBuyerAction.action(act, owner, buyer, copyBuyerAction.getActionId(), 0));

        assertThat(owner, receivedMessageContaining("the owner"));
    }

    @Test
    void testActionContractDataNotSet() {
        contract.setData(-10);
        assertTrue(copyBuyerAction.action(act, owner, buyer, copyBuyerAction.getActionId(), 0));

        assertThat(owner, receivedMessageContaining("does not know who"));
    }

    @Test
    void testActionContractBuyerHasNoPriceList() {
        ItemsPackageFactory.removeItem(targetBuyer, targetBuyer.getInventory().getFirstContainedItem());
        assertTrue(copyBuyerAction.action(act, owner, buyer, copyBuyerAction.getActionId(), 0));

        assertThat(owner, receivedMessageContaining("does not know who"));
    }

    @Test
    void testActionTargetBuyerHasNoPriceList() {
        ItemsPackageFactory.removeItem(buyer, buyer.getInventory().getFirstContainedItem());

        assertTrue(copyBuyerAction.action(act, owner, buyer, copyBuyerAction.getActionId(), 0));
        assertThat(owner, receivedMessageContaining("does not know who"));
    }

    // copyToBuyer action

    @Test
    void testBuyerActionCopiedSuccessfully() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException, PriceList.PageNotAdded {
        PriceList priceList = PriceList.getPriceListFromBuyer(buyer);
        for (int i = 1; i < 100; i++) {
            priceList.addItem(i, (byte)1);
        }
        priceList.savePriceList();
        Item oldPriceList = targetBuyer.getInventory().getFirstContainedItem();

        assertEquals(3, buyer.getInventory().getFirstContainedItem().getItemCount());

        assertTrue(copyBuyerAction.action(act, owner, buyer, copyBuyerAction.getActionId(), 0));
        assertThat(owner, receivedMessageContaining("successfully copied"));

        PriceList copiedList = PriceList.getPriceListFromBuyer(targetBuyer);
        List<PriceList.Entry> sortedEntries = new ArrayList<>();
        copiedList.iterator().forEachRemaining(sortedEntries::add);
        for (int i = 0; i < 99; i++) {
            assertEquals(i + 1, sortedEntries.get(i).getTemplateId());
        }

        assertFalse(targetBuyer.getInventory().getItems().contains(oldPriceList));
    }

    @Test
    void testBuyerActionFailedCopyHandling() {
        Item priceList = spy(factory.createPriceList());
        int templateId = buyer.getInventory().getFirstContainedItem().getTemplateId();
        ItemsPackageFactory.removeItem(buyer, buyer.getInventory().getFirstContainedItem());
        buyer.getInventory().insertItem(priceList);
        Item oldPriceList = targetBuyer.getInventory().getFirstContainedItem();
        when(priceList.getTemplateId()).thenReturn(templateId, -1);

        assertTrue(copyBuyerAction.action(act, owner, buyer, copyBuyerAction.getActionId(), 0));
        assertThat(owner, receivedMessageContaining("looks confused"));

        assertTrue(targetBuyer.getInventory().getItems().contains(oldPriceList));
    }

    // copyToContract action

    @Test
    void testContractActionCopiedSuccessfully() throws IOException, PriceList.PriceListFullException, NoSuchTemplateException, PriceList.PageNotAdded {
        PriceList priceList = PriceList.getPriceListFromBuyer(targetBuyer);
        for (int i = 1; i < 100; i++) {
            priceList.addItem(i, (byte)1);
        }
        priceList.savePriceList();
        Item oldPriceList = buyer.getInventory().getFirstContainedItem();

        assertEquals(3, targetBuyer.getInventory().getFirstContainedItem().getItemCount());

        assertTrue(copyContractAction.action(act, owner, buyer, copyContractAction.getActionId(), 0));
        assertThat(owner, receivedMessageContaining("successfully copied"));

        PriceList copiedList = PriceList.getPriceListFromBuyer(targetBuyer);
        List<PriceList.Entry> sortedEntries = new ArrayList<>();
        copiedList.iterator().forEachRemaining(sortedEntries::add);
        for (int i = 0; i < 99; i++) {
            assertEquals(i + 1, sortedEntries.get(i).getTemplateId());
        }

        assertFalse(buyer.getInventory().getItems().contains(oldPriceList));
    }

    @Test
    void testContractActionFailedCopyHandling() {
        Item priceList = spy(factory.createPriceList());
        int templateId = targetBuyer.getInventory().getFirstContainedItem().getTemplateId();
        ItemsPackageFactory.removeItem(targetBuyer, targetBuyer.getInventory().getFirstContainedItem());
        targetBuyer.getInventory().insertItem(priceList);
        Item oldPriceList = buyer.getInventory().getFirstContainedItem();
        when(priceList.getTemplateId()).thenReturn(templateId, -1);

        assertTrue(copyContractAction.action(act, owner, buyer, copyContractAction.getActionId(), 0));
        assertThat(owner, receivedMessageContaining("looks confused"));

        assertTrue(buyer.getInventory().getItems().contains(oldPriceList));
    }

    // isBuyer

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
