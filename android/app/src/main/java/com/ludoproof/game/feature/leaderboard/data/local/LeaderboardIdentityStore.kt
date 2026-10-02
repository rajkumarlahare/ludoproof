package com.ludoproof.game.feature.leaderboard.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class LeaderboardCredential(
    val profileId: String,
    val profileToken: String,
    val registrationRequestId: String,
)

class LeaderboardIdentityStore(
    context: Context,
) {
    private val prefs =
        context
            .applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            )

    @Synchronized
    fun registrationRequestId():
        String {
        loadRegistrationRequestId()
            ?.let {
                return it
            }

        val generated =
            UUID.randomUUID()
                .toString()
                .lowercase(
                    Locale.ROOT,
                )

        persistRegistrationRequestId(
            generated,
        )
        prefs.edit()
            .remove(
                LEGACY_KEY_PROFILE_ID,
            )
            .apply()
        return generated
    }

    @Synchronized
    fun save(
        credential: LeaderboardCredential,
    ) {
        require(
            PROFILE_ID.matches(
                credential.profileId,
            ),
        )
        require(
            PROFILE_TOKEN.matches(
                credential.profileToken,
            ),
        )
        require(
            UUID_V4.matches(
                credential.registrationRequestId,
            ),
        )

        val protectedRequestId =
            registrationRequestId()
        require(
            protectedRequestId ==
                credential.registrationRequestId,
        ) {
            "Leaderboard registration identity does not match this device"
        }

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION,
            )
        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey(),
        )
        cipher.updateAAD(
            credentialAad(
                credential.profileId,
                protectedRequestId,
            ),
        )
        val ciphertext =
            cipher.doFinal(
                credential
                    .profileToken
                    .toByteArray(
                        Charsets.UTF_8,
                    ),
            )

        check(
            prefs.edit()
                .putString(
                    KEY_PROFILE_ID,
                    credential.profileId,
                )
                .putString(
                    KEY_TOKEN_IV,
                    Base64.encodeToString(
                        cipher.iv,
                        Base64.NO_WRAP,
                    ),
                )
                .putString(
                    KEY_TOKEN_CIPHERTEXT,
                    Base64.encodeToString(
                        ciphertext,
                        Base64.NO_WRAP,
                    ),
                )
                .commit(),
        ) {
            "Leaderboard credential could not be persisted"
        }
    }

    @Synchronized
    fun load():
        LeaderboardCredential? {
        val profileId =
            prefs.getString(
                KEY_PROFILE_ID,
                null,
            )
                ?: return null
        val requestId =
            loadRegistrationRequestId()
                ?: run {
                    clearCredentialOnly()
                    return null
                }
        val ivText =
            prefs.getString(
                KEY_TOKEN_IV,
                null,
            )
                ?: return null
        val ciphertextText =
            prefs.getString(
                KEY_TOKEN_CIPHERTEXT,
                null,
            )
                ?: return null

        if (
            !PROFILE_ID.matches(
                profileId,
            ) ||
            !UUID_V4.matches(
                requestId,
            )
        ) {
            clearCredentialOnly()
            return null
        }

        return try {
            val cipher =
                Cipher.getInstance(
                    TRANSFORMATION,
                )
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
            cipher.updateAAD(
                credentialAad(
                    profileId,
                    requestId,
                ),
            )
            val token =
                cipher.doFinal(
                    Base64.decode(
                        ciphertextText,
                        Base64.NO_WRAP,
                    ),
                )
                    .toString(
                        Charsets.UTF_8,
                    )

            if (
                !PROFILE_TOKEN.matches(
                    token,
                )
            ) {
                clearCredentialOnly()
                null
            } else {
                LeaderboardCredential(
                    profileId =
                        profileId,
                    profileToken =
                        token,
                    registrationRequestId =
                        requestId,
                )
            }
        } catch (_: Exception) {
            clearCredentialOnly()
            null
        }
    }

    fun clearCredentialOnly() {
        prefs.edit()
            .remove(
                KEY_PROFILE_ID,
            )
            .remove(
                KEY_TOKEN_IV,
            )
            .remove(
                KEY_TOKEN_CIPHERTEXT,
            )
            .apply()
    }

    private fun persistRegistrationRequestId(
        requestId: String,
    ) {
        require(
            UUID_V4.matches(
                requestId,
            ),
        )

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION,
            )
        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey(),
        )
        cipher.updateAAD(
            REGISTRATION_AAD,
        )

        val ciphertext =
            cipher.doFinal(
                requestId
                    .toByteArray(
                        Charsets.UTF_8,
                    ),
            )

        check(
            prefs.edit()
                .putString(
                    KEY_REGISTRATION_IV,
                    Base64.encodeToString(
                        cipher.iv,
                        Base64.NO_WRAP,
                    ),
                )
                .putString(
                    KEY_REGISTRATION_CIPHERTEXT,
                    Base64.encodeToString(
                        ciphertext,
                        Base64.NO_WRAP,
                    ),
                )
                .commit(),
        ) {
            "Leaderboard registration recovery secret could not be persisted"
        }
    }

    private fun loadRegistrationRequestId():
        String? {
        val ivText =
            prefs.getString(
                KEY_REGISTRATION_IV,
                null,
            )
                ?: return null
        val ciphertextText =
            prefs.getString(
                KEY_REGISTRATION_CIPHERTEXT,
                null,
            )
                ?: return null

        return try {
            val cipher =
                Cipher.getInstance(
                    TRANSFORMATION,
                )
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
            cipher.updateAAD(
                REGISTRATION_AAD,
            )
            val requestId =
                cipher.doFinal(
                    Base64.decode(
                        ciphertextText,
                        Base64.NO_WRAP,
                    ),
                )
                    .toString(
                        Charsets.UTF_8,
                    )
                    .trim()
                    .lowercase(
                        Locale.ROOT,
                    )

            requestId
                .takeIf {
                    UUID_V4.matches(
                        it,
                    )
                }
                ?: run {
                    clearAllIdentity()
                    null
                }
        } catch (_: Exception) {
            clearAllIdentity()
            null
        }
    }

    private fun clearAllIdentity() {
        prefs.edit()
            .clear()
            .apply()
    }

    private fun secretKey():
        SecretKey {
        val keyStore =
            KeyStore
                .getInstance(
                    ANDROID_KEYSTORE,
                )
                .apply {
                    load(
                        null,
                    )
                }

        val existing =
            keyStore.getKey(
                KEY_ALIAS,
                null,
            ) as? SecretKey
        if (
            existing !=
            null
        ) {
            return existing
        }

        val generator =
            KeyGenerator
                .getInstance(
                    KeyProperties
                        .KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE,
                )
        generator.init(
            KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT,
                )
                .setBlockModes(
                    KeyProperties
                        .BLOCK_MODE_GCM,
                )
                .setEncryptionPaddings(
                    KeyProperties
                        .ENCRYPTION_PADDING_NONE,
                )
                .setRandomizedEncryptionRequired(
                    true,
                )
                .build(),
        )
        return generator
            .generateKey()
    }

    private fun credentialAad(
        profileId: String,
        requestId: String,
    ):
        ByteArray =
        (
            "ludoproof:leaderboard-credential:v2:" +
                profileId +
                ":" +
                requestId
            )
            .toByteArray(
                Charsets.UTF_8,
            )

    private companion object {
        const val PREFS_NAME =
            "ludoproof_leaderboard_identity"
        const val ANDROID_KEYSTORE =
            "AndroidKeyStore"
        const val KEY_ALIAS =
            "ludoproof_leaderboard_identity_key_v2"
        const val TRANSFORMATION =
            "AES/GCM/NoPadding"
        const val KEY_PROFILE_ID =
            "profile_id_v2"
        const val LEGACY_KEY_PROFILE_ID =
            "profile_id_v1"
        const val KEY_REGISTRATION_IV =
            "registration_request_iv_v2"
        const val KEY_REGISTRATION_CIPHERTEXT =
            "registration_request_ciphertext_v2"
        const val KEY_TOKEN_IV =
            "profile_token_iv_v2"
        const val KEY_TOKEN_CIPHERTEXT =
            "profile_token_ciphertext_v2"

        val REGISTRATION_AAD =
            "ludoproof:leaderboard-registration:v2"
                .toByteArray(
                    Charsets.UTF_8,
                )

        val PROFILE_ID =
            Regex(
                "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            )
        val PROFILE_TOKEN =
            Regex(
                "^lpp_[A-Za-z0-9_-]{32,}$",
            )
        val UUID_V4 =
            Regex(
                "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            )
    }
}
