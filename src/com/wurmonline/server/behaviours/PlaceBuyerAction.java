package com.wurmonline.server.behaviours;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.BuyerManagementQuestion;
import com.wurmonline.server.zones.VolaTile;

public class PlaceBuyerAction implements NpcMenuEntry {
    public PlaceBuyerAction() {
        PlaceNpcMenu.addNpcAction(this);
    }

    @Override
    public String getName() {
        return "Buyer";
    }

    @Override
    public boolean doAction(Action action, short num, Creature performer, Item source, VolaTile tile, int floorLevel) {
        Item contract = null;
        try {
            for (Item item : performer.getAllItems()) {
                if (item.getTemplateId() == CopyPriceListAction.contractTemplateId && item.getData() == -1) {
                    contract = item;
                    break;
                }
            }

            if (contract == null) {
                contract = Creature.createItem(CopyPriceListAction.contractTemplateId, (float)(10 + Server.rand.nextInt(80)));
                performer.getInventory().insertItem(contract);
            }

            new BuyerManagementQuestion(performer, contract.getWurmId()).sendQuestion();
        } catch (Exception e) {
            e.printStackTrace();
            performer.getCommunicator().sendNormalServerMessage("A new contract could not be created.");
        }

        return true;
    }
}
