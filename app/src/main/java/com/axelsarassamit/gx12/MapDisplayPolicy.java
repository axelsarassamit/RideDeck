package com.axelsarassamit.gx12;

/** Avoid Xiaomi HyperOS routing adjacent map launches to a hidden virtual display. */
public final class MapDisplayPolicy {
    private MapDisplayPolicy() { }

    public static boolean useAdjacentPhoneMap(String manufacturer, String brand) {
        return !isXiaomiFamily(manufacturer) && !isXiaomiFamily(brand);
    }

    private static boolean isXiaomiFamily(String value) {
        if (value == null) return false;
        return value.equalsIgnoreCase("Xiaomi") || value.equalsIgnoreCase("Redmi")
            || value.equalsIgnoreCase("POCO");
    }
}
