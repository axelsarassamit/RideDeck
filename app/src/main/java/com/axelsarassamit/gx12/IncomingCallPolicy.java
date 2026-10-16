package com.axelsarassamit.gx12;
/** Conservative call classification and speech policy, independent of Android or caller app. */
public final class IncomingCallPolicy {
    private IncomingCallPolicy() { }
    public static boolean incoming(int type, boolean answerAction, boolean fullscreen, boolean chronometer) {
        if (type != 0) return type == 1;
        return !chronometer && (answerAction || fullscreen);
    }
    public static String callerName(String candidate, String appName) {
        if (candidate == null) return "";
        String name=candidate.replaceAll("[\\p{Cntrl}]", " ").trim().replaceAll("\\s+", " ");
        String lower=name.toLowerCase(java.util.Locale.ROOT);
        if (name.isEmpty() || name.length()>100 || name.equalsIgnoreCase(appName) ||
            lower.matches(".*(incoming|ongoing|missed|unknown|private|restricted|unavailable|phone call|video call|voice call|calling|unbekannt|inkommande|okänd).*")) return "";
        if (lower.startsWith("tel:") || lower.startsWith("sip:") || !name.codePoints().anyMatch(Character::isLetter)) return "";
        return name;
    }
    public static String heading(boolean incoming, int type) { return incoming ? "Incoming call" : type==2 ? "Call in progress" : "Call"; }
}
