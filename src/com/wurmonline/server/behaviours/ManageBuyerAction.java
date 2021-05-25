package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.BuyerManagementQuestion;
import org.gotti.wurmunlimited.modsupport.actions.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class ManageBuyerAction implements ModAction, ActionPerformer, BehaviourProvider {
    private static final Logger logger = Logger.getLogger(ManageBuyerAction.class.getName());
    public static byte gmManagePowerRequired;
    private final short actionId;
    private final List<ActionEntry> entries;
    private final List<ActionEntry> empty = Collections.emptyList();

    public ManageBuyerAction() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Manage traders", "managing traders").build();
        ModActions.registerAction(actionEntry);
        entries = Collections.singletonList(actionEntry);
    }

    private static boolean writValid(Creature performer, Creature buyer, @Nullable Item writ) {
        return writ != null && writ.getTemplateId() == CopyPriceListAction.contractTemplateId &&
                       performer.getInventory().getItems().contains(writ) && writ.getData() == buyer.getWurmId();
    }

    private static boolean canManage(Creature performer, Creature buyer, @Nullable Item item) {
        if (!(performer.isPlayer() && buyer.getName().startsWith("Buyer_") && buyer.getTemplate().id == CreatureTemplateIds.SALESMAN_CID))
            return false;

        return performer.getPower() >= gmManagePowerRequired || writValid(performer, buyer, item);
    }

    private static Item getWrit(Creature performer, Creature buyer) {
        Optional<Item> maybeItem = performer.getInventory().getItems().stream()
                            .filter(i -> i.getTemplateId() == CopyPriceListAction.contractTemplateId &&
                                                 i.getData() == buyer.getWurmId()).findAny();
        if (maybeItem.isPresent()) {
            return maybeItem.get();
        }

        maybeItem = Arrays.stream(Items.getAllItems())
                            .filter(i -> i.getTemplateId() == CopyPriceListAction.contractTemplateId &&
                                                 i.getData() == buyer.getWurmId()).findAny();

        return maybeItem.orElse(null);
    }

    private List<ActionEntry> getBehaviours(Creature performer, Creature buyer, @Nullable Item subject) {
        if (canManage(performer, buyer, subject)) {
            return entries;
        }

        return empty;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        return getBehaviours(performer, target, subject);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
        return getBehaviours(performer, target, null);
    }

    private boolean action(Creature performer, Creature buyer, @Nullable Item item) {
        Item writ = getWrit(performer, buyer);
        if (writ != null && canManage(performer, buyer, item)) {
            new BuyerManagementQuestion(performer, writ.getWurmId()).sendQuestion();
        } else if (writ == null) {
            logger.warning("Could not find buyer's (" + buyer.getName() + ") contract on owner (" + buyer.getShop().getOwnerId() + ").");
            performer.getCommunicator().sendNormalServerMessage("Could not find the buyer's contract.");
        }

        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        if (num == actionId) {
            return action(performer, target, source);
        }

        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        if (num == actionId) {
            return action(performer, target, null);
        }

        return true;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
