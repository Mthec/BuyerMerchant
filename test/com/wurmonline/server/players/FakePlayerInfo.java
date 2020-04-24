package com.wurmonline.server.players;

import com.wurmonline.server.deities.Deity;
import com.wurmonline.server.steam.SteamId;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class FakePlayerInfo extends PlayerInfo {

    public FakePlayerInfo(String aname) {
        super(aname);
    }

    @Override
    public void setPower(byte var1) throws IOException {
        power = var1;
    }

    @Override
    public void setPaymentExpire(long var1) throws IOException {

    }

    @Override
    public void setPaymentExpire(long var1, boolean var3) throws IOException {

    }

    @Override
    public void setBanned(boolean var1, String var2, long var3) throws IOException {

    }

    @Override
    public void resetWarnings() throws IOException {

    }

    @Override
    void setReputation(int var1) {

    }

    @Override
    public void setMuted(boolean var1, String var2, long var3) {

    }

    @Override
    void setFatigueSecs(int var1, long var2) {

    }

    @Override
    void setCheated(String var1) {

    }

    @Override
    public void updatePassword(String var1) throws IOException {

    }

    @Override
    public void setRealDeath(byte var1) throws IOException {

    }

    @Override
    public void setFavor(float var1) throws IOException {

    }

    @Override
    public void setFaith(float var1) throws IOException {

    }

    @Override
    void setDeity(Deity var1) throws IOException {

    }

    @Override
    void setAlignment(float var1) throws IOException {

    }

    @Override
    void setGod(Deity var1) throws IOException {

    }

    @Override
    public void load() throws IOException {

    }

    @Override
    public void warn() throws IOException {

    }

    @Override
    public void save() throws IOException {

    }

    @Override
    public void setLastTrigger(int var1) {

    }

    @Override
    void setIpaddress(String var1) throws IOException {

    }

    @Override
    void setSteamId(SteamId var1) throws IOException {

    }

    @Override
    public void setRank(int var1) throws IOException {

    }

    @Override
    public void setReimbursed(boolean var1) throws IOException {

    }

    @Override
    void setPlantedSign() throws IOException {

    }

    @Override
    void setChangedDeity() throws IOException {

    }

    @Override
    public String getIpaddress() {
        return null;
    }

    @Override
    void setDead(boolean var1) {

    }

    @Override
    public void setSessionKey(String var1, long var2) throws IOException {

    }

    @Override
    void setName(String var1) throws IOException {

    }

    @Override
    public void setVersion(long var1) throws IOException {

    }

    @Override
    void saveFriend(long var1, long var3, byte var5, String var6) throws IOException {

    }

    @Override
    void updateFriend(long var1, long var3, byte var5, String var6) throws IOException {

    }

    @Override
    void deleteFriend(long var1, long var3) throws IOException {

    }

    @Override
    void saveEnemy(long var1, long var3) throws IOException {

    }

    @Override
    void deleteEnemy(long var1, long var3) throws IOException {

    }

    @Override
    void saveIgnored(long var1, long var3) throws IOException {

    }

    @Override
    void deleteIgnored(long var1, long var3) throws IOException {

    }

    @Override
    public void setNumFaith(byte var1, long var2) throws IOException {

    }

    @Override
    long getFlagLong() {
        return 0;
    }

    @Override
    long getFlag2Long() {
        return 0;
    }

    @Override
    public void setMoney(long var1) throws IOException {

    }

    @Override
    void setSex(byte var1) throws IOException {

    }

    @Override
    void setClimbing(boolean var1) throws IOException {

    }

    @Override
    void setChangedKingdom(byte var1, boolean var2) throws IOException {

    }

    @Override
    public void setFace(long var1) throws IOException {

    }

    @Override
    boolean addTitle(Titles.Title var1) {
        return false;
    }

    @Override
    boolean removeTitle(Titles.Title var1) {
        return false;
    }

    @Override
    void setAlcohol(float var1) {

    }

    @Override
    void setPet(long var1) {

    }

    @Override
    public void setNicotineTime(long var1) {

    }

    @Override
    public boolean setAlcoholTime(long var1) {
        return false;
    }

    @Override
    void setNicotine(float var1) {

    }

    @Override
    public void setMayMute(boolean var1) {

    }

    @Override
    public void setEmailAddress(String var1) {

    }

    @Override
    void setPriest(boolean var1) {

    }

    @Override
    public void setOverRideShop(boolean var1) {

    }

    @Override
    public void setReferedby(long var1) {

    }

    @Override
    public void setBed(long var1) {

    }

    @Override
    void setLastChangedVillage(long var1) {

    }

    @Override
    void setSleep(int var1) {

    }

    @Override
    void setTheftwarned(boolean var1) {

    }

    @Override
    public void setHasNoReimbursementLeft(boolean var1) {

    }

    @Override
    void setDeathProtected(boolean var1) {

    }

    @Override
    public void setCurrentServer(int var1) {

    }

    @Override
    public void setDevTalk(boolean var1) {

    }

    @Override
    public void transferDeity(@Nullable Deity var1) throws IOException {

    }

    @Override
    void saveSwitchFatigue() {

    }

    @Override
    void saveFightMode(byte var1) {

    }

    @Override
    void setNextAffinity(long var1) {

    }

    @Override
    public void saveAppointments() {

    }

    @Override
    void setTutorialLevel(int var1) {

    }

    @Override
    void setAutofight(boolean var1) {

    }

    @Override
    void setLastVehicle(long var1) {

    }

    @Override
    public void setIsPlayerAssistant(boolean var1) {

    }

    @Override
    public void setMayAppointPlayerAssistant(boolean var1) {

    }

    @Override
    public boolean togglePlayerAssistantWindow(boolean var1) {
        return false;
    }

    @Override
    public void setLastTaggedTerr(byte var1) {

    }

    @Override
    public void setNewPriestType(byte var1, long var2) {

    }

    @Override
    public void setChangedJoat() {

    }

    @Override
    public void setMovedInventory(boolean var1) {

    }

    @Override
    public void setFreeTransfer(boolean var1) {

    }

    @Override
    public boolean setHasSkillGain(boolean var1) {
        return false;
    }

    @Override
    public void loadIgnored(long var1) {

    }

    @Override
    public void loadTitles(long var1) {

    }

    @Override
    public void loadFriends(long var1) {

    }

    @Override
    public void loadHistorySteamIds(long var1) {

    }

    @Override
    public void loadHistoryIPs(long var1) {

    }

    @Override
    public void loadHistoryEmails(long var1) {

    }

    @Override
    public boolean setChampionPoints(short var1) {
        return false;
    }

    @Override
    public void setChangedKingdom() {

    }

    @Override
    public void setChampionTimeStamp() {

    }

    @Override
    public void setChampChanneling(float var1) {

    }

    @Override
    public void setMuteTimes(short var1) {

    }

    @Override
    public void setVotedKing(boolean var1) {

    }

    @Override
    public void setEpicLocation(byte var1, int var2) {

    }

    @Override
    public void setChaosKingdom(byte var1) {

    }

    @Override
    public void setHotaWins(short var1) {

    }

    @Override
    public void setSpamMode(boolean var1) {

    }

    @Override
    public void setKarma(int var1) {

    }

    @Override
    public void setScenarioKarma(int var1) {

    }

    @Override
    public void setBlood(byte var1) {

    }

    @Override
    public void setFlag(int var1, boolean var2) {

    }

    @Override
    public void setFlagBits(long var1) {

    }

    @Override
    public void setFlag2Bits(long var1) {

    }

    @Override
    public void forceFlagsUpdate() {

    }

    @Override
    public void setAbility(int var1, boolean var2) {

    }

    @Override
    public void setCurrentAbilityTitle(int var1) {

    }

    @Override
    public void setUndeadData() {

    }

    @Override
    public void setModelName(String var1) {

    }

    @Override
    public void addMoneyEarnedBySellingEver(long var1) {

    }

    @Override
    public void setPointsForChamp() {

    }

    @Override
    public void switchChamp() {

    }

    @Override
    public void setPassRetrieval(String var1, String var2) throws IOException {

    }
}
