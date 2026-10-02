package com.ludoproof.game.feature.leaderboard.presentation

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardCacheStore
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardCredential
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardIdentityStore
import com.ludoproof.game.feature.leaderboard.data.remote.LeaderboardApi
import com.ludoproof.game.feature.leaderboard.domain.model.LeaderboardEntry
import com.ludoproof.game.feature.leaderboard.domain.model.LeaderboardSnapshot
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory
import java.text.NumberFormat
import java.util.concurrent.Executors

class LeaderboardActivity :
    Activity() {
    private val executor =
        Executors
            .newSingleThreadExecutor()
    private val mainHandler =
        Handler(
            Looper.getMainLooper(),
        )
    private val api by lazy {
        LeaderboardApi()
    }
    private val identityStore by lazy {
        LeaderboardIdentityStore(
            this,
        )
    }
    private val cacheStore by lazy {
        LeaderboardCacheStore(
            this,
        )
    }

    private var snapshot:
        LeaderboardSnapshot? =
        null
    private var loading =
        true
    private var showingCached =
        false
    private var statusMessage:
        String? =
        null

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

        cacheStore
            .load()
            ?.let {
                    cached ->
                runCatching {
                    LeaderboardSnapshot
                        .fromJson(
                            cached.payload,
                        )
                }.getOrNull()
                    ?.let {
                            cachedSnapshot ->
                        snapshot =
                            cachedSnapshot
                        showingCached =
                            true
                        loading =
                            false
                    }
            }

        render()
        refreshLeaderboard()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun refreshLeaderboard() {
        if (
            snapshot ==
            null
        ) {
            loading =
                true
            render()
        }

        executor.execute {
            try {
                val credential =
                    ensureCredential()
                val payload =
                    api.load(
                        profileToken =
                            credential.profileToken,
                        limit =
                            50,
                    )
                val fresh =
                    LeaderboardSnapshot
                        .fromJson(
                            payload,
                        )
                cacheStore
                    .save(
                        payload,
                    )

                mainHandler.post {
                    if (
                        isDestroyed ||
                        isFinishing
                    ) {
                        return@post
                    }
                    snapshot =
                        fresh
                    showingCached =
                        false
                    loading =
                        false
                    statusMessage =
                        null
                    render()
                }
            } catch (
                error: Exception,
            ) {
                mainHandler.post {
                    if (
                        isDestroyed ||
                        isFinishing
                    ) {
                        return@post
                    }
                    loading =
                        false
                    statusMessage =
                        if (
                            snapshot !=
                            null
                        ) {
                            "Could not refresh. Showing the last saved ranking."
                        } else {
                            "Leaderboard is unavailable right now. Check your connection and try again."
                        }
                    render()
                }
            }
        }
    }

    private fun ensureCredential():
        LeaderboardCredential {
        identityStore
            .load()
            ?.let {
                return it
            }

        val requestId =
            identityStore
                .registrationRequestId()
        val response =
            api.registerProfile(
                clientRequestId =
                    requestId,
            )
        val credential =
            LeaderboardCredential(
                profileId =
                    response.getString(
                        "profileId",
                    ),
                profileToken =
                    response.getString(
                        "profileToken",
                    ),
                registrationRequestId =
                    requestId,
            )
        identityStore.save(
            credential,
        )
        return credential
    }

    private fun render() {
        val (root, host) =
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
                clipToPadding =
                    false
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

        val side =
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
        val contentWidth =
            minOf(
                resources
                    .displayMetrics
                    .widthPixels -
                    side *
                    2,
                dp(
                    LudoProofTheme
                        .pageMaxContentWidthDp(
                            this,
                        ),
                ),
            )
                .coerceAtLeast(
                    1,
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
                    dp(32),
                )
            }

        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                contentWidth,
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
            currentPlayerCard(),
            sectionParams(
                16,
            ),
        )
        content.addView(
            scoringStrip(),
            sectionParams(
                10,
            ),
        )

        statusMessage
            ?.let {
                    message ->
                content.addView(
                    statusPanel(
                        message,
                    ),
                    sectionParams(
                        10,
                    ),
                )
            }

        content.addView(
            rankingHeader(),
            sectionParams(
                20,
            ),
        )

        when {
            loading &&
                snapshot ==
                null -> {
                content.addView(
                    loadingPanel(),
                    sectionParams(
                        10,
                    ),
                )
            }

            snapshot
                ?.entries
                .isNullOrEmpty() -> {
                content.addView(
                    emptyPanel(),
                    sectionParams(
                        10,
                    ),
                )
            }

            else -> {
                snapshot
                    ?.entries
                    .orEmpty()
                    .forEachIndexed {
                            index,
                            entry ->
                        content.addView(
                            rankingRow(
                                entry,
                            ),
                            sectionParams(
                                if (
                                    index ==
                                    0
                                ) {
                                    10
                                } else {
                                    8
                                },
                            ),
                        )
                    }
            }
        }

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
            minimumHeight =
                dp(58)

            addView(
                Button(
                    this@LeaderboardActivity,
                ).apply {
                    LudoProofTheme
                        .circularAction(
                            this,
                            "←",
                        )
                    contentDescription =
                        "Back"
                    setOnClickListener {
                        finish()
                    }
                },
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(48),
                ),
            )

            addView(
                LinearLayout(
                    this@LeaderboardActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                    gravity =
                        Gravity.START or
                            Gravity.CENTER_VERTICAL

                    addView(
                        TextView(
                            this@LeaderboardActivity,
                        ).apply {
                            text =
                                "TOP LEADERS"
                            textSize =
                                if (
                                    LudoProofTheme
                                        .isCompactWidth(
                                            this@LeaderboardActivity,
                                        )
                                ) {
                                    23f
                                } else {
                                    27f
                                }
                            setTypeface(
                                Typeface.DEFAULT_BOLD,
                            )
                            setTextColor(
                                Color.WHITE,
                            )
                            setSingleLine(
                                true,
                            )
                        },
                    )

                    addView(
                        TextView(
                            this@LeaderboardActivity,
                        ).apply {
                            text =
                                if (
                                    showingCached
                                ) {
                                    "VERIFIED ONLINE • SAVED"
                                } else {
                                    "VERIFIED ONLINE • ALL TIME"
                                }
                            textSize =
                                10f
                            setTypeface(
                                Typeface.DEFAULT_BOLD,
                            )
                            setTextColor(
                                0xFF6EEBFF.toInt(),
                            )
                            setPadding(
                                0,
                                dp(2),
                                0,
                                0,
                            )
                        },
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart =
                        dp(12)
                    marginEnd =
                        dp(8)
                },
            )

            addView(
                Button(
                    this@LeaderboardActivity,
                ).apply {
                    LudoProofTheme
                        .homeCircularAction(
                            this,
                            "↻",
                        )
                    contentDescription =
                        "Refresh leaderboard"
                    isEnabled =
                        !loading
                    alpha =
                        if (
                            loading
                        ) {
                            0.55f
                        } else {
                            1f
                        }
                    setOnClickListener {
                        refreshLeaderboard()
                    }
                },
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(48),
                ),
            )
        }

    private fun currentPlayerCard():
        LinearLayout {
        val viewer =
            snapshot
                ?.viewer
        val localProfile =
            ProfileStore(
                this,
            ).snapshot()
        val displayName =
            viewer
                ?.displayName
                ?: localProfile
                    .displayName
        val avatarId =
            CosmeticInventoryStore(
                this,
            ).selectedId(
                CosmeticCategory.AVATAR,
            )
        val avatarSymbol =
            StoreCosmeticCatalog
                .find(
                    avatarId,
                )
                ?.previewSymbol
                ?: initials(
                    displayName,
                )

        return LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14),
            )
            background =
                LudoProofTheme
                    .darkPanelDrawable(
                        this@LeaderboardActivity,
                        goldBorder =
                            true,
                    )
            elevation =
                dp(7)
                    .toFloat()

            addView(
                LinearLayout(
                    this@LeaderboardActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL

                    addView(
                        TextView(
                            this@LeaderboardActivity,
                        ).apply {
                            text =
                                avatarSymbol
                            textSize =
                                20f
                            setTypeface(
                                Typeface.DEFAULT_BOLD,
                            )
                            setTextColor(
                                Color.WHITE,
                            )
                            gravity =
                                Gravity.CENTER
                            background =
                                GradientDrawable(
                                    GradientDrawable
                                        .Orientation
                                        .TOP_BOTTOM,
                                    intArrayOf(
                                        0xFF2C91FF.toInt(),
                                        0xFF0756BE.toInt(),
                                        0xFF062B72.toInt(),
                                    ),
                                ).apply {
                                    shape =
                                        GradientDrawable.OVAL
                                    setStroke(
                                        dp(2),
                                        LudoProofTheme.GOLD,
                                    )
                                }
                        },
                        LinearLayout.LayoutParams(
                            dp(68),
                            dp(68),
                        ),
                    )

                    addView(
                        LinearLayout(
                            this@LeaderboardActivity,
                        ).apply {
                            orientation =
                                LinearLayout.VERTICAL

                            addView(
                                TextView(
                                    this@LeaderboardActivity,
                                ).apply {
                                    text =
                                        displayName
                                    textSize =
                                        20f
                                    setTypeface(
                                        Typeface.DEFAULT_BOLD,
                                    )
                                    setTextColor(
                                        Color.WHITE,
                                    )
                                    maxLines =
                                        1
                                    ellipsize =
                                        TextUtils
                                            .TruncateAt
                                            .END
                                },
                            )

                            addView(
                                TextView(
                                    this@LeaderboardActivity,
                                ).apply {
                                    text =
                                        if (
                                            viewer !=
                                            null
                                        ) {
                                            "YOUR RANK  #" +
                                                viewer.rank
                                        } else {
                                            "UNRANKED • COMPLETE A VERIFIED ONLINE MATCH"
                                        }
                                    textSize =
                                        11f
                                    setTypeface(
                                        Typeface.DEFAULT_BOLD,
                                    )
                                    setTextColor(
                                        if (
                                            viewer !=
                                            null
                                        ) {
                                            LudoProofTheme.GOLD
                                        } else {
                                            LudoProofTheme.TEXT_DIM
                                        },
                                    )
                                    setPadding(
                                        0,
                                        dp(5),
                                        0,
                                        0,
                                    )
                                },
                            )
                        },
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f,
                        ).apply {
                            marginStart =
                                dp(12)
                        },
                    )
                },
            )

            addView(
                metricRow(
                    viewer,
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin =
                        dp(14)
                },
            )
        }
    }

    private fun metricRow(
        viewer: LeaderboardEntry?,
    ):
        LinearLayout =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER

            listOf(
                "POINTS" to
                    number(
                        viewer
                            ?.points
                            ?: 0,
                    ),
                "WINS" to
                    (
                        viewer
                            ?.wins
                            ?: 0
                        ).toString(),
                "GAMES" to
                    (
                        viewer
                            ?.games
                            ?: 0
                        ).toString(),
            ).forEachIndexed {
                    index,
                    item ->
                addView(
                    metricCell(
                        item.first,
                        item.second,
                    ),
                    LinearLayout.LayoutParams(
                        0,
                        dp(58),
                        1f,
                    ).apply {
                        if (
                            index >
                            0
                        ) {
                            marginStart =
                                dp(7)
                        }
                    },
                )
            }
        }

    private fun metricCell(
        label: String,
        value: String,
    ):
        LinearLayout =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            gravity =
                Gravity.CENTER
            background =
                LudoProofTheme
                    .rounded(
                        0xC9092C69.toInt(),
                        12f,
                        0x554FDFFF,
                        1f,
                        this@LeaderboardActivity,
                    )

            addView(
                TextView(
                    this@LeaderboardActivity,
                ).apply {
                    text =
                        value
                    textSize =
                        18f
                    setTypeface(
                        Typeface.DEFAULT_BOLD,
                    )
                    setTextColor(
                        Color.WHITE,
                    )
                    gravity =
                        Gravity.CENTER
                },
            )
            addView(
                TextView(
                    this@LeaderboardActivity,
                ).apply {
                    text =
                        label
                    textSize =
                        9f
                    setTypeface(
                        Typeface.DEFAULT_BOLD,
                    )
                    setTextColor(
                        0xFF83DFFF.toInt(),
                    )
                    gravity =
                        Gravity.CENTER
                },
            )
        }

    private fun scoringStrip():
        TextView {
        val finish =
            snapshot
                ?.finishPoints
                ?: 20
        val bonus =
            snapshot
                ?.winBonus
                ?: 30

        return TextView(
            this,
        ).apply {
            text =
                "SERVER-VERIFIED SCORING  •  FINISH +" +
                    finish +
                    "  •  WIN +" +
                    bonus +
                    " BONUS"
            textSize =
                10f
            setTypeface(
                Typeface.DEFAULT_BOLD,
            )
            setTextColor(
                0xFFBFEAFF.toInt(),
            )
            gravity =
                Gravity.CENTER
            setPadding(
                dp(10),
                dp(10),
                dp(10),
                dp(10),
            )
            background =
                LudoProofTheme
                    .rounded(
                        0xB507255E.toInt(),
                        12f,
                        0x445FDFFF,
                        1f,
                        this@LeaderboardActivity,
                    )
        }
    }

    private fun rankingHeader():
        LinearLayout =
        LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL

            addView(
                TextView(
                    this@LeaderboardActivity,
                ).apply {
                    text =
                        "GLOBAL RANKING"
                    textSize =
                        16f
                    setTypeface(
                        Typeface.DEFAULT_BOLD,
                    )
                    setTextColor(
                        Color.WHITE,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )

            addView(
                TextView(
                    this@LeaderboardActivity,
                ).apply {
                    text =
                        "ALL TIME"
                    textSize =
                        10f
                    setTypeface(
                        Typeface.DEFAULT_BOLD,
                    )
                    setTextColor(
                        LudoProofTheme.GOLD,
                    )
                    gravity =
                        Gravity.CENTER
                    setPadding(
                        dp(10),
                        dp(6),
                        dp(10),
                        dp(6),
                    )
                    background =
                        LudoProofTheme
                            .rounded(
                                0xE907255E.toInt(),
                                999f,
                                0x88FFC62E.toInt(),
                                1f,
                                this@LeaderboardActivity,
                            )
                },
            )
        }

    private fun rankingRow(
        entry: LeaderboardEntry,
    ):
        LinearLayout {
        val border =
            when {
                entry.isViewer ->
                    0xFF5DE6FF.toInt()
                entry.rank ==
                    1 ->
                    0xFFFFD33D.toInt()
                entry.rank ==
                    2 ->
                    0xFFC8D8EA.toInt()
                entry.rank ==
                    3 ->
                    0xFFD18A50.toInt()
                else ->
                    0x6659BFFF
                        .toInt()
            }

        return LinearLayout(
            this,
        ).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL
            minimumHeight =
                dp(
                    if (
                        entry.rank <=
                        3
                    ) {
                        78
                    } else {
                        70
                    },
                )
            setPadding(
                dp(10),
                dp(9),
                dp(10),
                dp(9),
            )
            background =
                LudoProofTheme
                    .rounded(
                        if (
                            entry.isViewer
                        ) {
                            0xE50B3B83.toInt()
                        } else {
                            0xDC0B3278.toInt()
                        },
                        15f,
                        border,
                        if (
                            entry.rank <=
                                3 ||
                            entry.isViewer
                        ) {
                            1.8f
                        } else {
                            1f
                        },
                        this@LeaderboardActivity,
                    )
            elevation =
                dp(
                    if (
                        entry.rank <=
                        3
                    ) {
                        5
                    } else {
                        2
                    },
                ).toFloat()

            addView(
                TextView(
                    this@LeaderboardActivity,
                ).apply {
                    text =
                        "#" +
                            entry.rank
                    textSize =
                        if (
                            entry.rank <=
                            3
                        ) {
                            16f
                        } else {
                            14f
                        }
                    setTypeface(
                        Typeface.DEFAULT_BOLD,
                    )
                    setTextColor(
                        border,
                    )
                    gravity =
                        Gravity.CENTER
                },
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(48),
                ),
            )

            addView(
                TextView(
                    this@LeaderboardActivity,
                ).apply {
                    text =
                        initials(
                            entry.displayName,
                        )
                    textSize =
                        15f
                    setTypeface(
                        Typeface.DEFAULT_BOLD,
                    )
                    setTextColor(
                        Color.WHITE,
                    )
                    gravity =
                        Gravity.CENTER
                    background =
                        GradientDrawable(
                            GradientDrawable
                                .Orientation
                                .TOP_BOTTOM,
                            intArrayOf(
                                0xFF278DEB.toInt(),
                                0xFF0A55B2.toInt(),
                            ),
                        ).apply {
                            shape =
                                GradientDrawable.OVAL
                            setStroke(
                                dp(1),
                                border,
                            )
                        }
                },
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(48),
                ).apply {
                    marginEnd =
                        dp(10)
                },
            )

            addView(
                LinearLayout(
                    this@LeaderboardActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL

                    addView(
                        TextView(
                            this@LeaderboardActivity,
                        ).apply {
                            text =
                                entry.displayName +
                                if (
                                    entry.isViewer
                                ) {
                                    "  • YOU"
                                } else {
                                    ""
                                }
                            textSize =
                                16f
                            setTypeface(
                                Typeface.DEFAULT_BOLD,
                            )
                            setTextColor(
                                Color.WHITE,
                            )
                            maxLines =
                                1
                            ellipsize =
                                TextUtils
                                    .TruncateAt
                                    .END
                        },
                    )

                    val winRate =
                        if (
                            entry.games >
                            0
                        ) {
                            entry.wins *
                                100 /
                                entry.games
                        } else {
                            0
                        }
                    addView(
                        TextView(
                            this@LeaderboardActivity,
                        ).apply {
                            text =
                                "W " +
                                    entry.wins +
                                    "  •  G " +
                                    entry.games +
                                    "  •  " +
                                    winRate +
                                    "% win rate"
                            textSize =
                                11f
                            setTextColor(
                                LudoProofTheme.TEXT_MUTED,
                            )
                            setPadding(
                                0,
                                dp(4),
                                0,
                                0,
                            )
                        },
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )

            addView(
                LinearLayout(
                    this@LeaderboardActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                    gravity =
                        Gravity.END or
                            Gravity.CENTER_VERTICAL

                    addView(
                        TextView(
                            this@LeaderboardActivity,
                        ).apply {
                            text =
                                number(
                                    entry.points,
                                )
                            textSize =
                                16f
                            setTypeface(
                                Typeface.DEFAULT_BOLD,
                            )
                            setTextColor(
                                if (
                                    entry.rank ==
                                    1
                                ) {
                                    LudoProofTheme.GOLD
                                } else {
                                    Color.WHITE
                                },
                            )
                            gravity =
                                Gravity.END
                        },
                    )
                    addView(
                        TextView(
                            this@LeaderboardActivity,
                        ).apply {
                            text =
                                "PTS"
                            textSize =
                                9f
                            setTypeface(
                                Typeface.DEFAULT_BOLD,
                            )
                            setTextColor(
                                0xFF77DEFF.toInt(),
                            )
                            gravity =
                                Gravity.END
                        },
                    )
                },
                LinearLayout.LayoutParams(
                    dp(76),
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
    }

    private fun loadingPanel():
        TextView =
        messagePanel(
            "Loading verified rankings…",
            LudoProofTheme.TEXT_MUTED,
        )

    private fun emptyPanel():
        TextView =
        messagePanel(
            "No ranked players yet. Finish a verified online match to enter the leaderboard.",
            LudoProofTheme.TEXT_MUTED,
        )

    private fun statusPanel(
        message: String,
    ):
        TextView =
        messagePanel(
            message,
            LudoProofTheme.GOLD,
        )

    private fun messagePanel(
        message: String,
        color: Int,
    ):
        TextView =
        TextView(
            this,
        ).apply {
            text =
                message
            textSize =
                12f
            setTextColor(
                color,
            )
            gravity =
                Gravity.CENTER
            setPadding(
                dp(14),
                dp(16),
                dp(14),
                dp(16),
            )
            background =
                LudoProofTheme
                    .rounded(
                        0xC708265E.toInt(),
                        14f,
                        0x445FDFFF,
                        1f,
                        this@LeaderboardActivity,
                    )
        }

    private fun initials(
        name: String,
    ): String {
        val parts =
            name.trim()
                .split(
                    Regex(
                        "\\s+",
                    ),
                )
                .filter {
                    it.isNotBlank()
                }

        return when {
            parts.size >=
                2 ->
                (
                    parts[0]
                        .take(
                            1,
                        ) +
                        parts[1]
                            .take(
                                1,
                            )
                    ).uppercase()

            parts.size ==
                1 ->
                parts[0]
                    .take(
                        2,
                    )
                    .uppercase()

            else ->
                "LP"
        }
    }

    private fun number(
        value: Int,
    ): String =
        NumberFormat
            .getIntegerInstance()
            .format(
                value,
            )

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
    ): Int =
        LudoProofTheme
            .dp(
                this,
                value,
            )
}
