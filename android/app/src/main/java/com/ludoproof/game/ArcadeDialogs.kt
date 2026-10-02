package com.ludoproof.game

import android.content.Context
import com.ludoproof.game.ui.dialogs.showNaturalWorldAuditDialog
import com.ludoproof.game.ui.dialogs.showProofHistoryDialog
import com.ludoproof.game.ui.dialogs.showSettingsDialog

object ArcadeDialogs {
    fun showSettings(context: Context) = showSettingsDialog(context)

    fun showNaturalWorldAudit(
        context: Context,
        audit: OfflineRandomnessAudit?,
    ) = showNaturalWorldAuditDialog(context, audit)

    fun showProofHistory(
        context: Context,
        title: String,
        body: String,
    ) = showProofHistoryDialog(context, title, body)
}
