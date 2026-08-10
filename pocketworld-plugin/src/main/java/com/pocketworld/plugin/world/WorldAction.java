package com.pocketworld.plugin.world;

/**
 * A single configurable ability within a pocket world, gated per {@link PermissionRank}. {@code
 * BUILD}/{@code BREAK}/{@code INTERACT} are the physical-interaction permissions that matter for
 * {@link PermissionRank#VISITOR}; {@code INVITE}/{@code KICK}/{@code SET_SPAWN} are the ones that
 * matter for {@link PermissionRank#MEMBER}/{@link PermissionRank#MOD}. Setting another member's
 * rank is deliberately not here at all - that stays an owner-only action, never configurable.
 */
public enum WorldAction {
    BUILD,
    BREAK,
    INTERACT,
    INVITE,
    KICK,
    SET_SPAWN
}
