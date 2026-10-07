package com.axelsarassamit.gx12
/** Negotiated navigation area, independent of phone metrics. */
data class BikeMapRenderSpec(val width: Int, val height: Int, val densityDpi: Int = 192) {
    init { require(width == 480 && height in listOf(234, 240)) { "Unsupported bike display" } }
    val renderWidth = width * 2
    val renderHeight = height * 2
    init { require(densityDpi in listOf(160, 192, 240)) { "Unsupported map layout size" } }
}
