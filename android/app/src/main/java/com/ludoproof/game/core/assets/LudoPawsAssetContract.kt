package com.ludoproof.game.core.assets

internal object LudoPawsAssetContract {
    const val CATALOG_PATH =
        "ludo_paws/catalog.json"

    const val SINGLE_BITMAP_MAX_DECODED_BYTES =
        3 * 1024 * 1024
    const val ACTIVE_CHARACTER_ART_MAX_DECODED_BYTES =
        12 * 1024 * 1024

    const val PAWN_MAX_COMPRESSED_BYTES =
        120 * 1024
    const val PORTRAIT_MAX_COMPRESSED_BYTES =
        260 * 1024
    const val FULL_BODY_MAX_COMPRESSED_BYTES =
        480 * 1024
    const val EXPRESSION_MAX_COMPRESSED_BYTES =
        260 * 1024
    const val VOICE_MAX_COMPRESSED_BYTES =
        180 * 1024
    const val VOICE_MAX_DURATION_MS =
        2_500

    private val safeId =
        Regex(
            "^[a-z][a-z0-9_]{1,31}$",
        )

    val allowedExpressions:
        Set<String> =
        setOf(
            "happy",
            "sad",
            "angry",
            "nervous",
            "excited",
            "victory",
            "defeat",
        )

    val allowedVoiceCues:
        Set<String> =
        setOf(
            "six",
            "capture",
            "captured",
            "safe",
            "home",
            "frustrated",
            "third_six",
            "idle",
            "nervous",
            "victory",
            "defeat",
        )

    fun isSafeId(
        value: String,
    ): Boolean =
        safeId.matches(
            value,
        )

    fun characterRoot(
        characterId: String,
    ): String {
        requireSafeId(
            characterId,
        )
        return "ludo_paws/characters/$characterId"
    }

    fun pawnPath(
        characterId: String,
    ): String =
        characterRoot(
            characterId,
        ) +
            "/pawn.webp"

    fun portraitPath(
        characterId: String,
    ): String =
        characterRoot(
            characterId,
        ) +
            "/portrait.webp"

    fun fullBodyPath(
        characterId: String,
    ): String =
        characterRoot(
            characterId,
        ) +
            "/full_body.webp"

    fun expressionPath(
        characterId: String,
        expression: String,
    ): String {
        require(
            expression in
                allowedExpressions,
        ) {
            "Unsupported expression: $expression"
        }
        return characterRoot(
            characterId,
        ) +
            "/expressions/$expression.webp"
    }

    fun audioRoot(
        characterId: String,
    ): String {
        requireSafeId(
            characterId,
        )
        return "ludo_paws/audio/$characterId"
    }

    fun voicePath(
        characterId: String,
        cue: String,
        variant: Int,
    ): String {
        require(
            cue in
                allowedVoiceCues,
        ) {
            "Unsupported voice cue: $cue"
        }
        require(
            variant in
                1..99,
        ) {
            "Voice variant must be between 1 and 99"
        }
        return audioRoot(
            characterId,
        ) +
            "/" +
            cue +
            "_" +
            variant
                .toString()
                .padStart(
                    2,
                    '0',
                ) +
            ".ogg"
    }

    fun isSafeRuntimeAssetPath(
        path: String,
    ): Boolean {
        if (
            path.isBlank() ||
            path.startsWith('/') ||
            path.contains("..") ||
            path.contains('\\')
        ) {
            return false
        }

        return path.startsWith(
            "ludo_paws/",
        ) &&
            (
                path.endsWith(
                    ".webp",
                ) ||
                    path.endsWith(
                        ".ogg",
                    ) ||
                    path ==
                    CATALOG_PATH
                )
    }

    fun calculatePowerOfTwoSampleSize(
        width: Int,
        height: Int,
        targetMaxDimensionPx: Int,
    ): Int {
        require(
            width > 0 &&
                height > 0,
        ) {
            "Bitmap bounds must be positive"
        }
        require(
            targetMaxDimensionPx > 0,
        ) {
            "Target dimension must be positive"
        }

        val maxDimension =
            maxOf(
                width,
                height,
            )
        var sample =
            1
        while (
            maxDimension /
                (sample * 2) >=
            targetMaxDimensionPx
        ) {
            sample *=
                2
        }
        return sample
    }

    fun estimatedArgb8888Bytes(
        width: Int,
        height: Int,
        sampleSize: Int,
    ): Long {
        require(
            width > 0 &&
                height > 0,
        )
        require(
            sampleSize > 0,
        )
        val sampledWidth =
            maxOf(
                1,
                width /
                    sampleSize,
            )
        val sampledHeight =
            maxOf(
                1,
                height /
                    sampleSize,
            )
        return sampledWidth
            .toLong() *
            sampledHeight
                .toLong() *
            4L
    }

    private fun requireSafeId(
        value: String,
    ) {
        require(
            isSafeId(
                value,
            ),
        ) {
            "Unsafe Ludo Paws asset id: $value"
        }
    }
}
