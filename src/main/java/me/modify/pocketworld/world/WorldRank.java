package me.modify.pocketworld.world;

public enum WorldRank {
    /**
     * Highest world rank.
     * Indicates ownership of a PocketWorld with all management permissions.
     */
    OWNER {
        @Override
        public String toString() {
            return "owner";
        }
    },

    /**
     * Moderator world rank.
     * Allows inviting/kicking players from a pocket world.
     */
    MOD {
        @Override
        public String toString() {
            return "mod";
        }
    },

    /**
     * Default world rank.
     * Members of a PocketWorld do not have any higher permissions.
     */
    MEMBER {
        @Override
        public String toString() {
            return "member";
        }
    };
}
