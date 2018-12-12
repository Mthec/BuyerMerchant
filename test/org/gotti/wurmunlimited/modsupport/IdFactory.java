package org.gotti.wurmunlimited.modsupport;

public class IdFactory {

    private static int counter = 100000;

    public static int getIdFor(String identifier, IdType starter) {
        return counter++;
    }
}
