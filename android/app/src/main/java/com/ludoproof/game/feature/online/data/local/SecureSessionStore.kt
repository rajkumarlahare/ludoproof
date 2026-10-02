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

data class PlayerSession(
    val matchId: String,
    val playerId: String,
    val playerToken: String,
    val modeWire: String =
        GameMode.ONLINE.wireValue,
)

class SecureSessionStore(
    private val context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun save(value: PlayerSession) {
        require(MATCH_ID.matches(value.matchId))
        require(value.playerId.isNotBlank())
        require(PLAYER_TOKEN.matches(value.playerToken))
        require(
            GameMode
                .fromWireValue(
                    value.modeWire,
                )
                ?.isRemote ==
                true,
        )

        val cipher =
            Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey(),
        )
        cipher.updateAAD(
            LocalSecretBinding.playerSessionAad(
                value.matchId,
                value.playerId,
            ),
        )
        val encrypted =
            cipher.doFinal(
                value.playerToken
                    .toByteArray(Charsets.UTF_8),
            )

        val persisted =
            prefs.edit()
            .putInt(
                KEY_FORMAT_VERSION,
                LocalSecretBinding.FORMAT_VERSION,
            )
            .putString(KEY_MATCH_ID, value.matchId)
            .putString(KEY_PLAYER_ID, value.playerId)
            .putString(
                KEY_GAME_MODE,
                value.modeWire,
            )
            .putString(
                KEY_IV,
                Base64.encodeToString(
                    cipher.iv,
                    Base64.NO_WRAP,
                ),
            )
            .putString(
                KEY_TOKEN_CIPHERTEXT,
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP,
                ),
            )
            .commit()

        check(persisted) {
            "Player session could not be durably persisted"
        }
        legacyPrefs().edit().clear().apply()
    }

    fun load(): PlayerSession? {
        val encrypted = loadEncrypted()
        if (encrypted != null) {
            return encrypted
        }
        return migrateLegacy()
    }

    fun clear() {
        prefs.edit().clear().apply()
        legacyPrefs().edit().clear().apply()
    }

    private fun loadEncrypted(): PlayerSession? {
        val matchId =
            prefs.getString(KEY_MATCH_ID, null)
                ?: return null
        val playerId =
            prefs.getString(KEY_PLAYER_ID, null)
                ?: return null
        val modeWire =
            prefs.getString(
                KEY_GAME_MODE,
                GameMode.ONLINE.wireValue,
            )
                ?: GameMode.ONLINE.wireValue
        val mode =
            GameMode
                .fromWireValue(
                    modeWire,
                )
                ?.takeIf {
                    it.isRemote
                }
                ?: GameMode.ONLINE
        val ivText =
            prefs.getString(KEY_IV, null)
                ?: return null
        val ciphertextText =
            prefs.getString(
                KEY_TOKEN_CIPHERTEXT,
                null,
            ) ?: return null

        if (
            !MATCH_ID.matches(matchId) ||
            playerId.isBlank()
        ) {
            clear()
            return null
        }

        val formatVersion =
            prefs.getInt(
                KEY_FORMAT_VERSION,
                1,
            )

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
            if (
                formatVersion >=
                LocalSecretBinding.FORMAT_VERSION
            ) {
                cipher.updateAAD(
                    LocalSecretBinding.playerSessionAad(
                        matchId,
                        playerId,
                    ),
                )
            }
            val playerToken =
                cipher.doFinal(
                    Base64.decode(
                        ciphertextText,
                        Base64.NO_WRAP,
                    ),
                )
                    .toString(Charsets.UTF_8)

            if (!PLAYER_TOKEN.matches(playerToken)) {
                clear()
                null
            } else {
                val value =
                    PlayerSession(
                        matchId = matchId,
                        playerId = playerId,
                        playerToken = playerToken,
                        modeWire =
                            mode.wireValue,
                    )
                if (
                    formatVersion <
                    LocalSecretBinding.FORMAT_VERSION
                ) {
                    save(value)
                }
                value
            }
        } catch (_: Exception) {
            clear()
            null
        }
    }

    private fun migrateLegacy(): PlayerSession? {
        val legacy = legacyPrefs()
        val matchId =
            legacy.getString(
                KEY_MATCH_ID,
                null,
            ) ?: return null
        val playerId =
            legacy.getString(
                KEY_PLAYER_ID,
                null,
            ) ?: return null
        val playerToken =
            legacy.getString(
                LEGACY_PLAYER_TOKEN,
                null,
            ) ?: return null

        if (
            !MATCH_ID.matches(matchId) ||
            playerId.isBlank() ||
            !PLAYER_TOKEN.matches(playerToken)
        ) {
            legacy.edit().clear().apply()
            return null
        }

        val session =
            PlayerSession(
                matchId = matchId,
                playerId = playerId,
                playerToken = playerToken,
                modeWire =
                    GameMode.ONLINE
                        .wireValue,
            )
        return try {
            save(session)
            session
        } catch (_: Exception) {
            legacy.edit().clear().apply()
            null
        }
    }

    private fun legacyPrefs() =
        context.getSharedPreferences(
            LEGACY_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

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
            "ludoproof_secure_session"
        const val LEGACY_PREFS_NAME =
            "ludoproof_session"
        const val ANDROID_KEYSTORE =
            "AndroidKeyStore"
        const val KEY_ALIAS =
            "ludoproof_player_session_v1"
        const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        const val KEY_FORMAT_VERSION =
            "formatVersion"
        const val KEY_MATCH_ID = "matchId"
        const val KEY_PLAYER_ID = "playerId"
        const val KEY_GAME_MODE = "gameMode"
        const val LEGACY_PLAYER_TOKEN =
            "playerToken"
        const val KEY_IV = "iv"
        const val KEY_TOKEN_CIPHERTEXT =
            "playerTokenCiphertext"

        val MATCH_ID =
            Regex("^LP[A-Z2-9]{8}$")
        val PLAYER_TOKEN =
            Regex("^lp_[A-Za-z0-9_-]{32,}$")
    }
}
