package com.wurmonline.server;

import java.math.BigInteger;

public class WurmId {
    private static long itemIdCounter;
    private static int savecounter;
    private static long playerIdCounter;
    private static long creatureIdCounter;
    private static long structureIdCounter;
    private static long tempIdCounter;
    private static long illusionIdCounter;
    private static long temporaryWoundIdCounter;
    private static long woundIdCounter;
    private static long temporarySkillsIdCounter;
    private static long playerSkillsIdCounter;
    private static long creatureSkillsIdCounter;
    private static long bankIdCounter;
    private static long spellIdCounter;
    private static long wccommandCounter;
    private static long planIdCounter;
    private static long bodyIdCounter;
    private static long coinIdCounter;
    private static long poiIdCounter;
    private static long couponIdCounter;

    public static final void loadIdNumbers(boolean create) {

    }

    public static final int getType(long id) {
        return (int)(id & 255L);
    }

    public static final int getOrigin(long id) {
        return (int)(id >> 8) & '\uffff';
    }

    public static final long getNumber(long id) {
        return id >> 24;
    }

    public static final long getId(long id) {
        return id;
    }

    public static final long getNextItemId() {
        ++itemIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(itemIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 2L;
    }

    public static final long getNextPlayerId() {
        ++playerIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(playerIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 0L;
    }

    public static final long getNextBodyPartId(long creatureId, byte bodyplace, boolean isPlayer) {
        return BigInteger.valueOf(BigInteger.valueOf(creatureId >> 8).shiftLeft(1).longValue() + (long)(isPlayer ? 1 : 0)).shiftLeft(16).longValue() + (long)(bodyplace << 8) + 19L;
    }

    public static final long getCreatureIdForBodyPart(long bodypartId) {
        boolean isPlayer = (BigInteger.valueOf(bodypartId).shiftRight(16).longValue() & 1L) == 1L;
        return (bodypartId >> 17) + (long)(isPlayer ? 0 : 1);
    }

    public static final int getBodyPlaceForBodyPart(long bodypartId) {
        return (int)(bodypartId >> 8 & 255L);
    }

    public static final long getNextCreatureId() {
        ++creatureIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(creatureIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 1L;
    }

    public static final long getNextStructureId() {
        ++structureIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(structureIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 4L;
    }

    public static final long getNextTempItemId() {
        ++tempIdCounter;
        return BigInteger.valueOf(tempIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 6L;
    }

    public static final long getNextIllusionId() {
        ++illusionIdCounter;
        return BigInteger.valueOf(illusionIdCounter).shiftLeft(24).longValue() + 24L;
    }

    public static final long getNextTemporaryWoundId() {
        ++temporaryWoundIdCounter;
        return BigInteger.valueOf(temporaryWoundIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 32L;
    }

    public static final long getNextWoundId() {
        ++woundIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(woundIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 8L;
    }

    public static final long getNextTemporarySkillId() {
        ++temporarySkillsIdCounter;
        return BigInteger.valueOf(temporarySkillsIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 31L;
    }

    public static final long getNextPlayerSkillId() {
        ++playerSkillsIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(playerSkillsIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 10L;
    }

    public static final long getNextCreatureSkillId() {
        ++creatureSkillsIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(creatureSkillsIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 9L;
    }

    public static final long getNextBankId() {
        ++bankIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(bankIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 13L;
    }

    public static final long getNextSpellId() {
        ++spellIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(spellIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 15L;
    }

    public static final long getNextWCCommandId() {
        ++wccommandCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(wccommandCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 21L;
    }

    public static final long getNextPlanId() {
        ++planIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(planIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 16L;
    }

    public static final long getNextBodyId() {
        ++bodyIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(bodyIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 19L;
    }

    public static final long getNextCoinId() {
        ++coinIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(coinIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 20L;
    }

    public static final long getNextPoiId() {
        ++poiIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(poiIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 26L;
    }

    public static final long getNextCouponId() {
        ++couponIdCounter;
        ++savecounter;
        checkSave();
        return BigInteger.valueOf(couponIdCounter).shiftLeft(24).longValue() + (long)(Servers.localServer.id << 8) + 29L;
    }

    public static final void checkSave() {
        if (savecounter >= 1000) {
            updateNumbers();
            savecounter = 0;
        }

    }

    public static final void updateNumbers() {}
    private static final void saveNumbers() {}
}
