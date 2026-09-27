package com.ludoproof.game

import android.content.Context
import java.util.UUID

class PendingOperationStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun getOrCreate(
        type: String,
        operationKey: String,
    ): String {
        require(TYPE.matches(type))
        require(
            operationKey.length in 1..256,
        )

        val existingType =
            prefs.getString(
                KEY_TYPE,
                null,
            )
        val existingKey =
            prefs.getString(
                KEY_OPERATION_KEY,
                null,
            )
        val existingId =
            prefs.getString(
                KEY_REQUEST_ID,
                null,
            )

        if (
            existingType == type &&
            existingKey ==
                operationKey &&
            existingId != null &&
            REQUEST_ID.matches(
                existingId,
            )
        ) {
            return existingId
        }

        val requestId =
            UUID.randomUUID()
                .toString()
                .lowercase()
        val persisted =
            prefs.edit()
                .putString(
                    KEY_TYPE,
                    type,
                )
                .putString(
                    KEY_OPERATION_KEY,
                    operationKey,
                )
                .putString(
                    KEY_REQUEST_ID,
                    requestId,
                )
                .commit()

        check(persisted) {
            "Could not durably persist request identity"
        }
        return requestId
    }

    fun clear(
        type: String,
        operationKey: String,
    ) {
        if (
            prefs.getString(
                KEY_TYPE,
                null,
            ) != type ||
            prefs.getString(
                KEY_OPERATION_KEY,
                null,
            ) != operationKey
        ) {
            return
        }

        prefs.edit()
            .remove(KEY_TYPE)
            .remove(
                KEY_OPERATION_KEY,
            )
            .remove(
                KEY_REQUEST_ID,
            )
            .commit()
    }

    private companion object {
        const val PREFS_NAME =
            "ludoproof_pending_operation"
        const val KEY_TYPE =
            "type"
        const val KEY_OPERATION_KEY =
            "operationKey"
        const val KEY_REQUEST_ID =
            "requestId"

        val TYPE =
            Regex(
                "^[a-z][a-z0-9_-]{1,31}$",
            )
        val REQUEST_ID =
            Regex(
                "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            )
    }
}
