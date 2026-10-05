package com.axelsarassamit.gx12;

/** Automatic selection never requires bike configuration just to use a phone map. */
public final class MapDisplayPolicy {
    private MapDisplayPolicy() { }
    public static boolean tryBike(boolean permission, boolean bluetoothOn, boolean savedBike, boolean savedAccess) {
        return permission && bluetoothOn && savedBike && savedAccess;
    }
}
