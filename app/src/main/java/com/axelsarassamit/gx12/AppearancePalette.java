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
            : new int[]{0xff466b10,0xff126491,0xff924000,0xff465467,0xff0865d8};
        return colours[Math.max(0,Math.min(colours.length-1,index))];
    }
    public static int background(boolean dark) { return dark ? 0xff081422 : 0xfff4f7fb; }
    public static int panel(boolean dark) { return dark ? 0xff101c2e : 0xffffffff; }
    public static int raised(boolean dark) { return dark ? 0xff1a2a40 : 0xffe6edf7; }
    public static int line(boolean dark) { return dark ? 0xff2d405a : 0xffbeccd8; }
    public static int text(boolean dark) { return dark ? 0xfff6f8fc : 0xff0a1018; }
    public static int muted(boolean dark) { return dark ? 0xffa0b0c8 : 0xff506278; }
    public static int danger(boolean dark) { return dark ? 0xffff8585 : 0xffb4232a; }
    public static int onAccent(boolean dark) { return dark ? 0xff0b1018 : 0xffffffff; }
}
