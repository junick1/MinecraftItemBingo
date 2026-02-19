package me.junick.itemBingo.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BingoBoard {
    private int width;
    private int height;
    private List<ItemStack> items;

    public BingoBoard(int width, int height, List<ItemStack> items) {
        this.width = width;
        this.height = height;
        this.items = items;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public BingoBoard() {}
}
