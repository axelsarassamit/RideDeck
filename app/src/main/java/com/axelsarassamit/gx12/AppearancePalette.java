package com.axelsarassamit.gx12;

/** Android-independent colours and mode resolution shared by the three apps. */
public final class AppearancePalette {
    private AppearancePalette() { }
    public static boolean dark(int mode, boolean systemDark) {
        return mode == 2 || (mode != 1 && systemDark);
    }
    public static int accent(int index, boolean dark) {
        int[] colours = dark
            ? new int[]{0xffc3f77a,0xff90c8ff,0xffffc464,0xffeef2f6,0xff6c94ff}
            : new int[]{0xff466b10,0xff126491,0xff875400,0xff465467,0xff345cc4};
        return colours[Math.max(0,Math.min(colours.length-1,index))];
    }
    public static int background(boolean dark) { return dark ? 0xff0c111b : 0xfff2f5fa; }
    public static int panel(boolean dark) { return dark ? 0xff141d2b : 0xffffffff; }
    public static int raised(boolean dark) { return dark ? 0xff1c293b : 0xffe5ebf4; }
    public static int line(boolean dark) { return dark ? 0xff2b3a50 : 0xffc2ccda; }
    public static int text(boolean dark) { return dark ? 0xfff4f6fb : 0xff172237; }
    public static int muted(boolean dark) { return dark ? 0xffa3b2c9 : 0xff53637a; }
    public static int onAccent(boolean dark) { return dark ? 0xff0c111b : 0xffffffff; }
}
