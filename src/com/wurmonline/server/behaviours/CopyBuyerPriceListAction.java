package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.List;

public class CopyBuyerPriceListAction implements ModAction, ActionPerformer, BehaviourProvider {

    private final short actionId;

    public CopyBuyerPriceListAction() {
        actionId = (short)ModActions.getNextActionId();

        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "To Contract Buyer", "copying price list",
                new int[] { ActionTypes.ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM,
                            ActionTypes.ACTION_TYPE_QUICK
                }).build();

        ModActions.registerAction(actionEntry);
        CopyPriceListAction.actionEntries.add(actionEntry);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
        return CopyPriceListAction.getBehavioursFor(performer, subject, target);
    }

    @Override
    public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
        if (num == actionId) {
            return CopyPriceListAction.action(performer, action.getSubjectId(), target, false);
        }
        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
