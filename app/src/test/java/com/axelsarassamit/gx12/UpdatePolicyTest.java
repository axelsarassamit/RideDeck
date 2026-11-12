package com.axelsarassamit.gx12;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class UpdatePolicyTest {
    @Test public void requiresUnambiguousNumericReleaseVersion() {
        assertEquals(1232, UpdatePolicy.versionCode("v0.12.32"));
        for (String value : new String[]{"v0.12.32-beta", "0.12.100", "0.12", "0..1", "0.12.32.extra", "-1.0.0"}) {
            try { UpdatePolicy.versionCode(value); fail(value); }
            catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void rejectsWrongEditionSignerVersionAndMissingSigner() {
        java.util.Set<String> signer = Collections.singleton("original");
        assertTrue(UpdatePolicy.candidate("standard", "standard", 1231, 1232, 1232, signer, signer));
        assertFalse(UpdatePolicy.candidate("standard", "yamaha", 1231, 1232, 1232, signer, signer));
        assertFalse(UpdatePolicy.candidate("standard", "standard", 1231, 1232, 1232, signer, Collections.singleton("other")));
        assertFalse(UpdatePolicy.candidate("standard", "standard", 1231, 1231, 1232, signer, signer));
        assertFalse(UpdatePolicy.candidate("standard", "standard", 1231, 1233, 1232, signer, signer));
        assertFalse(UpdatePolicy.candidate("standard", "standard", 1231, 1232, 1232, Collections.emptySet(), Collections.emptySet()));
    }
}
