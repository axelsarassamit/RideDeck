package com.axelsarassamit.gx12;
import android.app.*;import android.content.*;import android.os.*;import android.service.notification.StatusBarNotification;
import androidx.test.core.app.ActivityScenario;import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;import static org.junit.Assert.*;
public final class CallEmailTest {
 private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 private Notification notification(String name,int type){Bundle extras=new Bundle();extras.putInt("android.callType",type);extras.putParcelable("android.callPerson",new Person.Builder().setName(name).build());return new Notification.Builder(context(),"test-call").setSmallIcon(android.R.drawable.sym_call_incoming).setCategory(Notification.CATEGORY_CALL).setContentTitle(name).setContentText(type==1?"Incoming call":"Call in progress").addExtras(extras).build();}
 private StatusBarNotification item(String pkg,int id,Notification n){return new StatusBarNotification(pkg,pkg,id,"ridedeck-call-test",1000,0,0,n,android.os.Process.myUserHandle(),System.currentTimeMillis());}
 @Test public void compatibleCallNotificationsHaveNamesStatesAndCleanup(){
  Context c=context();try{
   for(String pkg:new String[]{"com.android.incallui","com.android.phone","com.whatsapp","jp.naver.line.android","com.facebook.orca","org.telegram.messenger","org.thoughtcrime.securesms","com.discord","com.microsoft.teams"}){
    StatusBarNotification incoming=item(pkg,701,notification("RideDeck test caller",1));GX12NotificationListener.consume(c,incoming);
    assertNotNull(pkg,GX12NotificationListener.activeCall);assertTrue(pkg,GX12NotificationListener.activeCall.incoming);assertEquals("RideDeck test caller",GX12NotificationListener.activeCall.callerName);
    GX12NotificationListener.consume(c,item(pkg,701,notification("RideDeck test caller",2)));assertFalse(pkg,GX12NotificationListener.activeCall.incoming);
    GX12NotificationListener.removeCall(c,incoming.getKey());assertNull(pkg,GX12NotificationListener.activeCall);
   }
  }finally{GX12NotificationListener.reconcileCalls(c,java.util.Collections.emptySet());}
 }
 @Test public void numberOnlyAndUnknownCallsHaveNoAnnouncementName(){Context c=context();try{for(String name:new String[]{"+46 70 1234567","Unknown caller"}){GX12NotificationListener.consume(c,item("com.android.phone",702,notification(name,1)));assertTrue(GX12NotificationListener.activeCall.incoming);assertEquals("",GX12NotificationListener.activeCall.callerName);}}finally{GX12NotificationListener.reconcileCalls(c,java.util.Collections.emptySet());}}
 @Test public void snapshotDropsAStaleCallerWithoutRemovalCallback(){Context c=context();try{GX12NotificationListener.consume(c,item("com.whatsapp",703,notification("Alice",1)));assertNotNull(GX12NotificationListener.activeCall);GX12NotificationListener.reconcileCalls(c,java.util.Collections.emptySet());assertNull(GX12NotificationListener.activeCall);}finally{GX12NotificationListener.reconcileCalls(c,java.util.Collections.emptySet());}}
 @Test public void emailCannotExposeAnInlineReplyOrAReaderReplyButton() throws Exception {
  Context c=context();android.content.SharedPreferences prefs=RidePreferences.prefs(c);boolean existed=prefs.contains("message_apps");java.util.Set<String> saved=new java.util.HashSet<>(RidePreferences.selectedMessages(c));
  try{
   prefs.edit().putStringSet("message_apps",new java.util.HashSet<>(java.util.Arrays.asList("com.google.android.gm","com.microsoft.office.outlook","com.samsung.android.email.provider"))).commit();GX12NotificationListener.clearPreviews();
   for(String pkg:new String[]{"com.microsoft.office.outlook","com.samsung.android.email.provider","com.google.android.gm"}){
    PendingIntent intent=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_MUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    android.app.RemoteInput input=new android.app.RemoteInput.Builder("test-text").setLabel("Reply").build();
    Notification n=new Notification.Builder(c,"test-email").setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle("RideDeck email test").setContentText("Read-only email preview").addAction(new Notification.Action.Builder(null,"Reply",intent).addRemoteInput(input).setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY).build()).build();
    GX12NotificationListener.consume(c,item(pkg,704,n));GX12NotificationListener.NotificationPreview preview=GX12NotificationListener.selectedPreviews(c).get(0);assertNull(pkg,preview.reply);
   }
   assertFalse(c.getSystemService(KeyguardManager.class).isKeyguardLocked());
   try(ActivityScenario<Activity> activity=ActivityScenario.launch(new Intent(c,MainActivity.class))){activity.onActivity(a->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showFullMessage");m.setAccessible(true);m.invoke(a);}catch(Exception e){throw new AssertionError(e);}});Thread.sleep(700);
    androidx.test.uiautomator.UiDevice device=androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    assertTrue(device.hasObject(androidx.test.uiautomator.By.text("Read-only email preview")));assertFalse(device.hasObject(androidx.test.uiautomator.By.text("Reply")));assertTrue(device.hasObject(androidx.test.uiautomator.By.text("Read aloud")));device.pressBack();
   }
  }finally{if(existed)prefs.edit().putStringSet("message_apps",saved).commit();else prefs.edit().remove("message_apps").commit();GX12NotificationListener.clearPreviews();}
 }

 @Test public void incomingPanelClearsAfterCallDisappears() throws Exception {
  Context c=context();try{
   Notification incoming=notification("RideDeck test caller",1);incoming.extras.putCharSequence(Notification.EXTRA_TITLE,"Incoming call");
   GX12NotificationListener.consume(c,item("com.android.phone",705,incoming));
   try(ActivityScenario<Activity> activity=ActivityScenario.launch(new Intent(c,MainActivity.class))){
    Thread.sleep(700);androidx.test.uiautomator.UiDevice device=androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    assertTrue(device.hasObject(androidx.test.uiautomator.By.textContains("Incoming call")));
    assertTrue(device.hasObject(androidx.test.uiautomator.By.textContains("RideDeck test caller")));
    activity.onActivity(a->{GX12NotificationListener.reconcileCalls(c,java.util.Collections.emptySet());try{java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod("refreshCallPanel");method.setAccessible(true);method.invoke(a);}catch(Exception e){throw new AssertionError(e);}});
    Thread.sleep(300);assertFalse(device.hasObject(androidx.test.uiautomator.By.textContains("Incoming call")));
   }
  }finally{GX12NotificationListener.reconcileCalls(c,java.util.Collections.emptySet());}
 }
}
