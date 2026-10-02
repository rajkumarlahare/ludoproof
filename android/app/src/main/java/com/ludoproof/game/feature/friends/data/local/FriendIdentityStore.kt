package com.ludoproof.game.feature.friends.data.local

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

data class FriendCredential(
    val friendId: String,
    val friendToken: String,
    val registrationRequestId: String,
)

class FriendIdentityStore(
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
        val existing =
            prefs.getString(
                KEY_REGISTRATION_REQUEST_ID,
                null,
            )
                ?.trim()
                ?.lowercase(
                    Locale.ROOT,
                )

        if (
            existing !=
                null &&
            UUID_V4.matches(
                existing,
            )
        ) {
            return existing
        }

        val generated =
            UUID.randomUUID()
                .toString()
                .lowercase(
                    Locale.ROOT,
                )

        check(
            prefs.edit()
                .putString(
                    KEY_REGISTRATION_REQUEST_ID,
                    generated,
                )
                .commit(),
        ) {
            "Friend registration request could not be persisted"
        }

        return generated
    }

    @Synchronized
    fun save(
        credential: FriendCredential,
    ) {
        require(
            FRIEND_ID.matches(
                credential.friendId,
            ),
        )
        require(
            FRIEND_TOKEN.matches(
                credential.friendToken,
            ),
        )
        require(
            UUID_V4.matches(
                credential.registrationRequestId,
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
            aad(
                credential.friendId,
                credential.registrationRequestId,
            ),
        )
        val ciphertext =
            cipher.doFinal(
                credential
                    .friendToken
                    .toByteArray(
                        Charsets.UTF_8,
                    ),
            )

        check(
            prefs.edit()
                .putString(
                    KEY_FRIEND_ID,
                    credential.friendId,
                )
                .putString(
                    KEY_REGISTRATION_REQUEST_ID,
                    credential.registrationRequestId,
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
                        ciphertext,
                        Base64.NO_WRAP,
                    ),
                )
                .commit(),
        ) {
            "Friend credential could not be persisted"
        }
    }

    @Synchronized
    fun load():
        FriendCredential? {
        val friendId =
            prefs.getString(
                KEY_FRIEND_ID,
                null,
            )
                ?: return null
        val requestId =
            prefs.getString(
                KEY_REGISTRATION_REQUEST_ID,
                null,
            )
                ?: return null
        val ivText =
            prefs.getString(
                KEY_IV,
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
            !FRIEND_ID.matches(
                friendId,
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
                aad(
                    friendId,
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
                !FRIEND_TOKEN.matches(
                    token,
                )
            ) {
                clearCredentialOnly()
                null
            } else {
                FriendCredential(
                    friendId =
                        friendId,
                    friendToken =
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
                KEY_FRIEND_ID,
            )
            .remove(
                KEY_IV,
            )
            .remove(
                KEY_TOKEN_CIPHERTEXT,
            )
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

    private fun aad(
        friendId: String,
        requestId: String,
    ):
        ByteArray =
        (
            "ludoproof:friend-credential:v1:" +
                friendId +
                ":" +
                requestId
            )
            .toByteArray(
                Charsets.UTF_8,
            )

    private companion object {
        const val PREFS_NAME =
            "ludoproof_friend_identity_v1"
        const val ANDROID_KEYSTORE =
            "AndroidKeyStore"
        const val KEY_ALIAS =
            "ludoproof_friend_identity_key_v1"
        const val TRANSFORMATION =
            "AES/GCM/NoPadding"
        const val KEY_FRIEND_ID =
            "friend_id"
        const val KEY_REGISTRATION_REQUEST_ID =
            "registration_request_id"
        const val KEY_IV =
            "friend_token_iv"
        const val KEY_TOKEN_CIPHERTEXT =
            "friend_token_ciphertext"

        val FRIEND_ID =
            Regex(
                "^LPF-[A-Z2-9]{4}-[A-Z2-9]{4}$",
            )
        val FRIEND_TOKEN =
            Regex(
                "^lf_[A-Za-z0-9_-]{32,}$",
            )
        val UUID_V4 =
            Regex(
                "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            )
    }
}
