package me.modify.pocketworld.world;

public enum WorldRank {
    OWNER {
        @Override
        public String toString() {
            return "owner";
        }
    },

    MOD {
        @Override
        public String toString() {
            return "mod";
        }
    },

    MEMBER {
        @Override
        public String toString() {
            return "member";
        }
    };

    public static String getEnumClassName() {
        return values()[0].getDeclaringClass().getSimpleName();
    }
}
