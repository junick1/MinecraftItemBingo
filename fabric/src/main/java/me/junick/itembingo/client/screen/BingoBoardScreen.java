package me.junick.itembingo.client.screen;

import me.junick.itembingo.client.Keybinds;
import me.junick.itembingo.client.net.ClientNetworking;
import me.junick.itembingo.client.net.ModProtocol;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.CellState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Fullscreen pannable bingo board.
 *
 * <p>Controls: drag the board to pan, scroll to pan vertically, Shift+scroll
 * horizontally, Ctrl+scroll to zoom. Click an inventory item to pick it up and
 * drop it on a cell to submit; Shift+click auto-submits to the first matching
 * cell. All submissions are requests — the server validates and the pushed
 * board update is the only thing that changes what's shown.
 */
public class BingoBoardScreen extends Screen {
    private static final int HEADER_H = 24;
    private static final int SLOT = 18;
    private static final int INV_COLS = 9;

    private final BoardCamera camera = new BoardCamera();
    private int seenRevision = -1;

    /** Inventory slot being dragged (vanilla index 0-35), or -1. */
    private int draggingSlot = -1;
    private ItemStack draggingStack = ItemStack.EMPTY;
    private boolean panning;

    public BingoBoardScreen() {
        super(Component.translatable("itembingo.screen.title"));
    }

    /* ------------------------- layout ------------------------- */

    private int invLeft() {
        return (width - INV_COLS * SLOT) / 2;
    }

    /** Top of the inventory strip: 3 main rows + gap + hotbar + padding. */
    private int invTop() {
        return height - (3 * SLOT + 4 + SLOT + 6);
    }

    private int boardTop() {
        return HEADER_H;
    }

    private int boardBottom() {
        return invTop() - 4;
    }

    /** Vanilla inventory index (0-35) at a screen point, or -1. */
    private int slotAt(double mx, double my) {
        int left = invLeft();
        int top = invTop();
        if (mx < left || mx >= left + INV_COLS * SLOT) return -1;
        int col = (int) ((mx - left) / SLOT);
        // Three main-inventory rows first (vanilla 9-35), then the hotbar (0-8).
        for (int r = 0; r < 3; r++) {
            int rowY = top + r * SLOT;
            if (my >= rowY && my < rowY + SLOT) return 9 + r * 9 + col;
        }
        int hotbarY = top + 3 * SLOT + 4;
        if (my >= hotbarY && my < hotbarY + SLOT) return col;
        return -1;
    }

    private boolean inBoardArea(double mx, double my) {
        return my >= boardTop() && my < boardBottom();
    }

    private int cellIndexAt(double mx, double my) {
        if (!inBoardArea(mx, my)) return -1;
        int col = camera.colAt(mx, 0);
        int row = camera.rowAt(my, boardTop());
        if (col < 0 || row < 0) return -1;
        return row * BoardClientState.width() + col;
    }

    /* ------------------------- lifecycle ------------------------- */

    @Override
    protected void init() {
        syncCamera(true);
    }

    private void syncCamera(boolean attach) {
        int w = Math.max(1, BoardClientState.width());
        int h = Math.max(1, BoardClientState.height());
        if (attach) {
            camera.attach(w, h, width, boardBottom() - boardTop());
        } else {
            camera.boardChanged(w, h);
            camera.resize(width, boardBottom() - boardTop());
        }
        seenRevision = BoardClientState.revision();
    }

