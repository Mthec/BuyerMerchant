package com.wurmonline.server;

import com.wurmonline.server.players.Player;

import java.lang.reflect.Method;

import static org.mockito.Mockito.spy;

public class ServerPackageFactory {

    public static void addPlayer(Player player) {
        Players.getInstanceForUnitTestingWithoutDatabase().addPlayer(player);
    }
}
