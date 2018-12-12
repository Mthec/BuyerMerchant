package com.wurmonline.server.skills;

import org.jetbrains.annotations.NotNull;

public final class SkillsFactory {

    public static Skills createSkills(long id) {

        Skills skills = new Skills() {
            @Override
            public TempSkill learnTemp(int i, float f) {
                return null;
            }

            @NotNull
            @Override
            public Skill learn(int skillNumber, float startValue) {
                Skill skill = new Skill();
                skills.put(skillNumber, skill);
                return skill;
            }

            @Override
            public long getId() {
                return id;
            }

            @Override
            public void load() throws Exception {

            }

            @Override
            public void delete() throws Exception {

            }
        };

        skills.learn(102, 20.0f);
        return skills;
    }

    public static Skills createSkills(String templateName) {
        return createSkills(1);
    }
}
