package com.axelsarassamit.gx12;

/** Android semantic actions: 0 is unspecified, 1 is Reply. */
final class ReplyActionPolicy {
    static int rank(int semanticAction, boolean freeForm, boolean hasIntent) {
        if (!freeForm || !hasIntent) return 0;
        if (semanticAction == 1) return 2;
        return semanticAction == 0 ? 1 : 0;
    }
}
