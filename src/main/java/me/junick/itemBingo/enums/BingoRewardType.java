package me.junick.itemBingo.enums;

public enum BingoRewardType  {
    DIAMOND("§b다이아몬드 포인트"),
    SLOT("§a빙고칸 포인트"),
    LINE("§e빙고줄 포인트");

    private final String displayName;

    BingoRewardType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
