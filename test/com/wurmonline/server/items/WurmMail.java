package com.wurmonline.server.items;

import java.util.ArrayList;
import java.util.List;

public class WurmMail {

    public static List<WurmMail> allMail = new ArrayList<>(5);
    public long itemId;
    public long ownerId;

    public WurmMail(byte _type, long _itemid, long _sender, long _receiver, long _price, long _sent, long _expiration, int _sourceserver, boolean _rejected, boolean loading) {
        itemId = _itemid;
        ownerId = _receiver;
    }

    public static void resetStatic() {
        allMail = new ArrayList<>(5);
    }

    public static void addWurmMail(WurmMail mail) {
        allMail.add(mail);
    }

    public void createInDatabase() {

    }
}
