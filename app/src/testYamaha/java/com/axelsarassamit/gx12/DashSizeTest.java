package com.axelsarassamit.gx12;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import app.pillion.core.NaviLiteDisplay;
public class DashSizeTest {
    @Test public void scooterUses234Rows() { assertEquals(234, NaviLiteDisplay.INSTANCE.forCcuPartNumber("006-B3952-01").getHeight()); }
    @Test public void otherNaviLiteDashUses240Rows() { assertEquals(240, NaviLiteDisplay.INSTANCE.forCcuPartNumber("other").getHeight()); }
}
