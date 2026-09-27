package com.ludoproof.game

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class PendingRollSecret(
    val matchId: String,
    val clientSeed: String,
    val clientCommitment: String,
)

class PendingRollStore(
    private val context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun save(value: PendingRollSecret) {
        require(MATCH_ID.matches(value.matchId))
        require(DIGEST.matches(value.clientSeed))
        require(DIGEST.matches(value.clientCommitment))

        val cipher =
            Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey(),
        )
        val encrypted =
            cipher.doFinal(
                value.clientSeed
                    .toByteArray(Charsets.UTF_8),
            )

        val persisted =
            prefs.edit()
            .putString(KEY_MATCH_ID, value.matchId)
            .putString(
                KEY_COMMITMENT,
                value.clientCommitment,
            )
            .putString(
                KEY_IV,
                Base64.encodeToString(
                    cipher.iv,
                    Base64.NO_WRAP,
                ),
            )
            .putString(
                KEY_SEED_CIPHERTEXT,
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP,
                ),
            )
            .commit()

        check(persisted) {
            "Pending roll seed could not be durably persisted"
        }
    }

    fun load(): PendingRollSecret? {
        val matchId =
            prefs.getString(KEY_MATCH_ID, null)
                ?: return null
        val commitment =
            prefs.getString(KEY_COMMITMENT, null)
                ?: return null
        val ivText =
            prefs.getString(KEY_IV, null)
                ?: return null
        val ciphertextText =
            prefs.getString(
                KEY_SEED_CIPHERTEXT,
                null,
            ) ?: return null

        if (
            !MATCH_ID.matches(matchId) ||
            !DIGEST.matches(commitment)
        ) {
            clear()
            return null
        }

        return try {
            val cipher =
                Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(
                    128,
                    Base64.decode(
                        ivText,
                        Base64.NO_WRAP,
                    ),
                ),
            )
            val seed =
                cipher.doFinal(
                    Base64.decode(
                        ciphertextText,
                        Base64.NO_WRAP,
                    ),
                )
                    .toString(Charsets.UTF_8)

            if (!DIGEST.matches(seed)) {
                clear()
                null
            } else {
                PendingRollSecret(
                    matchId = matchId,
                    clientSeed = seed,
                    clientCommitment = commitment,
                )
            }
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_MATCH_ID)
            .remove(KEY_COMMITMENT)
            .remove(KEY_IV)
            .remove(KEY_SEED_CIPHERTEXT)
            .apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore =
            KeyStore.getInstance(
                ANDROID_KEYSTORE,
            ).apply {
                load(null)
            }

        val existing =
            keyStore.getKey(
                KEY_ALIAS,
                null,
            ) as? SecretKey
        if (existing != null) {
            return existing
        }

        val generator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE,
            )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM,
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE,
                )
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME =
            "ludoproof_pending_roll"
        const val ANDROID_KEYSTORE =
            "AndroidKeyStore"
        const val KEY_ALIAS =
            "ludoproof_pending_seed_v1"
        const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        const val KEY_MATCH_ID = "matchId"
        const val KEY_COMMITMENT =
            "clientCommitment"
        const val KEY_IV = "iv"
        const val KEY_SEED_CIPHERTEXT =
            "seedCiphertext"

        val DIGEST =
            Regex("^[0-9a-f]{64}$")
        val MATCH_ID =
            Regex("^LP[A-Z2-9]{8}$")
    }
}
