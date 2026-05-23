package dev.narek.pveauction.model;

public final class ClanPermissions {

    public static final int INVITE = 1;
    public static final int KICK = 2;
    public static final int WITHDRAW = 4;
    public static final int ALL = INVITE | KICK | WITHDRAW;

    private ClanPermissions() {}

    public static boolean has(int mask, int flag) {
        return (mask & flag) == flag;
    }
}
