package com.wurmonline.server.creatures;

import com.wurmonline.server.bodys.BodyFactory;

import java.io.IOException;

public class FakeCreatureStatus extends CreatureStatus {

    public FakeCreatureStatus(Creature creature, float x, float y, float rot, int layer) {
        setPosition(new CreaturePos(creature.getWurmId(), x, y, 1, rot, 1, layer, -10, false));
        this.template = creature.getTemplate();
        statusHolder = creature;
        try {
            body = BodyFactory.getBody(creature, this.template.getBodyType(), this.template.getCentimetersHigh(), this.template.getCentimetersLong(), this.template.getCentimetersWide());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setVehicle(long l, byte b) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    void setLoyalty(float v) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void load() throws Exception {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void savePosition(long l, boolean b, int i, boolean b1) throws IOException {

    }

    @Override
    public boolean save() throws IOException {
        return false;
    }

    @Override
    public void setKingdom(byte b) throws IOException {
        kingdom = b;
    }

    @Override
    public void setDead(boolean b) throws IOException {
        dead = b;
    }

    @Override
    void updateAge(int i) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    void setDominator(long l) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setReborn(boolean b) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setLastPolledLoyalty() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    void setOffline(boolean b) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    boolean setStayOnline(boolean b) {
        return false;
    }

    @Override
    void setDetectionSecs() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    void setType(byte b) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    void updateFat() throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    void setInheritance(long l, long l1, long l2) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setInventoryId(long l) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    void saveCreatureName(String s) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    void setLastGroomed(long l) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    void setDisease(byte b) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
}
