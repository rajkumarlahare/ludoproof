package com.ludoproof.game

import java.util.Locale

/**
 * Presentation-only mapping from authoritative Ludo color to team identity.
 *
 * Pet species is deliberately not part of this mapping: a RED player may choose
 * Cat, Duck, Goat or Dog and must always retain RED team markings. The base colors
 * match the board palette; darker sigil tints remain readable on white home slots.
 */
internal enum class LudoPawsTeamSigil {
    DIAMOND,
    LEAF,
    WAVE,
    SUN,
}

internal enum class LudoPawsTeamColor(
    val wireName: String,
    val argb: Int,
    val sigilArgb: Int,
    val glColor: FloatArray,
    val sigil: LudoPawsTeamSigil,
) {
    RED(
        wireName = "RED",
        argb = 0xFFF1252F.toInt(),
        sigilArgb = 0xFFC91D27.toInt(),
        glColor = floatArrayOf(241f / 255f, 37f / 255f, 47f / 255f, 1f),
        sigil = LudoPawsTeamSigil.DIAMOND,
    ),
    GREEN(
        wireName = "GREEN",
        argb = 0xFF00A950.toInt(),
        sigilArgb = 0xFF007A3A.toInt(),
        glColor = floatArrayOf(0f, 169f / 255f, 80f / 255f, 1f),
        sigil = LudoPawsTeamSigil.LEAF,
    ),
    YELLOW(
        wireName = "YELLOW",
        argb = 0xFFFFD81B.toInt(),
        sigilArgb = 0xFF9A7410.toInt(),
        glColor = floatArrayOf(1f, 216f / 255f, 27f / 255f, 1f),
        sigil = LudoPawsTeamSigil.SUN,
    ),
    BLUE(
        wireName = "BLUE",
        argb = 0xFF3097D7.toInt(),
        sigilArgb = 0xFF12689F.toInt(),
        glColor = floatArrayOf(48f / 255f, 151f / 255f, 215f / 255f, 1f),
        sigil = LudoPawsTeamSigil.WAVE,
    );

    companion object {
        fun from(color: String?): LudoPawsTeamColor? =
            when (color?.trim()?.uppercase(Locale.ROOT)) {
                "RED" -> RED
                "GREEN" -> GREEN
                "YELLOW" -> YELLOW
                "BLUE" -> BLUE
                else -> null
            }
    }
}

internal object LudoPawsTeamIdentityPolicy {
    fun resolve(color: String?): LudoPawsTeamColor? =
        LudoPawsTeamColor.from(color)
}
