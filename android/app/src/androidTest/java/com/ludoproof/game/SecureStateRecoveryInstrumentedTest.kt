package com.ludoproof.game

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureStateRecoveryInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SecureSessionStore(context).clear()
        PendingRollStore(context).clear()
    }

    @After
    fun tearDown() {
        SecureSessionStore(context).clear()
        PendingRollStore(context).clear()
    }

    @Test
    fun playerSessionSurvivesStoreRecreationThroughAndroidKeystore() {
        val expected =
            PlayerSession(
                matchId = MATCH_ID,
                playerId = "player-restore-1",
                playerToken = PLAYER_TOKEN,
                modeWire = GameMode.TEAM_UP.wireValue,
            )

        SecureSessionStore(context).save(expected)

        val restored = SecureSessionStore(context).load()

        assertEquals(expected, restored)
    }

    @Test
    fun pendingRollSecretSurvivesStoreRecreationThroughAndroidKeystore() {
        val expected =
            PendingRollSecret(
                matchId = MATCH_ID,
                clientSeed = CLIENT_SEED,
                clientCommitment = CLIENT_COMMITMENT,
            )

        PendingRollStore(context).save(expected)

        val restored = PendingRollStore(context).load()

        assertEquals(expected, restored)
    }

    @Test
    fun tamperedPlayerSessionCiphertextFailsClosedAndIsCleared() {
        SecureSessionStore(context).save(
            PlayerSession(
                matchId = MATCH_ID,
                playerId = "player-tamper-1",
                playerToken = PLAYER_TOKEN,
                modeWire = GameMode.ONLINE.wireValue,
            ),
        )
        val prefs =
            context.getSharedPreferences(
                "ludoproof_secure_session",
                Context.MODE_PRIVATE,
            )
        val ciphertext =
            requireNotNull(
                prefs.getString(
                    "playerTokenCiphertext",
                    null,
                ),
            )
        prefs.edit()
            .putString(
                "playerTokenCiphertext",
                mutateBase64(ciphertext),
            )
            .commit()

        assertNull(SecureSessionStore(context).load())
        assertFalse(prefs.contains("playerTokenCiphertext"))
    }

    @Test
    fun tamperedPendingSeedCiphertextFailsClosedAndIsCleared() {
        PendingRollStore(context).save(
            PendingRollSecret(
                matchId = MATCH_ID,
                clientSeed = CLIENT_SEED,
                clientCommitment = CLIENT_COMMITMENT,
            ),
        )
        val prefs =
            context.getSharedPreferences(
                "ludoproof_pending_roll",
                Context.MODE_PRIVATE,
            )
        val ciphertext =
            requireNotNull(
                prefs.getString(
                    "seedCiphertext",
                    null,
                ),
            )
        prefs.edit()
            .putString(
                "seedCiphertext",
                mutateBase64(ciphertext),
            )
            .commit()

        assertNull(PendingRollStore(context).load())
        assertFalse(prefs.contains("seedCiphertext"))
    }

    private fun mutateBase64(value: String): String {
        val replacement = if (value.last() == 'A') 'B' else 'A'
        return value.dropLast(1) + replacement
    }

    private companion object {
        const val MATCH_ID = "LPABCDEFGH"
        const val PLAYER_TOKEN =
            "lp_abcdefghijklmnopqrstuvwxyzABCDEF"
        const val CLIENT_SEED =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        const val CLIENT_COMMITMENT =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
    }
}
