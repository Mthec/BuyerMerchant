package com.wurmonline.server.zones;

import com.wurmonline.math.TilePos;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import org.jetbrains.annotations.Nullable;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Zones {

    public static Item marketStall = null;
    public static Creature creature = null;

    public static int numberOfZones;
    public static int worldTileSizeX = 100;
    public static int worldTileSizeY = 100;
    public static float worldMeterSizeX = (float)((worldTileSizeX - 1) * 4);
    public static float worldMeterSizeY = (float)((worldTileSizeY - 1) * 4);

    public static void resetStatic() {
        marketStall = null;
        creature = null;
    }

    public static int safeTileX(int i) {
        return 1;
    }

    public static int safeTileY(int i) {
        return 1;
    }

    public static final float calculatePosZ(float posx, float posy, VolaTile tile, boolean isOnSurface, boolean floating, float currentPosZ, @Nullable Creature creature, long bridgeId) {
        return 1;
    }

    public static Zone getZone(int tilex, int tiley, boolean surfaced) throws NoSuchZoneException {

        Zone newZone = mock(Zone.class);
        VolaTile volaTile = mock(VolaTile.class);
        when(newZone.getOrCreateTile(anyInt(), anyInt())).thenReturn(volaTile);
        when(volaTile.getVillage()).thenReturn(null);
        when(volaTile.getStructure()).thenReturn(null);
        if (marketStall == null)
            when(volaTile.getItems()).thenReturn(new Item[0]);
        else
            when(volaTile.getItems()).thenReturn(new Item[] {marketStall});
        if (creature == null)
            when(volaTile.getCreatures()).thenReturn(new Creature[0]);
        else
            when(volaTile.getCreatures()).thenReturn(new Creature[] {creature});
//        ;new Zone(tilex, tilex, tiley, tiley, surfaced) {
//            @Override
//            void save() throws IOException {
//
//            }
//
//            @Override
//            void load() throws IOException {
//
//            }
//
//            @Override
//            void loadFences() throws IOException {
//
//            }
//        };
//        try {
//            Field wurmid = Zone.class.getDeclaredMethod("getOrCreateTile", TilePos.class);
//            wurmid.setAccessible(true);
//            Field modifiers = Field.class.getDeclaredField("modifiers");
//            modifiers.setAccessible(true);
//            modifiers.setInt(wurmid, wurmid.getModifiers() & ~Modifier.FINAL);
//            wurmid.set(newShop, wurmId);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        return newZone;
    }

    public static VolaTile getOrCreateTile(int tileX, int tileY, boolean surface) throws NoSuchZoneException {
        return getZone(tileX, tileY, surface).getOrCreateTile(tileX, tileY);
    }

    public static VirtualZone createZone(Creature watcher, int startX, int startY, int centerX, int centerY, int size, boolean surface) {
        return new VirtualZone(watcher, startX, startY, centerX, centerY, size, surface);
    }

    public static Zone[] getZonesCoveredBy(VirtualZone v) {
        return new Zone[0];
    }

    public static float calculateHeight(float x, float y, boolean surface) {
        return 0f;
    }

    public static VolaTile getTileOrNull(int tileX, int tileY, boolean surface) throws NoSuchZoneException {
        return null;
    }

    public static VolaTile getTileOrNull(TilePos pos, boolean var1) throws NoSuchZoneException {
        return null;
    }

    public static Zone[] getZonesCoveredBy(int var1, int var2, int var3, int var4, boolean var5) {
        return new Zone[0];
    }

    public static void removeZone(int var1) {

    }
}
