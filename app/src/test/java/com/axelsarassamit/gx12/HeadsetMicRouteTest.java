package com.axelsarassamit.gx12;
import android.media.AudioDeviceInfo;
import org.junit.Test;
import static org.junit.Assert.*;
public class HeadsetMicRouteTest {
 @Test public void supportsWiredAndUsbHeadsets() {
  assertTrue(HeadsetMicRoute.headsetInput(AudioDeviceInfo.TYPE_WIRED_HEADSET,26));
  assertTrue(HeadsetMicRoute.headsetInput(AudioDeviceInfo.TYPE_USB_HEADSET,26));
  assertTrue(HeadsetMicRoute.headsetInput(AudioDeviceInfo.TYPE_USB_DEVICE,26));
 }
 @Test public void doesNotMistakeOutputOnlyDevicesForHeadsetMicrophones() {
  assertFalse(HeadsetMicRoute.headsetInput(AudioDeviceInfo.TYPE_WIRED_HEADPHONES,36));
  assertFalse(HeadsetMicRoute.headsetInput(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,36));
  assertFalse(HeadsetMicRoute.headsetInput(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,36));
  assertFalse(HeadsetMicRoute.headsetInput(AudioDeviceInfo.TYPE_BUILTIN_MIC,36));
 }
 @Test public void acceptsCallAudio() { assertTrue(HeadsetMicRoute.bluetooth(AudioDeviceInfo.TYPE_BLUETOOTH_SCO,26)); }
 @Test public void rejectsMusicOnlyAndPhoneMic() {
  assertFalse(HeadsetMicRoute.bluetooth(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,35));
  assertFalse(HeadsetMicRoute.bluetooth(AudioDeviceInfo.TYPE_BUILTIN_MIC,35));
 }
 @Test public void bleNeedsModernAndroid() {
  assertTrue(HeadsetMicRoute.bluetooth(AudioDeviceInfo.TYPE_BLE_HEADSET,31));
  assertFalse(HeadsetMicRoute.bluetooth(AudioDeviceInfo.TYPE_BLE_HEADSET,30));
 }
}
