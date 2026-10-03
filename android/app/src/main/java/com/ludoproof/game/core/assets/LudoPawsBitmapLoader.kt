package com.ludoproof.game.core.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

internal class LudoPawsBitmapLoader(
    context: Context,
) {
    private val assets =
        context.applicationContext
            .assets

    fun load(
        path: String,
        targetMaxDimensionPx: Int,
        maxDecodedBytes: Int =
            LudoPawsAssetContract
                .SINGLE_BITMAP_MAX_DECODED_BYTES,
    ): Bitmap? {
        if (
            !LudoPawsAssetContract
                .isSafeRuntimeAssetPath(
                    path,
                ) ||
            !path.endsWith(
                ".webp",
            ) ||
            targetMaxDimensionPx <= 0 ||
            maxDecodedBytes <= 0
        ) {
            return null
        }

        val bounds =
            BitmapFactory.Options().apply {
                inJustDecodeBounds =
                    true
            }

        val boundsRead =
            runCatching {
                assets.open(
                    path,
                ).use {
                        stream ->
                    BitmapFactory
                        .decodeStream(
                            stream,
                            null,
                            bounds,
                        )
                }
            }
                .isSuccess

        if (
            !boundsRead ||
            bounds.outWidth <= 0 ||
            bounds.outHeight <= 0
        ) {
            return null
        }

        var sampleSize =
            LudoPawsAssetContract
                .calculatePowerOfTwoSampleSize(
                    width =
                        bounds.outWidth,
                    height =
                        bounds.outHeight,
                    targetMaxDimensionPx =
                        targetMaxDimensionPx,
                )

        while (
            LudoPawsAssetContract
                .estimatedArgb8888Bytes(
                    width =
                        bounds.outWidth,
                    height =
                        bounds.outHeight,
                    sampleSize =
                        sampleSize,
                ) >
            maxDecodedBytes
        ) {
            sampleSize *=
                2
        }

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize =
                    sampleSize
                inPreferredConfig =
                    Bitmap.Config.ARGB_8888
            }

        return runCatching {
            assets.open(
                path,
            ).use {
                    stream ->
                BitmapFactory
                    .decodeStream(
                        stream,
                        null,
                        decodeOptions,
                    )
            }
        }
            .getOrNull()
    }
}
