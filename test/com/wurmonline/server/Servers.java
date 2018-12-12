package com.wurmonline.server;

import static org.mockito.Mockito.mock;

public class Servers {
    @SuppressWarnings("WeakerAccess")
    public static ServerEntry localServer;
    private static boolean isTestServer = false;
    private static boolean isPVPServer = false;
    private static boolean isEpicServer = false;
    private static boolean isHomeServer = true;

    static {
        localServer = mock(ServerEntry.class);
        setHostileServer(false);
    }

    public static boolean isThisATestServer() {
        return isTestServer;
    }
    public static boolean isThisAHomeServer() {
        return isHomeServer;
    }
    public static boolean isThisAPvpServer() {
        return isPVPServer;
    }
    public static boolean isThisAEpicServer() {
        return isEpicServer;
    }

    public static int getLocalServerId() {
        return 1;
    }

    public static void setTestServer(boolean value) {
        isTestServer = value;
    }

    public static void setHostileServer(boolean value) {
        isPVPServer = value;
        isEpicServer = value;
        isHomeServer = !value;

        localServer.PVPSERVER = isPVPServer;
        localServer.EPIC = isEpicServer;
        localServer.HOMESERVER = isHomeServer;
    }
}
