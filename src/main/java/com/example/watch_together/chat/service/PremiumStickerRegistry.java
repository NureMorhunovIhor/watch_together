package com.example.watch_together.chat.service;

import java.util.Set;

public class PremiumStickerRegistry {

    private static final Set<String> STICKERS = Set.of(
            "premium_fire",
            "premium_popcorn",
            "premium_crown",
            "premium_star",
            "premium_rocket",
            "premium_heart"
    );

    public static boolean exists(String stickerId) {
        return stickerId != null && STICKERS.contains(stickerId);
    }
}