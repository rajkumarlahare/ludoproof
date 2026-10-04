package com.ludoproof.game

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.ludoproof.game.feature.characters.data.local.CharacterSelectionStore
import com.ludoproof.game.feature.friends.data.FriendPresenceController
import com.ludoproof.game.feature.settings.data.local.GameMusicController

class LudoProofApplication :
    Application(),
    Application.ActivityLifecycleCallbacks {
    private lateinit var friendPresence:
        FriendPresenceController
    private var resumedActivities = 0

    override fun onCreate() {
        super.onCreate()
        // Prime the process-local cosmetic mirror before any remote create/join
        // request can be sent. Character selection remains presentation-only.
        CharacterSelectionStore(this).load()
        friendPresence =
            FriendPresenceController(this)
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(
        activity: Activity,
    ) {
        resumedActivities += 1
        if (resumedActivities == 1) {
            friendPresence.start()
            GameMusicController
                .onAppForeground(this)
        }
    }

    override fun onActivityPaused(
        activity: Activity,
    ) {
        resumedActivities =
            (resumedActivities - 1)
                .coerceAtLeast(0)
        if (resumedActivities == 0) {
            friendPresence.stop()
            GameMusicController
                .onAppBackground()
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityStarted(
        activity: Activity,
    ) = Unit

    override fun onActivityStopped(
        activity: Activity,
    ) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(
        activity: Activity,
    ) = Unit
}
