package org.gotti.wurmunlimited.modsupport.actions;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Behaviour;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.skills.NoSuchSkillException;
import com.wurmonline.server.skills.Skill;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModActions {
    @SuppressWarnings("SpellCheckingInspection")
    private static boolean inited = false;
    private static short lastServerActionId = 1000;
    private static final List<BehaviourProvider> behaviourProviders = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<Short, ActionPerformerChain> actionPerformers = new ConcurrentHashMap<>();

    public ModActions() {
    }

    public static int getNextActionId() {
        return Actions.actionEntrys.length;
    }

    private static void initLastServerActionId() {
        if (lastServerActionId == 0) {
            lastServerActionId = (short)(Actions.actionEntrys.length - 1);
        }

    }

    public static short getLastServerActionId() {
        initLastServerActionId();
        return lastServerActionId;
    }

    public static void registerAction(ActionEntry actionEntry) {
        initLastServerActionId();
        short number = actionEntry.getNumber();
        if (Actions.actionEntrys.length != number) {
            throw new RuntimeException(String.format("Trying to register an action with the wrong action number. Expected %d, got %d", Actions.actionEntrys.length, number));
        } else {
            ActionEntry[] newArray = Arrays.copyOf(Actions.actionEntrys, number + 1);
            newArray[number] = actionEntry;

            try {
                setFinalField(null, ReflectionUtil.getField(Actions.class, "actionEntrys"), newArray);
            } catch (IllegalArgumentException | ClassCastException | NoSuchFieldException var4) {
                throw new RuntimeException(var4);
            }
        }
    }

    public static void registerAction(ModAction testAction) {
        registerActionPerformer(testAction.getActionPerformer());
        registerBehaviourProvider(testAction.getBehaviourProvider());
    }

    public static void registerActionPerformer(ActionPerformer actionPerformer) {
        if (actionPerformer != null) {
            short actionId = actionPerformer.getActionId();
            actionPerformers.computeIfAbsent(actionId, ActionPerformerChain::new).addActionPerformer(actionPerformer);
        }

    }

    public static void registerBehaviourProvider(BehaviourProvider behaviourProvider) {
        if (behaviourProvider != null && !behaviourProviders.contains(behaviourProvider)) {
            behaviourProviders.add(behaviourProvider);
        }

    }

    public static void init() {
        if (!inited) {
            inited = true;
        }
    }

    public static ActionPerformerBase getActionPerformer(Action action) {
        short actionId = action.getActionEntry().getNumber();
        return actionPerformers.get(actionId);
    }

    public static BehaviourProvider getBehaviourProvider(Behaviour behaviour) {
        return !behaviourProviders.isEmpty() ? new ChainedBehaviourProvider(new WrappedBehaviourProvider(behaviour), behaviourProviders) : null;
    }

    public static Skill getSkillOrNull(Creature creature, int skillId) {
        try {
            return creature.getSkills().getSkill(skillId);
        } catch (NoSuchSkillException var3) {
            return null;
        }
    }

    public static void setFinalField(Object obj, Field field, Object value) {
        try {
            field.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(obj, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
