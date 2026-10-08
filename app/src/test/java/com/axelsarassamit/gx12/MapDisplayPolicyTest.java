package com.axelsarassamit.gx12;

import org.junit.Test;
import static org.junit.Assert.*;

public class MapDisplayPolicyTest {
    @Test public void configuredBikeCanBeTried() { assertTrue(MapDisplayPolicy.tryBike(true, true, true, true)); }
    @Test public void noBikeUsesPhone() { assertFalse(MapDisplayPolicy.tryBike(true, true, false, true)); }
    @Test public void noDisplaySetupUsesPhone() { assertFalse(MapDisplayPolicy.tryBike(true, true, true, false)); }
    @Test public void bluetoothOffOrPermissionDeniedUsesPhone() {
        assertFalse(MapDisplayPolicy.tryBike(true, false, true, true));
        assertFalse(MapDisplayPolicy.tryBike(false, true, true, true));
    }


    @Test public void xiaomiFamilyUsesNormalPhoneMapLaunch() {
        assertFalse(MapDisplayPolicy.useAdjacentPhoneMap("Xiaomi", "Xiaomi"));
        assertFalse(MapDisplayPolicy.useAdjacentPhoneMap("Xiaomi", "Redmi"));
        assertFalse(MapDisplayPolicy.useAdjacentPhoneMap("POCO", "POCO"));
    }

    @Test public void otherManufacturersKeepAdjacentPhoneMapLaunch() {
        assertTrue(MapDisplayPolicy.useAdjacentPhoneMap("Google", "Pixel"));
        assertTrue(MapDisplayPolicy.useAdjacentPhoneMap("Samsung", "Samsung"));
    }
}
