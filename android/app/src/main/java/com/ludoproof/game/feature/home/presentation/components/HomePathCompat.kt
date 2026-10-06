package com.ludoproof.game.ui.home

import android.graphics.Path

internal fun Path.quadraticTo(
    controlX: Float,
    controlY: Float,
    endX: Float,
    endY: Float,
) {
    quadTo(controlX, controlY, endX, endY)
}
