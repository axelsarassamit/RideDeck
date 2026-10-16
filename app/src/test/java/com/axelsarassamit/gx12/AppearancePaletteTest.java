package com.axelsarassamit.gx12;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AppearancePaletteTest {
    @Test public void systemFollowsBothDirectionsAndOverridesStayFixed() {
        assertFalse(AppearancePalette.dark(0,false));assertTrue(AppearancePalette.dark(0,true));
        assertFalse(AppearancePalette.dark(1,true));assertFalse(AppearancePalette.dark(1,false));
        assertTrue(AppearancePalette.dark(2,false));assertTrue(AppearancePalette.dark(2,true));
    }
    @Test public void allThemesKeepReadableTextAndPrimaryButtonContrast() {
        for(boolean dark:new boolean[]{false,true}) {
            assertTrue(contrast(AppearancePalette.danger(dark),AppearancePalette.panel(dark))>=4.5);
            int bg=AppearancePalette.background(dark),panel=AppearancePalette.panel(dark);
            assertTrue(contrast(AppearancePalette.text(dark),bg)>=7);
            assertTrue(contrast(AppearancePalette.muted(dark),panel)>=4.5);
            for(int i=0;i<5;i++) {
                int accent=AppearancePalette.accent(i,dark);
                assertTrue("accent "+i+" dark="+dark,contrast(accent,panel)>=4.5);
                assertTrue(contrast(accent,AppearancePalette.onAccent(dark))>=4.5);
            }
        }
    }
    @Test public void legacyThemeIndicesRemainBounded() {
        assertEquals(AppearancePalette.accent(0,true),AppearancePalette.accent(-1,true));
        assertEquals(AppearancePalette.accent(4,false),AppearancePalette.accent(50,false));
    }
    private static double contrast(int a,int b) {double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private static double luminance(int c) {return .2126*channel(c>>16)+.7152*channel(c>>8)+.0722*channel(c);}
    private static double channel(int v) {double c=(v&255)/255.;return c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4);}
}
