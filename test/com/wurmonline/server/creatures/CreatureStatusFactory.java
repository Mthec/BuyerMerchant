package com.wurmonline.server.creatures;

import java.util.logging.Level;
import java.util.logging.Logger;

final class CreatureStatusFactory {
    private static final Logger logger = Logger.getLogger(CreatureStatusFactory.class.getName());

    private CreatureStatusFactory() {
    }

    static CreatureStatus createCreatureStatus(Creature creature, float posx, float posy, float rot, int layer) throws Exception {
        CreatureStatus toReturn = null;
        toReturn = new FakeCreatureStatus(creature);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("Created new CreatureStatus: " + toReturn);
        }

        return toReturn;
    }
}
