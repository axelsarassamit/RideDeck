package com.axelsarassamit.gx12;

import org.junit.Test;
import static org.junit.Assert.*;

public class MapDisplayPolicyTest {
    @Test public void XiaomiFamilyUsesNormalMapLaunchToAvoidHiddenVirtualDisplay() {
        assertFalse(MapDisplayPolicy.useAdjacentPhoneMap("Xiaomi", "Xiaomi"));
        assertFalse(MapDisplayPolicy.useAdjacentPhoneMap("Xiaomi", "Redmi"));
        assertFalse(MapDisplayPolicy.useAdjacentPhoneMap("Xiaomi", "POCO"));
    }

    @Test public void otherBrandsCanRequestAdjacentMapLaunch() {
        assertTrue(MapDisplayPolicy.useAdjacentPhoneMap("Google", "Pixel"));
        assertTrue(MapDisplayPolicy.useAdjacentPhoneMap("Samsung", "Samsung"));
    }
}
