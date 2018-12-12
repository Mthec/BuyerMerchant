package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;

abstract class QuestionExtension extends Question {
    QuestionExtension(Creature aResponder, String aTitle, String aQuestion, int aType, long aTarget) {
        super(aResponder, aTitle, aQuestion, aType, aTarget);
    }

    boolean wasSelected(String id) {
        String val = getAnswer().getProperty(id);
        return val != null && val.equals("true");
    }
}
