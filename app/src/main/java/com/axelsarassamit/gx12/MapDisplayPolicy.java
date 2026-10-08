package com.axelsarassamit.gx12;

/** Automatic selection never requires bike configuration just to use a phone map. */
public final class MapDisplayPolicy {
    private MapDisplayPolicy() { }
    public static boolean tryBike(boolean permission, boolean bluetoothOn, boolean savedBike, boolean savedAccess) {
        return permission && bluetoothOn && savedBike && savedAccess;
    }


    /** Xiaomi HyperOS may route adjacent map tasks to an unavailable virtual display. */
    public static boolean useAdjacentPhoneMap(String manufacturer, String brand) {
        return !isXiaomiFamily(manufacturer) && !isXiaomiFamily(brand);
    }

    private static boolean isXiaomiFamily(String value) {
        if (value == null) return false;
        return value.equalsIgnoreCase("Xiaomi") || value.equalsIgnoreCase("Redmi")
            || value.equalsIgnoreCase("POCO");
    }
}
