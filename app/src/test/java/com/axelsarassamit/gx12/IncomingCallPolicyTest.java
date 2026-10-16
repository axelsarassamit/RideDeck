package com.axelsarassamit.gx12;
import org.junit.Test;import static org.junit.Assert.*;
public class IncomingCallPolicyTest {
 @Test public void standardIncomingCallIsRecognised(){assertTrue(IncomingCallPolicy.incoming(1,false,false,false));}
 @Test public void ongoingAndScreeningNeverAnnounce(){assertFalse(IncomingCallPolicy.incoming(2,true,true,false));assertFalse(IncomingCallPolicy.incoming(3,true,true,false));}
 @Test public void legacyCallNeedsRingingEvidence(){assertTrue(IncomingCallPolicy.incoming(0,true,false,false));assertTrue(IncomingCallPolicy.incoming(0,false,true,false));assertFalse(IncomingCallPolicy.incoming(0,false,false,false));assertFalse(IncomingCallPolicy.incoming(0,true,true,true));}
 @Test public void numbersAndPrivateCallersAreNotSpoken(){for(String s:new String[]{"+46 (70) 123-4567","٠٨١٢٣٤","Unknown caller","Private number","Incoming call","tel:+123","sip:123@example.com","WhatsApp"})assertEquals(s,"",IncomingCallPolicy.callerName(s,"WhatsApp"));}
 @Test public void realNamesKeepTheirOwnAlphabet(){for(String s:new String[]{"Åsa","Alice Smith","李雷","คุณเอ"})assertEquals(s,s,IncomingCallPolicy.callerName(s,"Phone"));}
 @Test public void whitespaceAndControlsAreCleaned(){assertEquals("Alice Smith",IncomingCallPolicy.callerName("  Alice\n  Smith  ","Phone"));}
 @Test public void readOnlyEmailPackagesAreExplicit(){assertTrue(RidePreferences.readOnlyEmail("com.google.android.gm"));assertTrue(RidePreferences.readOnlyEmail("com.microsoft.office.outlook"));assertTrue(RidePreferences.readOnlyEmail("com.samsung.android.email.provider"));assertFalse(RidePreferences.readOnlyEmail("com.whatsapp"));}
}
