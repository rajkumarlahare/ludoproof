package com.ludoproof.game.feature.store.domain

import com.ludoproof.game.feature.store.domain.model.CosmeticCategory
import com.ludoproof.game.feature.store.domain.model.CosmeticUnlockKind
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic

object StoreCosmeticCatalog {
    const val DEFAULT_BOARD =
        "board_classic"
    const val DEFAULT_DICE =
        "dice_classic"
    const val DEFAULT_AVATAR =
        "avatar_4"

    val boards =
        listOf(
            cosmetic("board_classic","CLASSIC",CosmeticCategory.BOARD,CosmeticUnlockKind.FREE,"CL",0xFFF7F4EC.toInt(),0xFF2C8DD8.toInt()),
            cosmetic("board_checkers","CHECKERS",CosmeticCategory.BOARD,CosmeticUnlockKind.FREE,"CK",0xFFF5EEE2.toInt(),0xFF914C35.toInt()),
            cosmetic("board_chess","CHESS",CosmeticCategory.BOARD,CosmeticUnlockKind.REWARDED_ADS,"CH",0xFFF1E3C4.toInt(),0xFF51463C.toInt(),ads=5),
            cosmetic("board_diwali","DIWALI",CosmeticCategory.BOARD,CosmeticUnlockKind.REWARDED_ADS,"DI",0xFFFFC84A.toInt(),0xFF8B2A71.toInt(),ads=10),
            cosmetic("board_denim","DENIM",CosmeticCategory.BOARD,CosmeticUnlockKind.REWARDED_ADS,"DE",0xFF2D83C5.toInt(),0xFF15537B.toInt(),ads=10),
            cosmetic("board_neon","NEON",CosmeticCategory.BOARD,CosmeticUnlockKind.REWARDED_ADS,"NE",0xFF10131C.toInt(),0xFF36F7E5.toInt(),level=10,ads=20),
            cosmetic("board_pirate","PIRATE",CosmeticCategory.BOARD,CosmeticUnlockKind.GEMS,"PI",0xFFC9A86A.toInt(),0xFF4E7E91.toInt(),gems=200,level=16),
            cosmetic("board_alien","ALIEN",CosmeticCategory.BOARD,CosmeticUnlockKind.GEMS,"AL",0xFF1E243D.toInt(),0xFF4EDA73.toInt(),gems=250,level=22),
            cosmetic("board_penguin","PENGUIN",CosmeticCategory.BOARD,CosmeticUnlockKind.GEMS,"PE",0xFFDFF6FF.toInt(),0xFF4D9CC8.toInt(),gems=250),
        )

    val dice =
        listOf(
            cosmetic("dice_classic","CLASSIC",CosmeticCategory.DICE,CosmeticUnlockKind.FREE,"⚄",0xFFF7F7F7.toInt(),0xFF1B2838.toInt()),
            cosmetic("dice_pumpkin","PUMPKIN",CosmeticCategory.DICE,CosmeticUnlockKind.FREE,"1",0xFFD9802F.toInt(),0xFF6A351C.toInt()),
            cosmetic("dice_diwali","DIWALI",CosmeticCategory.DICE,CosmeticUnlockKind.REWARDED_ADS,"6",0xFFB21C32.toInt(),0xFFFFC832.toInt(),ads=5),
            cosmetic("dice_football","FOOTBALL",CosmeticCategory.DICE,CosmeticUnlockKind.REWARDED_ADS,"6",0xFFF5F5F5.toInt(),0xFF222222.toInt(),level=18,ads=10),
            cosmetic("dice_cricket","CRICKET",CosmeticCategory.DICE,CosmeticUnlockKind.REWARDED_ADS,"1",0xFF4F9E5F.toInt(),0xFFE8F4D8.toInt(),ads=20),
            cosmetic("dice_summers","SUMMERS",CosmeticCategory.DICE,CosmeticUnlockKind.REWARDED_ADS,"3",0xFFFFF1D4.toInt(),0xFFE84432.toInt(),level=12,ads=30),
            cosmetic("dice_colors","COLORS",CosmeticCategory.DICE,CosmeticUnlockKind.GEMS,"3",0xFFFFD63B.toInt(),0xFFEF3F8A.toInt(),gems=200,level=24),
            cosmetic("dice_heart","HEART",CosmeticCategory.DICE,CosmeticUnlockKind.GEMS,"♥",0xFFF8F8F8.toInt(),0xFFD82E3D.toInt(),gems=250),
        )

    val avatars =
        listOf(
            cosmetic("avatar_9","Avatar 9",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A9",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_4","Avatar 4",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A4",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_3","Avatar 3",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A3",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_5","Avatar 5",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A5",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_6","Avatar 6",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A6",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_7","Avatar 7",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A7",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_1","Avatar 1",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A1",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_8","Avatar 8",CosmeticCategory.AVATAR,CosmeticUnlockKind.FREE,"A8",0xFF1E91E8.toInt(),0xFF073F97.toInt()),
            cosmetic("avatar_2","Avatar 2",CosmeticCategory.AVATAR,CosmeticUnlockKind.GEMS,"A2",0xFF7D3FC1.toInt(),0xFF33206E.toInt(),gems=500,level=30),
            cosmetic("avatar_10","Avatar 10",CosmeticCategory.AVATAR,CosmeticUnlockKind.GEMS,"A10",0xFFE14F7C.toInt(),0xFF79203D.toInt(),gems=500,level=30),
        )

    val all =
        boards +
            dice +
            avatars

    val defaultOwnedIds =
        all
            .filter {
                it.unlockKind ==
                    CosmeticUnlockKind.FREE
            }
            .map {
                it.id
            }
            .toSet()

    fun find(
        id: String,
    ): StoreCosmetic? =
        all.firstOrNull {
            it.id ==
                id
        }

    fun forCategory(
        category: CosmeticCategory,
    ): List<StoreCosmetic> =
        when (category) {
            CosmeticCategory.BOARD -> boards
            CosmeticCategory.DICE -> dice
            CosmeticCategory.AVATAR -> avatars
        }

    fun defaultId(
        category: CosmeticCategory,
    ): String =
        when (category) {
            CosmeticCategory.BOARD -> DEFAULT_BOARD
            CosmeticCategory.DICE -> DEFAULT_DICE
            CosmeticCategory.AVATAR -> DEFAULT_AVATAR
        }

    private fun cosmetic(
        id: String,
        title: String,
        category: CosmeticCategory,
        unlockKind: CosmeticUnlockKind,
        previewSymbol: String,
        previewPrimary: Int,
        previewSecondary: Int,
        gems: Int = 0,
        level: Int = 0,
        ads: Int = 0,
    ) =
        StoreCosmetic(
            id = id,
            title = title,
            category = category,
            unlockKind = unlockKind,
            gemPrice = gems,
            requiredLevel = level,
            requiredAdViews = ads,
            previewSymbol = previewSymbol,
            previewPrimary = previewPrimary,
            previewSecondary = previewSecondary,
        )
}
