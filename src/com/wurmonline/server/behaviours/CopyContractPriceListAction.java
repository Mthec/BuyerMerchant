package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.List;

public class CopyContractPriceListAction implements ModAction, ActionPerformer, BehaviourProvider {

    private final short actionId;
    private final ActionEntry actionEntry;

    public CopyContractPriceListAction() {
        actionId = (short)ModActions.getNextActionId();

        actionEntry = new ActionEntryBuilder(actionId, "To This Buyer", "copying price list",
                new int[] { ActionTypes.ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM,
                            ActionTypes.ACTION_TYPE_QUICK
                }).build();

        ModActions.registerAction(actionEntry);
        CopyPriceListAction.actionEntries.add(actionEntry);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        if (num == actionId) {
            return CopyPriceListAction.action(performer, action.getSubjectId(), target, true);
        }
        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
