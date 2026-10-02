package com.ludoproof.game.feature.friends.presentation

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.*
import com.ludoproof.game.feature.friends.data.local.FriendCredential
import com.ludoproof.game.feature.friends.data.local.FriendIdentityStore
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors

class FriendsActivity :
    Activity() {
    private val api by lazy {
        GameApi()
    }
    private val executor =
        Executors
            .newSingleThreadExecutor()
    private val mainHandler =
        Handler(
            Looper
                .getMainLooper(),
        )
    private val identityStore by lazy {
        FriendIdentityStore(
            this,
        )
    }
    private val profileStore by lazy {
        ProfileStore(
            this,
        )
    }
    private val sessionStore by lazy {
        SecureSessionStore(
            this,
        )
    }
    private val pendingOperationStore by lazy {
        PendingOperationStore(
            this,
        )
    }

    private var credential:
        FriendCredential? =
        null
    private var snapshot:
        JSONObject? =
        null
    private var isOnline =
        true

    @Volatile
    private var requestInFlight =
        false

    private var selectedPlayerCount =
        2

    private lateinit var statusText:
        TextView
    private lateinit var friendIdText:
        TextView
    private lateinit var addFriendInput:
        EditText
    private lateinit var roomContainer:
        LinearLayout
    private lateinit var requestsContainer:
        LinearLayout
    private lateinit var invitesContainer:
        LinearLayout
    private lateinit var friendsContainer:
        LinearLayout

    private val connectivityMonitor by lazy {
        ConnectivityMonitor(
            this,
        ) {
                online ->
            mainHandler.post {
                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@post
                }

                val changed =
                    isOnline !=
                        online
                isOnline =
                    online

                if (!online) {
                    showStatus(
                        "Offline — friends, presence and invites will refresh when internet returns.",
                    )
                } else if (
                    changed
                ) {
                    syncFriends(
                        announce = false,
                    )
                }
            }
        }
    }

    private val refreshRunnable =
        object :
            Runnable {
            override fun run() {
                if (
                    !isFinishing &&
                    !isDestroyed &&
                    isOnline
                ) {
                    syncFriends(
                        announce = false,
                    )
                }
                mainHandler
                    .postDelayed(
                        this,
                        REFRESH_MS,
                    )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )
        LudoProofTheme
            .configureWindow(
                this,
            )

        credential =
            identityStore
                .load()

        buildUi()
        renderAll()
    }

    override fun onStart() {
        super.onStart()
        connectivityMonitor
            .start()
        mainHandler
            .removeCallbacks(
                refreshRunnable,
            )
        mainHandler
            .post(
                refreshRunnable,
            )
    }

    override fun onStop() {
        connectivityMonitor
            .stop()
        mainHandler
            .removeCallbacks(
                refreshRunnable,
            )
        super.onStop()
    }

    override fun onDestroy() {
        mainHandler
            .removeCallbacksAndMessages(
                null,
            )
        executor
            .shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val (
            root,
            host,
        ) =
            LudoProofTheme
                .arcadeRoot(
                    this,
                )

        val scroll =
            ScrollView(
                this,
            ).apply {
                isFillViewport =
                    true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }

        val contentHost =
            FrameLayout(
                this,
            )
        scroll.addView(
            contentHost,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val horizontalPadding =
            dp(
                if (
                    LudoProofTheme
                        .isCompactWidth(
                            this,
                        )
                ) {
                    12
                } else {
                    18
                },
            )
        val width =
            minOf(
                resources
                    .displayMetrics
                    .widthPixels -
                    horizontalPadding *
                        2,
                dp(
                    LudoProofTheme
                        .pageMaxContentWidthDp(
                            this,
                        ),
                ),
            )

        val content =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(34),
                )
            }

        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                width,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or
                    Gravity.CENTER_HORIZONTAL,
            ),
        )

        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        content.addView(
            topBar(),
        )
        content.addView(
            identityPanel(),
            sectionParams(
                14,
            ),
        )
        content.addView(
            addFriendPanel(),
            sectionParams(
                14,
            ),
        )

        roomContainer =
            sectionPanel(
                "PRIVATE ROOM",
            )
        content.addView(
            roomContainer,
            sectionParams(
                14,
            ),
        )

        requestsContainer =
            sectionPanel(
                "FRIEND REQUESTS",
            )
        content.addView(
            requestsContainer,
            sectionParams(
                14,
            ),
        )

        invitesContainer =
            sectionPanel(
                "ROOM INVITES",
            )
        content.addView(
            invitesContainer,
            sectionParams(
                14,
            ),
        )

        friendsContainer =
            sectionPanel(
                "MY FRIENDS",
            )
        content.addView(
            friendsContainer,
            sectionParams(
                14,
            ),
        )

        statusText =
            TextView(
                this,
            ).apply {
                text =
                    "Preparing friends…"
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                    bright = true,
                )
                setPadding(
                    dp(12),
                    dp(16),
                    dp(12),
                    dp(16),
                )
            }
        content.addView(
            statusText,
        )

        setContentView(
            root,
        )
    }

    private fun topBar():
        LinearLayout =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL

            addView(
                Button(
                    this@FriendsActivity,
                ).apply {
                    text =
                        "‹  BACK"
                    LudoProofTheme.secondary(
                        this,
                    )
                    setOnClickListener {
                        finish()
                    }
                },
                LinearLayout.LayoutParams(
                    dp(92),
                    dp(50),
                ),
            )

            addView(
                TextView(
                    this@FriendsActivity,
                ).apply {
                    text =
                        "FRIENDS"
                    LudoProofTheme.title(
                        this,
                        if (
                            LudoProofTheme
                                .isCompactWidth(
                                    this@FriendsActivity,
                                )
                        ) {
                            24f
                        } else {
                            28f
                        },
                        gold = true,
                    )
                    gravity =
                        Gravity.CENTER_VERTICAL
                    setPadding(
                        dp(12),
                        0,
                        0,
                        0,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )
        }

    private fun identityPanel():
        LinearLayout =
        sectionPanel(
            "YOUR FRIEND ID",
        ).apply {
            friendIdText =
                TextView(
                    this@FriendsActivity,
                ).apply {
                    text =
                        credential
                            ?.friendId
                            ?: "CREATING…"
                    LudoProofTheme.title(
                        this,
                        22f,
                        gold = true,
                    )
                    setPadding(
                        dp(8),
                        dp(8),
                        dp(8),
                        dp(10),
                    )
                }
            addView(
                friendIdText,
            )

            addView(
                TextView(
                    this@FriendsActivity,
                ).apply {
                    text =
                        "Share only this Friend ID. Your private friend credential stays encrypted on this device."
                    LudoProofTheme.body(
                        this,
                        11f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        dp(8),
                        0,
                        dp(8),
                        dp(10),
                    )
                },
            )

            val actions =
                LinearLayout(
                    this@FriendsActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                }

            actions.addView(
                actionButton(
                    "COPY ID",
                    positive = true,
                ) {
                    copyFriendId()
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(50),
                    1f,
                ).apply {
                    marginEnd =
                        dp(5)
                },
            )
            actions.addView(
                actionButton(
                    "SHARE ID",
                ) {
                    shareFriendId()
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(50),
                    1f,
                ).apply {
                    marginStart =
                        dp(5)
                },
            )
            addView(
                actions,
            )
        }

    private fun addFriendPanel():
        LinearLayout =
        sectionPanel(
            "ADD FRIEND",
        ).apply {
            addView(
                TextView(
                    this@FriendsActivity,
                ).apply {
                    text =
                        "Enter a Friend ID like LPF-ABCD-EFGH."
                    LudoProofTheme.body(
                        this,
                        11f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        dp(6),
                        0,
                        dp(6),
                        dp(8),
                    )
                },
            )

            addFriendInput =
                EditText(
                    this@FriendsActivity,
                ).apply {
                    hint =
                        "LPF-ABCD-EFGH"
                    setSingleLine(
                        true,
                    )
                    LudoProofTheme.input(
                        this,
                    )
                }
            addView(
                addFriendInput,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(54),
                ),
            )

            addView(
                actionButton(
                    "SEND FRIEND REQUEST",
                    primary = true,
                ) {
                    sendFriendRequest()
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(54),
                ).apply {
                    topMargin =
                        dp(9)
                },
            )
        }

    private fun renderAll() {
        friendIdText.text =
            credential
                ?.friendId
                ?: "CREATING…"
        renderRoom()
        renderRequests()
        renderInvites()
        renderFriends()

        if (
            credential ==
            null
        ) {
            ensureIdentity()
        }
    }

    private fun renderRoom() {
        clearSectionBody(
            roomContainer,
        )

        val session =
            activeFriendSession()

        if (
            session !=
            null
        ) {
            roomContainer.addView(
                TextView(
                    this,
                ).apply {
                    text =
                        "ACTIVE ROOM  •  " +
                            session.matchId
                    LudoProofTheme.title(
                        this,
                        20f,
                        gold = true,
                    )
                    setPadding(
                        dp(8),
                        dp(8),
                        dp(8),
                        dp(9),
                    )
                },
            )

            roomContainer.addView(
                TextView(
                    this,
                ).apply {
                    text =
                        "Invite friends below, then open the room. The host can start only after all selected seats are filled."
                    LudoProofTheme.body(
                        this,
                        11f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        dp(8),
                        0,
                        dp(8),
                        dp(10),
                    )
                },
            )

            val actions =
                LinearLayout(
                    this,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                }
            actions.addView(
                actionButton(
                    "OPEN ROOM",
                    positive = true,
                ) {
                    openFriendRoom()
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(52),
                    1f,
                ).apply {
                    marginEnd =
                        dp(5)
                },
            )
            actions.addView(
                actionButton(
                    "SHARE ROOM",
                ) {
                    shareRoom(
                        session.matchId,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(52),
                    1f,
                ).apply {
                    marginStart =
                        dp(5)
                },
            )
            roomContainer.addView(
                actions,
            )
            return
        }

        roomContainer.addView(
            TextView(
                this,
            ).apply {
                text =
                    "Choose how many real players must join before the private match can start."
                LudoProofTheme.body(
                    this,
                    11f,
                    centered = true,
                    bright = true,
                )
                setPadding(
                    dp(8),
                    0,
                    dp(8),
                    dp(10),
                )
            },
        )

        val countRow =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        listOf(
            2,
            3,
            4,
        ).forEachIndexed {
                index,
                count ->
            countRow.addView(
                actionButton(
                    if (
                        selectedPlayerCount ==
                        count
                    ) {
                        "✓  $count"
                    } else {
                        count.toString()
                    },
                    positive =
                        selectedPlayerCount ==
                            count,
                ) {
                    selectedPlayerCount =
                        count
                    renderRoom()
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(50),
                    1f,
                ).apply {
                    if (
                        index >
                        0
                    ) {
                        marginStart =
                            dp(5)
                    }
                    if (
                        index <
                        2
                    ) {
                        marginEnd =
                            dp(5)
                    }
                },
            )
        }
        roomContainer.addView(
            countRow,
        )

        roomContainer.addView(
            actionButton(
                "CREATE PRIVATE ROOM",
                primary = true,
            ) {
                createFriendRoom()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56),
            ).apply {
                topMargin =
                    dp(10)
            },
        )
    }

    private fun renderRequests() {
        clearSectionBody(
            requestsContainer,
        )

        val incoming =
            snapshot
                ?.optJSONArray(
                    "incomingRequests",
                )
        val outgoing =
            snapshot
                ?.optJSONArray(
                    "outgoingRequests",
                )

        if (
            incoming.isEmptyArray() &&
            outgoing.isEmptyArray()
        ) {
            requestsContainer
                .addView(
                    emptyText(
                        "No pending friend requests.",
                    ),
                )
            return
        }

        incoming
            .forEachObject {
                request ->
            val friend =
                request
                    .optJSONObject(
                        "friend",
                    )
                    ?: return@forEachObject
            val requestId =
                request
                    .optString(
                        "requestId",
                    )
            val row =
                friendRow(
                    friend,
                    "INCOMING REQUEST",
                )
            val actions =
                LinearLayout(
                    this,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                }
            actions.addView(
                actionButton(
                    "ACCEPT",
                    positive = true,
                ) {
                    respondFriendRequest(
                        requestId,
                        true,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f,
                ).apply {
                    marginEnd =
                        dp(5)
                },
            )
            actions.addView(
                actionButton(
                    "DECLINE",
                    danger = true,
                ) {
                    respondFriendRequest(
                        requestId,
                        false,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f,
                ).apply {
                    marginStart =
                        dp(5)
                },
            )
            row.addView(
                actions,
            )
            requestsContainer
                .addView(
                    row,
                    itemParams(),
                )
        }

        outgoing
            .forEachObject {
                request ->
            val friend =
                request
                    .optJSONObject(
                        "friend",
                    )
                    ?: return@forEachObject
            requestsContainer
                .addView(
                    friendRow(
                        friend,
                        "REQUEST SENT • PENDING",
                    ),
                    itemParams(),
                )
        }
    }

    private fun renderInvites() {
        clearSectionBody(
            invitesContainer,
        )
        val invites =
            snapshot
                ?.optJSONArray(
                    "invites",
                )

        if (
            invites.isEmptyArray()
        ) {
            invitesContainer
                .addView(
                    emptyText(
                        "No private-room invites.",
                    ),
                )
            return
        }

        invites
            .forEachObject {
                invite ->
            val from =
                invite
                    .optJSONObject(
                        "from",
                    )
                    ?: return@forEachObject
            val inviteId =
                invite
                    .optString(
                        "inviteId",
                    )
            val matchId =
                invite
                    .optString(
                        "matchId",
                    )

            val row =
                friendRow(
                    from,
                    "INVITED YOU • $matchId",
                )
            val actions =
                LinearLayout(
                    this,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                }
            actions.addView(
                actionButton(
                    "JOIN ROOM",
                    positive = true,
                ) {
                    acceptInvite(
                        inviteId,
                        matchId,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f,
                ).apply {
                    marginEnd =
                        dp(5)
                },
            )
            actions.addView(
                actionButton(
                    "DECLINE",
                    danger = true,
                ) {
                    declineInvite(
                        inviteId,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f,
                ).apply {
                    marginStart =
                        dp(5)
                },
            )
            row.addView(
                actions,
            )
            invitesContainer
                .addView(
                    row,
                    itemParams(),
                )
        }
    }

    private fun renderFriends() {
        clearSectionBody(
            friendsContainer,
        )
        val friends =
            snapshot
                ?.optJSONArray(
                    "friends",
                )

        if (
            friends.isEmptyArray()
        ) {
            friendsContainer
                .addView(
                    emptyText(
                        "No friends yet. Share your Friend ID or add someone above.",
                    ),
                )
            return
        }

        val activeRoom =
            activeFriendSession()

        friends
            .forEachObject {
                friend ->
            val friendId =
                friend
                    .optString(
                        "friendId",
                    )
            val row =
                friendRow(
                    friend,
                    if (
                        friend.optBoolean(
                            "online",
                            false,
                        )
                    ) {
                        "● ONLINE"
                    } else {
                        "● OFFLINE"
                    },
                )

            val actions =
                LinearLayout(
                    this,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                }

            if (
                activeRoom !=
                null
            ) {
                actions.addView(
                    actionButton(
                        "INVITE",
                        positive = true,
                    ) {
                        inviteFriend(
                            friendId,
                            activeRoom
                                .matchId,
                        )
                    },
                    LinearLayout.LayoutParams(
                        0,
                        dp(46),
                        1f,
                    ).apply {
                        marginEnd =
                            dp(5)
                    },
                )
            }

            actions.addView(
                actionButton(
                    "REMOVE",
                    danger = true,
                ) {
                    confirmRemoveFriend(
                        friendId,
                        friend.optString(
                            "displayName",
                            "Friend",
                        ),
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f,
                ).apply {
                    if (
                        activeRoom !=
                        null
                    ) {
                        marginStart =
                            dp(5)
                    }
                },
            )
            row.addView(
                actions,
            )

            friendsContainer
                .addView(
                    row,
                    itemParams(),
                )
        }
    }

    private fun ensureIdentity() {
        if (
            credential !=
            null ||
            requestInFlight ||
            !isOnline
        ) {
            return
        }

        val displayName =
            profileStore
                .snapshot()
                .displayName
        val requestId =
            runCatching {
                identityStore
                    .registrationRequestId()
            }
                .getOrElse {
                    showStatus(
                        "Could not prepare Friend ID.",
                    )
                    return
                }

        runRequest(
            working =
                "Creating your Friend ID…",
            action = {
                api.registerFriend(
                    displayName =
                        displayName,
                    clientRequestId =
                        requestId,
                )
            },
            onSuccess = {
                    response ->
                val next =
                    FriendCredential(
                        friendId =
                            response.getString(
                                "friendId",
                            ),
                        friendToken =
                            response.getString(
                                "friendToken",
                            ),
                        registrationRequestId =
                            requestId,
                    )
                runCatching {
                    identityStore
                        .save(
                            next,
                        )
                }
                    .onFailure {
                        showStatus(
                            "Friend ID was created but could not be secured on this device.",
                        )
                        return@runRequest
                    }

                credential =
                    next
                friendIdText.text =
                    next.friendId
                showStatus(
                    "Friend ID ready.",
                )
                syncFriends(
                    announce = false,
                )
            },
        )
    }

    private fun syncFriends(
        announce: Boolean,
    ) {
        val auth =
            credential
                ?: run {
                    ensureIdentity()
                    return
                }

        if (
            requestInFlight ||
            !isOnline
        ) {
            return
        }

        runRequest(
            working =
                if (
                    announce
                ) {
                    "Refreshing friends…"
                } else {
                    null
                },
            action = {
                api.friendHeartbeat(
                    friendToken =
                        auth.friendToken,
                    displayName =
                        profileStore
                            .snapshot()
                            .displayName,
                )
                api.friendSnapshot(
                    auth.friendToken,
                )
            },
            onSuccess = {
                snapshot =
                    it
                renderAll()
                if (
                    announce
                ) {
                    showStatus(
                        "Friends refreshed.",
                    )
                }
            },
            quietFailure =
                !announce,
        )
    }

    private fun sendFriendRequest() {
        val auth =
            credential
                ?: run {
                    ensureIdentity()
                    return
                }
        val target =
            addFriendInput
                .text
                .toString()
                .trim()
                .uppercase()

        if (
            !FRIEND_ID.matches(
                target,
            )
        ) {
            showStatus(
                "Enter a valid Friend ID like LPF-ABCD-EFGH.",
            )
            return
        }

        runRequest(
            working =
                "Sending friend request…",
            action = {
                api.sendFriendRequest(
                    friendToken =
                        auth.friendToken,
                    friendId =
                        target,
                )
            },
            onSuccess = {
                    response ->
                addFriendInput
                    .setText(
                        "",
                    )
                showStatus(
                    when (
                        response
                            .optString(
                                "status",
                            )
                    ) {
                        "ALREADY_FRIENDS" ->
                            "You are already friends."
                        "INCOMING_PENDING" ->
                            "They already sent you a request. Accept it below."
                        else ->
                            "Friend request sent."
                    },
                )
                syncFriends(
                    announce = false,
                )
            },
        )
    }

    private fun respondFriendRequest(
        requestId: String,
        accept: Boolean,
    ) {
        val auth =
            credential
                ?: return

        runRequest(
            working =
                if (
                    accept
                ) {
                    "Accepting friend request…"
                } else {
                    "Declining friend request…"
                },
            action = {
                api.respondFriendRequest(
                    friendToken =
                        auth.friendToken,
                    requestId =
                        requestId,
                    accept =
                        accept,
                )
            },
            onSuccess = {
                showStatus(
                    if (
                        accept
                    ) {
                        "Friend added."
                    } else {
                        "Friend request declined."
                    },
                )
                syncFriends(
                    announce = false,
                )
            },
        )
    }

    private fun createFriendRoom() {
        if (
            activeFriendSession() !=
            null
        ) {
            showStatus(
                "A friend room is already saved. Open or share it first.",
            )
            return
        }

        val displayName =
            profileStore
                .snapshot()
                .displayName
        val operationKey =
            "friends:" +
                selectedPlayerCount +
                ":" +
                displayName
        val requestId =
            runCatching {
                pendingOperationStore
                    .getOrCreate(
                        "friend_create",
                        operationKey,
                    )
            }
                .getOrElse {
                    showStatus(
                        "Could not prepare private room.",
                    )
                    return
                }

        runRequest(
            working =
                "Creating $selectedPlayerCount-player private room…",
            action = {
                api.createFriendRoom(
                    displayName =
                        displayName,
                    clientRequestId =
                        requestId,
                    playerCount =
                        selectedPlayerCount,
                )
            },
            onSuccess = {
                    response ->
                val session =
                    PlayerSession(
                        matchId =
                            response
                                .getString(
                                    "matchId",
                                ),
                        playerId =
                            response
                                .getString(
                                    "playerId",
                                ),
                        playerToken =
                            response
                                .getString(
                                    "playerToken",
                                ),
                        modeWire =
                            GameMode.FRIENDS
                                .wireValue,
                    )

                runCatching {
                    sessionStore
                        .save(
                            session,
                        )
                }
                    .onFailure {
                        showStatus(
                            "Room was created but its secure session could not be saved.",
                        )
                        return@runRequest
                    }

                pendingOperationStore
                    .clear(
                        "friend_create",
                        operationKey,
                    )
                renderRoom()
                renderFriends()
                showStatus(
                    "Private room ${session.matchId} created. Invite friends below.",
                )
            },
        )
    }

    private fun inviteFriend(
        friendId: String,
        matchId: String,
    ) {
        val auth =
            credential
                ?: return

        runRequest(
            working =
                "Sending room invite…",
            action = {
                api.sendFriendInvite(
                    friendToken =
                        auth.friendToken,
                    friendId =
                        friendId,
                    matchId =
                        matchId,
                    clientRequestId =
                        UUID.randomUUID()
                            .toString(),
                )
            },
            onSuccess = {
                showStatus(
                    "Private-room invite sent.",
                )
                syncFriends(
                    announce = false,
                )
            },
        )
    }

    private fun acceptInvite(
        inviteId: String,
        matchId: String,
    ) {
        val auth =
            credential
                ?: return
        val displayName =
            profileStore
                .snapshot()
                .displayName
        val operationKey =
            "friends_join:" +
                matchId +
                ":" +
                displayName
        val requestId =
            runCatching {
                pendingOperationStore
                    .getOrCreate(
                        "friend_join",
                        operationKey,
                    )
            }
                .getOrElse {
                    showStatus(
                        "Could not prepare room join.",
                    )
                    return
                }

        runRequest(
            working =
                "Joining private room…",
            action = {
                val joined =
                    api.joinMatch(
                        matchId =
                            matchId,
                        displayName =
                            displayName,
                        clientRequestId =
                            requestId,
                    )

                api.respondFriendInvite(
                    friendToken =
                        auth.friendToken,
                    inviteId =
                        inviteId,
                    accept =
                        true,
                )

                joined
            },
            onSuccess = {
                    response ->
                val session =
                    PlayerSession(
                        matchId =
                            response
                                .getString(
                                    "matchId",
                                ),
                        playerId =
                            response
                                .getString(
                                    "playerId",
                                ),
                        playerToken =
                            response
                                .getString(
                                    "playerToken",
                                ),
                        modeWire =
                            GameMode.FRIENDS
                                .wireValue,
                    )

                runCatching {
                    sessionStore
                        .save(
                            session,
                        )
                }
                    .onFailure {
                        showStatus(
                            "Joined room but could not secure the session locally.",
                        )
                        return@runRequest
                    }

                pendingOperationStore
                    .clear(
                        "friend_join",
                        operationKey,
                    )

                openFriendRoom()
            },
        )
    }

    private fun declineInvite(
        inviteId: String,
    ) {
        val auth =
            credential
                ?: return

        runRequest(
            working =
                "Declining room invite…",
            action = {
                api.respondFriendInvite(
                    friendToken =
                        auth.friendToken,
                    inviteId =
                        inviteId,
                    accept =
                        false,
                )
            },
            onSuccess = {
                showStatus(
                    "Room invite declined.",
                )
                syncFriends(
                    announce = false,
                )
            },
        )
    }

    private fun confirmRemoveFriend(
        friendId: String,
        displayName: String,
    ) {
        AlertDialog
            .Builder(
                this,
            )
            .setTitle(
                "Remove friend?",
            )
            .setMessage(
                "Remove $displayName ($friendId) from your friends?",
            )
            .setNegativeButton(
                "CANCEL",
                null,
            )
            .setPositiveButton(
                "REMOVE",
            ) {
                    _,
                    _ ->
                removeFriend(
                    friendId,
                )
            }
            .show()
    }

    private fun removeFriend(
        friendId: String,
    ) {
        val auth =
            credential
                ?: return
        runRequest(
            working =
                "Removing friend…",
            action = {
                api.removeFriend(
                    friendToken =
                        auth.friendToken,
                    friendId =
                        friendId,
                )
            },
            onSuccess = {
                showStatus(
                    "Friend removed.",
                )
                syncFriends(
                    announce = false,
                )
            },
        )
    }

    private fun openFriendRoom() {
        val session =
            activeFriendSession()
                ?: run {
                    showStatus(
                        "Create or join a private friend room first.",
                    )
                    return
                }

        startActivity(
            Intent(
                this,
                MainActivity::class.java,
            )
                .putExtra(
                    GameModeIntent
                        .EXTRA_GAME_MODE,
                    GameMode.FRIENDS
                        .wireValue,
                )
                .putExtra(
                    EXTRA_FRIEND_ROOM_ID,
                    session.matchId,
                ),
        )
    }

    private fun copyFriendId() {
        val id =
            credential
                ?.friendId
                ?: run {
                    showStatus(
                        "Friend ID is still being created.",
                    )
                    return
                }
        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE,
            ) as ClipboardManager
        clipboard
            .setPrimaryClip(
                ClipData
                    .newPlainText(
                        "LudoProof Friend ID",
                        id,
                    ),
            )
        showStatus(
            "Friend ID copied.",
        )
    }

    private fun shareFriendId() {
        val id =
            credential
                ?.friendId
                ?: run {
                    showStatus(
                        "Friend ID is still being created.",
                    )
                    return
                }

        shareText(
            "Add me on LudoProof\nFriend ID: $id\n\n" +
                playStoreUrl(),
            "Share Friend ID",
        )
    }

    private fun shareRoom(
        matchId: String,
    ) {
        shareText(
            "Join my private LudoProof room: $matchId\n\n" +
                playStoreUrl(),
            "Share private room",
        )
    }

    private fun shareText(
        text: String,
        chooserTitle: String,
    ) {
        val intent =
            Intent(
                Intent.ACTION_SEND,
            ).apply {
                type =
                    "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    text,
                )
            }

        startActivity(
            Intent
                .createChooser(
                    intent,
                    chooserTitle,
                ),
        )
    }

    private fun playStoreUrl():
        String =
        "https://play.google.com/store/apps/details?id=$packageName"

    private fun activeFriendSession():
        PlayerSession? =
        sessionStore
            .load()
            ?.takeIf {
                GameMode
                    .fromWireValue(
                        it.modeWire,
                    ) ==
                    GameMode.FRIENDS
            }

    private fun runRequest(
        working: String?,
        action: () -> JSONObject,
        onSuccess: (
            JSONObject,
        ) -> Unit,
        quietFailure: Boolean = false,
    ) {
        if (
            requestInFlight ||
            !isOnline ||
            executor.isShutdown
        ) {
            if (
                !isOnline &&
                !quietFailure
            ) {
                showStatus(
                    "Internet connection is required.",
                )
            }
            return
        }

        requestInFlight =
            true
        if (
            working !=
            null
        ) {
            showStatus(
                working,
            )
        }

        val submitted =
            runCatching {
                executor.execute {
                    try {
                        val response =
                            action()
                        mainHandler.post {
                            requestInFlight =
                                false
                            if (
                                isFinishing ||
                                isDestroyed
                            ) {
                                return@post
                            }
                            onSuccess(
                                response,
                            )
                        }
                    } catch (
                        error: Exception,
                    ) {
                        mainHandler.post {
                            requestInFlight =
                                false
                            if (
                                isFinishing ||
                                isDestroyed
                            ) {
                                return@post
                            }

                            val apiError =
                                error as?
                                    GameApiException
                            if (
                                apiError
                                    ?.code ==
                                "FRIEND_AUTH_INVALID"
                            ) {
                                identityStore
                                    .clearCredentialOnly()
                                credential =
                                    null
                                friendIdText.text =
                                    "RECOVERING…"
                                ensureIdentity()
                                return@post
                            }

                            if (
                                !quietFailure
                            ) {
                                showStatus(
                                    "Error: " +
                                        (
                                            error.message
                                                ?: error
                                                    .toString()
                                            ),
                                )
                            }
                        }
                    }
                }
            }
                .isSuccess

        if (
            !submitted
        ) {
            requestInFlight =
                false
        }
    }

    private fun showStatus(
        value: String,
    ) {
        if (
            ::statusText
                .isInitialized
        ) {
            statusText.text =
                value
        }
    }

    private fun sectionPanel(
        title: String,
    ):
        LinearLayout =
        LudoProofTheme
            .panel(
                this,
            )
            .apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(14),
                )

                addView(
                    TextView(
                        this@FriendsActivity,
                    ).apply {
                        text =
                            title
                        LudoProofTheme.title(
                            this,
                            18f,
                            gold = true,
                        )
                        setPadding(
                            dp(4),
                            0,
                            dp(4),
                            dp(10),
                        )
                    },
                )
            }

    private fun friendRow(
        friend: JSONObject,
        subtitle: String,
    ):
        LinearLayout =
        LudoProofTheme
            .panel(
                this,
            )
            .apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(11),
                    dp(10),
                    dp(11),
                    dp(10),
                )

                addView(
                    TextView(
                        this@FriendsActivity,
                    ).apply {
                        text =
                            friend
                                .optString(
                                    "displayName",
                                    "Friend",
                                )
                        LudoProofTheme.title(
                            this,
                            16f,
                        )
                    },
                )

                addView(
                    TextView(
                        this@FriendsActivity,
                    ).apply {
                        text =
                            friend
                                .optString(
                                    "friendId",
                                ) +
                                "   •   " +
                                subtitle
                        LudoProofTheme.body(
                            this,
                            11f,
                            centered = false,
                            bright = true,
                        )
                        setPadding(
                            0,
                            dp(3),
                            0,
                            dp(8),
                        )
                    },
                )
            }

    private fun actionButton(
        label: String,
        primary: Boolean = false,
        positive: Boolean = false,
        danger: Boolean = false,
        action: () -> Unit,
    ):
        Button =
        Button(
            this,
        ).apply {
            text =
                label
            when {
                danger ->
                    LudoProofTheme.danger(
                        this,
                    )
                positive ->
                    LudoProofTheme.positive(
                        this,
                    )
                primary ->
                    LudoProofTheme.primary(
                        this,
                    )
                else ->
                    LudoProofTheme.secondary(
                        this,
                    )
            }
            setOnClickListener {
                action()
            }
        }

    private fun emptyText(
        value: String,
    ):
        TextView =
        TextView(
            this,
        ).apply {
            text =
                value
            LudoProofTheme.body(
                this,
                12f,
                centered = true,
                bright = true,
            )
            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8),
            )
        }

    private fun clearSectionBody(
        container: LinearLayout,
    ) {
        if (
            container.childCount >
            1
        ) {
            container
                .removeViews(
                    1,
                    container.childCount -
                        1,
                )
        }
    }

    private fun itemParams():
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin =
                dp(7)
        }

    private fun sectionParams(
        topDp: Int,
    ):
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin =
                dp(
                    topDp,
                )
        }

    private fun dp(
        value: Int,
    ):
        Int =
        LudoProofTheme
            .dp(
                this,
                value,
            )

    private fun JSONArray?.isEmptyArray():
        Boolean =
        this ==
            null ||
            length() ==
            0

    private inline fun JSONArray?.forEachObject(
        block: (
            JSONObject,
        ) -> Unit,
    ) {
        if (
            this ==
            null
        ) {
            return
        }
        for (
            index in
            0 until length()
        ) {
            optJSONObject(
                index,
            )
                ?.let(
                    block,
                )
        }
    }

    private companion object {
        const val REFRESH_MS =
            10_000L
        const val EXTRA_FRIEND_ROOM_ID =
            "ludoproof_friend_room_id_v1"

        val FRIEND_ID =
            Regex(
                "^LPF-[A-Z2-9]{4}-[A-Z2-9]{4}$",
            )
    }
}
