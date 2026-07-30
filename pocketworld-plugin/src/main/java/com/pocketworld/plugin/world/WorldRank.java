package com.pocketworld.plugin.world;

public enum WorldRank {

    /** Highest world rank: ownership of a PocketWorld with all management permissions. */
    OWNER("owner"),

    /** Allows inviting/kicking players from a pocket world. */
    MOD("mod"),

    /** Default world rank; no elevated permissions. */
    MEMBER("member");

    private final String label;

    WorldRank(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