    @Override
    public void onClose() {
        camera.save();
        BoardClientState.setHudViewport(camera.visibleTopLeftCol(), camera.visibleTopLeftRow());
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /* ------------------------- input ------------------------- */

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && BoardClientState.hasBoard()) {
            int slot = slotAt(event.x(), event.y());
            if (slot >= 0) {
                ItemStack stack = playerStack(slot);
                if (!stack.isEmpty()) {
                    if (hasShift()) {
                        ClientNetworking.sendSubmit(ModProtocol.SUBMIT_SHIFT, -1, slot, keyOf(stack));
                    } else {
                        draggingSlot = slot;
                        draggingStack = stack.copy();
                    }
                    return true;
                }
                return true; // empty slot: swallow the click
            }
            if (inBoardArea(event.x(), event.y())) {
                panning = true;
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panning) {
            camera.pan(dx, dy);
            return true;
        }
        if (draggingSlot >= 0) {
            return true; // ghost item follows the cursor in render()
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (panning) {
                panning = false;
                return true;
            }
            if (draggingSlot >= 0) {
                int idx = cellIndexAt(event.x(), event.y());
                if (idx >= 0 && !draggingStack.isEmpty()) {
                    ClientNetworking.sendSubmit(ModProtocol.SUBMIT_DIRECT, idx, draggingSlot, keyOf(draggingStack));
                }
                draggingSlot = -1;
                draggingStack = ItemStack.EMPTY;
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hasCtrl()) {
            camera.zoomAt(mouseX, mouseY - boardTop(), scrollY);
        } else if (hasShift()) {
            camera.pan((scrollY + scrollX) * BoardCamera.CELL * camera.zoom / 2.0, 0);
        } else {
            camera.pan(scrollX * BoardCamera.CELL * camera.zoom / 2.0,
                    scrollY * BoardCamera.CELL * camera.zoom / 2.0);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Keybinds.openBoard != null && Keybinds.openBoard.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean hasShift() {
        return minecraft().hasShiftDown();
    }

    private boolean hasCtrl() {
        return minecraft().hasControlDown();
    }

    private Minecraft minecraft() {
        return this.minecraft != null ? this.minecraft : Minecraft.getInstance();
    }

    private ItemStack playerStack(int slot) {
        var player = minecraft().player;
        return player == null ? ItemStack.EMPTY : player.getInventory().getItem(slot);
    }

    private static String keyOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /* ------------------------- rendering ------------------------- */

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (BoardClientState.revision() != seenRevision) {
            syncCamera(false);
        }

        g.fill(0, 0, width, height, 0xC8101014);
        renderHeader(g);

        if (!BoardClientState.hasBoard()) {
            String key = BoardClientState.status() == ModProtocol.STATUS_VIEW_DENIED
                    ? "itembingo.screen.view_denied" : "itembingo.screen.no_board";
            g.centeredText(font, Component.translatable(key), width / 2, height / 2, 0xFFAAAAAA);
            return;
        }

        renderBoard(g, mouseX, mouseY);
        renderInventory(g, mouseX, mouseY);

        if (draggingSlot >= 0 && !draggingStack.isEmpty()) {
            g.nextStratum();
            g.item(draggingStack, mouseX - 8, mouseY - 8);
        }
    }

    private void renderHeader(GuiGraphicsExtractor g) {
        g.fill(0, 0, width, HEADER_H, 0xE0141418);
        g.text(font, title, 8, (HEADER_H - 9) / 2, 0xFFFFFFFF);

        if (BoardClientState.hasBoard()) {
            Component progress = Component.translatable("itembingo.screen.progress",
                    BoardClientState.submittedCount(), BoardClientState.totalCells());
            Component mode = Component.translatable(modeKey(BoardClientState.gameMode()));
            Component right = BoardClientState.fogSubmitLock()
                    ? Component.empty().append(progress).append("  ").append(mode)
                            .append("  ").append(Component.translatable("itembingo.screen.fog_submit_lock"))
                    : Component.empty().append(progress).append("  ").append(mode);
            g.text(font, right, width - font.width(right) - 8, (HEADER_H - 9) / 2, 0xFFB0FFB0);

            Component hint = Component.translatable("itembingo.screen.hint");
            g.text(font, hint, 8, boardBottom() - 12, 0x90FFFFFF);
        }
    }

    private static String modeKey(byte mode) {
        return switch (mode) {
            case 1 -> "itembingo.screen.mode.swappage";
            case 2 -> "itembingo.screen.mode.fog_of_war";
            case 3 -> "itembingo.screen.mode.lockout";
            default -> "itembingo.screen.mode.normal";
        };
    }

    private void renderBoard(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int top = boardTop();
        int bottom = boardBottom();
        g.enableScissor(0, top, width, bottom);

        int w = BoardClientState.width();
        int h = BoardClientState.height();
        double size = camera.cellScreenSize();
        int hoveredIdx = (draggingSlot >= 0 || panning || !inBoardArea(mouseX, mouseY))
                ? (draggingSlot >= 0 ? cellIndexAt(mouseX, mouseY) : -1)
                : cellIndexAt(mouseX, mouseY);

        for (int row = 0; row < h; row++) {
            int y = (int) Math.round(camera.cellScreenY(row, top));
            if (y + size < top || y > bottom) continue;
            for (int col = 0; col < w; col++) {
                int x = (int) Math.round(camera.cellScreenX(col, 0));
                if (x + size < 0 || x > width) continue;
                CellState cell = BoardClientState.cell(col, row);
                if (cell == null) continue;
                renderCell(g, cell, x, y, (int) Math.round(size),
                        hoveredIdx == row * w + col, mouseX, mouseY);
            }
        }

        g.disableScissor();
    }

    private void renderCell(GuiGraphicsExtractor g, CellState cell, int x, int y, int size,
                            boolean hovered, int mouseX, int mouseY) {
        int bg = switch (cell.kind()) {
            case ModProtocol.CELL_HIDDEN -> 0xE0181820;
            case ModProtocol.CELL_LOCKED -> 0x80581414;
            case ModProtocol.CELL_SUBMITTED -> 0x601E5A28;
            default -> 0x60000000;
        };
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, bg);
        g.outline(x, y, size, size, hovered ? 0xFFFFFFFF : 0xFF3C3C46);

        switch (cell.kind()) {
            case ModProtocol.CELL_HIDDEN ->
                    g.centeredText(font, "?", x + size / 2, y + (size - 9) / 2, 0xFF666677);
            case ModProtocol.CELL_LOCKED -> {
                drawScaledItem(g, new ItemStack(Items.BARRIER), x, y, size);
                if (hovered) {
                    g.setTooltipForNextFrame(Component.translatable("itembingo.cell.locked"), mouseX, mouseY);
                }
            }
            case ModProtocol.CELL_VISIBLE, ModProtocol.CELL_SUBMITTED -> {
                if (cell.item() != null) {
                    drawScaledItem(g, new ItemStack(cell.item()), x, y, size);
                } else {
                    g.centeredText(font, "?", x + size / 2, y + (size - 9) / 2, 0xFFFFCC44);
                }
                if (cell.isSubmitted()) {
                    // Green check badge in the corner keeps the icon recognizable
                    // while making "done" readable at any zoom.
                    g.fill(x + size - 8, y + 2, x + size - 2, y + 8, 0xFF2ECC40);
                }
                if (hovered) {
                    g.setTooltipForNextFrame(cellTooltip(cell), mouseX, mouseY);
                }
            }
            default -> {}
        }
    }

    private void drawScaledItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int size) {
        float iconScale = (size - 4) / 16.0f;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x + 2, y + 2);
        pose.scale(iconScale, iconScale);
        g.item(stack, 0, 0);
        pose.popMatrix();
    }

    private List<FormattedCharSequence> cellTooltip(CellState cell) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        Component name = cell.item() != null
                ? cell.item().getName(new ItemStack(cell.item()))
                : Component.literal(String.valueOf(cell.rawItemKey()));
        lines.add(name.getVisualOrderText());
        if (cell.isSubmitted()) {
            lines.add(Component.translatable("itembingo.cell.submitted").getVisualOrderText());
            if (cell.hasSubmitter() && !cell.submitterName().isEmpty()) {
                lines.add(Component.translatable("itembingo.cell.submitted_by", cell.submitterName())
                        .getVisualOrderText());
            }
            if (cell.elapsedSeconds() >= 0) {
                lines.add(Component.translatable("itembingo.cell.time", formatElapsed(cell.elapsedSeconds()))
                        .getVisualOrderText());
            }
        }
        return lines;
    }

    private static String formatElapsed(long seconds) {
        return "%d:%02d:%02d".formatted(seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private void renderInventory(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int left = invLeft();
        int top = invTop();
        g.fill(left - 4, top - 4, left + INV_COLS * SLOT + 4, height - 2, 0xE0141418);

        int hoveredSlot = slotAt(mouseX, mouseY);
        for (int i = 0; i < 36; i++) {
            int col = i % 9;
            int rowOnScreen = i < 9 ? 3 : (i / 9) - 1; // hotbar drawn last
            int x = left + col * SLOT;
            int y = top + rowOnScreen * SLOT + (i < 9 ? 4 : 0);

            g.fill(x, y, x + SLOT, y + SLOT, i == hoveredSlot ? 0x50FFFFFF : 0x30000000);
            g.outline(x, y, SLOT, SLOT, 0xFF2A2A32);

            ItemStack stack = playerStack(i);
            if (!stack.isEmpty() && !(draggingSlot == i)) {
                g.item(stack, x + 1, y + 1);
                g.itemDecorations(font, stack, x + 1, y + 1);
            }
        }

        if (hoveredSlot >= 0 && draggingSlot < 0) {
            ItemStack stack = playerStack(hoveredSlot);
            if (!stack.isEmpty()) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                for (Component line : getTooltipFromItem(minecraft(), stack)) {
                    lines.add(line.getVisualOrderText());
                }
                lines.add(Component.translatable("itembingo.screen.submit_hint").getVisualOrderText());
                g.setTooltipForNextFrame(lines, mouseX, mouseY);
            }
        }
    }
}
