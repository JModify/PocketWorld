package com.pocketworld.plugin.world;

/**
 * The tiers a per-world {@link WorldAction} permission can be configured for - parallel to, but
 * distinct from, {@link WorldRank}. A player's effective tier here is their {@link WorldRank} if
 * they're a member of the world, or {@link #VISITOR} if they're physically present but not a
 * member at all. {@code OWNER} deliberately has no entry here: owners always have every
 * permission implicitly and that's never configurable, so there's nothing to store for them.
 */
public enum PermissionRank {

    /** Anyone physically in the world who isn't a member at any rank. */
    VISITOR,
    MEMBER,
    MOD
}
