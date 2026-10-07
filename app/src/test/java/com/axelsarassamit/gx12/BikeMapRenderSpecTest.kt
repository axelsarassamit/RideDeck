package com.axelsarassamit.gx12
import org.junit.Assert.*
import org.junit.Test
class BikeMapRenderSpecTest {
 @Test fun xmaxMatchesLandscapeAndLogicalScale() {
  val s=BikeMapRenderSpec(480,234)
  assertEquals(960,s.renderWidth); assertEquals(468,s.renderHeight); assertEquals(192,s.densityDpi)
  assertEquals(800,s.renderWidth*160/s.densityDpi)
 }
 @Test fun otherSupportedDashPreservesAspect() {
  val s=BikeMapRenderSpec(480,240)
  assertEquals(960,s.renderWidth); assertEquals(480,s.renderHeight)
 }
 @Test(expected=IllegalArgumentException::class) fun unsupportedDashRejected() { BikeMapRenderSpec(800,480) }
}
