package org.gotti.wurmunlimited.modsupport.actions;

import com.wurmonline.server.behaviours.ActionEntry;

public class ModActions {

    static int nextId;

    public static int getNextActionId() {
        return nextId;
    }

    public static void registerAction(ActionEntry entry) {
        ++nextId;
    }

    public static void registerAction(ModAction action) {}
}
