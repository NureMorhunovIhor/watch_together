package com.example.watch_together.direct.service;

import java.util.Set;

public class DirectStickerRegistry {

    private static final Set<String> FREE_STICKERS = Set.of(
            "free_smile",
            "free_laugh",
            "free_sad",
            "free_angry",
            "free_thumbsup",
            "free_heart"
    );

    private static final Set<String> PREMIUM_STICKERS = Set.of(
            "premium_fire",
            "premium_popcorn",
            "premium_crown",
            "premium_star",
            "premium_rocket",
            "premium_heart"
    );

    public static boolean exists(String stickerId) {
        return FREE_STICKERS.contains(stickerId) || PREMIUM_STICKERS.contains(stickerId);
    }

    public static boolean isPremium(String stickerId) {
        return PREMIUM_STICKERS.contains(stickerId);
    }
}