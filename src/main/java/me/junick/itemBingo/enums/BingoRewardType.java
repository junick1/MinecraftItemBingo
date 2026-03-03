package me.junick.itemBingo.enums;

public enum BingoRewardType  {
    DIAMOND("§b다이아몬드 포인트", false),
    SLOT("§a빙고칸 포인트", true),
    LINE("§e빙고줄 포인트", false);

    private final String displayName;
    private final boolean isPersonal;

    BingoRewardType(String displayName, boolean isPersonal) {
        this.displayName = displayName;
        this.isPersonal = isPersonal;
    }

    public String getDisplayName() { return displayName; }

    public boolean isPersonal() { return isPersonal; }
}
