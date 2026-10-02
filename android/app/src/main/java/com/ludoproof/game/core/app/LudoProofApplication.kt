package com.ludoproof.game

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.ludoproof.game.feature.friends.data.FriendPresenceController

class LudoProofApplication :
    Application(),
    Application.ActivityLifecycleCallbacks {
    private lateinit var friendPresence:
        FriendPresenceController
    private var resumedActivities =
        0

    override fun onCreate() {
        super.onCreate()
        friendPresence =
            FriendPresenceController(
                this,
            )
        registerActivityLifecycleCallbacks(
            this,
        )
    }

    override fun onActivityResumed(
        activity: Activity,
    ) {
        resumedActivities +=
            1
        if (
            resumedActivities ==
            1
        ) {
            friendPresence
                .start()
        }
    }

    override fun onActivityPaused(
        activity: Activity,
    ) {
        resumedActivities =
            (
                resumedActivities -
                    1
                )
                .coerceAtLeast(
                    0,
                )
        if (
            resumedActivities ==
            0
        ) {
            friendPresence
                .stop()
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
