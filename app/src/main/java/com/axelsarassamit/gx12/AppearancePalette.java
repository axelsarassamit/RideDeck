package com.axelsarassamit.gx12;

/** Android-independent colours and mode resolution shared by the three apps. */
public final class AppearancePalette {
    private AppearancePalette() { }
    public static boolean dark(int mode, boolean systemDark) {
        return mode == 2 || (mode != 1 && systemDark);
    }
    public static int accent(int index, boolean dark) {
        int[] colours = dark
            ? new int[]{0xffc3f77a,0xff90c8ff,0xffff8340,0xffeef2f6,0xff6284ff}
            : new int[]{0xff466b10,0xff126491,0xff924000,0xff465467,0xff3554bd};
        return colours[Math.max(0,Math.min(colours.length-1,index))];
    }
    public static int background(boolean dark) { return dark ? 0xff0b1018 : 0xfff5f8fc; }
    public static int panel(boolean dark) { return dark ? 0xff141e28 : 0xffffffff; }
    public static int raised(boolean dark) { return dark ? 0xff202d3d : 0xffe8edf5; }
    public static int line(boolean dark) { return dark ? 0xff2b3a50 : 0xffc2ccda; }
    public static int text(boolean dark) { return dark ? 0xfff5f8fc : 0xff0b1018; }
    public static int muted(boolean dark) { return dark ? 0xffa3b2c9 : 0xff53637a; }
    public static int danger(boolean dark) { return dark ? 0xffff8585 : 0xffb4232a; }
    public static int onAccent(boolean dark) { return dark ? 0xff0b1018 : 0xffffffff; }
}
