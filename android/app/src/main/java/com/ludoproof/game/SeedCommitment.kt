package com.ludoproof.game

import java.security.MessageDigest
import java.security.SecureRandom

data class PreparedRoll(
    val clientSeed: String,
    val clientCommitment: String,
)

object SeedCommitment {
    fun prepare(): PreparedRoll {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val seed = bytes.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
        val commitment = sha256Hex(
            "entronex:v4:client-commit:$seed",
        )
        return PreparedRoll(seed, commitment)
    }

    fun sha256Hex(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }
}
