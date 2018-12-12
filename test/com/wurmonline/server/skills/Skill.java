package com.wurmonline.server.skills;

public class Skill {

    private double knowledge;

    public double getKnowledge(double bonus) {
        return 100.0d;
    }

    // Used by CreatureStatus.getBodyType.  "He has a normal build."
    public double getKnowledge() { return 20.0d; }

    public double skillCheck(double check, double bonus, boolean b, float f) {
        return 100.0d;
    }
}
