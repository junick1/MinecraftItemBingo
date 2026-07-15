package me.junick.itembingo.client.screen;

import me.junick.itembingo.client.Keybinds;
import me.junick.itembingo.client.net.ClientNetworking;
import me.junick.itembingo.client.net.ModProtocol;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.CellState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fullscreen pannable bingo board.
 *
 * <p>Board controls: drag to pan, scroll = vertical, Shift+scroll =
 * horizontal, Ctrl+scroll = zoom toward the cursor.
 *
 * <p>The inventory strip is the player's REAL inventory: clicks go through
 * vanilla container logic ({@code handleContainerInput} on menu 0), so
 * pick-up/place/split/swap behave exactly like the survival inventory —
 * including number-key swaps, F (offhand) and Q (drop). Submitting = pick an
 * item onto the cursor, then click a matching cell (the server validates and
 * consumes from the real cursor stack); Shift+click a stack auto-submits it.
 */
public class BingoBoardScreen extends Screen {
    private static final int HEADER_H = 24;
    private static final int HINT_BAR_H = 16;
    private static final int SLOT = 18;
    private static final int INV_COLS = 9;

    /** How long a freshly-submitted cell glows, in ms. */
    private static final long FLASH_MS = 700;

    private final BoardCamera camera = new BoardCamera();
    private int seenRevision = -1;
    private boolean panning;

    /** Vanilla quick-craft (drag-distribute) state, mirroring AbstractContainerScreen. */
    private boolean quickCrafting;
    private int quickCraftType; // QUICKCRAFT_TYPE_CHARITABLE (left) or _GREEDY (right)
    private final LinkedHashSet<Integer> quickCraftSlots = new LinkedHashSet<>(); // vanilla inv indexes

    private int lastMouseX;
    private int lastMouseY;

    /** Submitted indices from the previous board push, to detect new ones. */
    private Set<Integer> knownSubmitted;
    private final Map<Integer, Long> flashes = new HashMap<>();

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
        return invTop() - HINT_BAR_H;
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

    /** Vanilla inventory index → slot id in the player's InventoryMenu. */
    private static int menuSlot(int invIndex) {
        return invIndex < 9 ? 36 + invIndex : invIndex;
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
        syncBoardState(true);
    }

    private void syncBoardState(boolean attach) {
        int w = Math.max(1, BoardClientState.width());
        int h = Math.max(1, BoardClientState.height());
        if (attach) {
            camera.attach(w, h, width, boardBottom() - boardTop());
        } else {
            camera.boardChanged(w, h);
            camera.resize(width, boardBottom() - boardTop());
        }

        // Diff submitted cells so brand-new ones get a short celebratory flash.
        Set<Integer> submitted = new HashSet<>();
        int total = w * h;
        for (int i = 0; i < total; i++) {
            CellState cell = BoardClientState.cell(i);
            if (cell != null && cell.isSubmitted()) submitted.add(i);
        }
        if (knownSubmitted != null && submitted.size() >= knownSubmitted.size()) {
            long now = System.currentTimeMillis();
            for (int idx : submitted) {
                if (!knownSubmitted.contains(idx)) flashes.put(idx, now);
            }
        } else {
            flashes.clear();
        }
        knownSubmitted = submitted;

        seenRevision = BoardClientState.revision();
    }

    @Override
    public void onClose() {
        camera.save();
        BoardClientState.setHudViewport(camera.visibleTopLeftCol(), camera.visibleTopLeftRow());
        // If something is still on the cursor, close the inventory menu properly
        // so the server puts the carried stack back (vanilla close semantics).
        var player = minecraft().player;
        if (player != null && !player.inventoryMenu.getCarried().isEmpty()) {
            player.closeContainer();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /* ------------------------- input ------------------------- */

    private ItemStack carried() {
        var player = minecraft().player;
        return player == null ? ItemStack.EMPTY : player.inventoryMenu.getCarried();
    }

    /** Vanilla container click on the player's own inventory menu. */
    private void containerClick(int menuSlotId, int button, ContainerInput input) {
        var mc = minecraft();
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, menuSlotId, button, input, mc.player);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int button = event.button();
        boolean left = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean right = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;

        if ((left || right) && BoardClientState.hasBoard()) {
            int slot = slotAt(event.x(), event.y());
            if (slot >= 0) {
                ItemStack stack = playerStack(slot);
                if (carried().isEmpty()) {
                    if (left && hasShift() && !stack.isEmpty()) {
                        ClientNetworking.sendSubmit(ModProtocol.SUBMIT_SHIFT, -1, slot, keyOf(stack));
                    } else {
                        containerClick(menuSlot(slot), left ? 0 : 1, ContainerInput.PICKUP);
                    }
                } else {
                    // Carrying something: like vanilla, don't place on mouse-down —
                    // begin a quick-craft. A plain click resolves on release; a
                    // drag distributes evenly (left) or one-by-one (right).
                    quickCrafting = true;
                    quickCraftType = left
                            ? AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE
                            : AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY;
                    quickCraftSlots.clear();
                    tryAddQuickCraftSlot(slot);
                }
                return true;
            }

            if (left && inBoardArea(event.x(), event.y())) {
                ItemStack cursor = carried();
                if (!cursor.isEmpty()) {
                    int idx = cellIndexAt(event.x(), event.y());
                    CellState cell = idx >= 0 ? BoardClientState.cell(idx) : null;
                    if (cell != null && cell.isVisible()) {
                        ClientNetworking.sendSubmit(ModProtocol.SUBMIT_DIRECT, idx,
                                ModProtocol.SLOT_CURSOR, keyOf(cursor));
                        return true;
                    }
                }
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
        if (quickCrafting) {
            int slot = slotAt(event.x(), event.y());
            if (slot >= 0) tryAddQuickCraftSlot(slot);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (panning && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            panning = false;
            return true;
        }
        if (quickCrafting) {
            finishQuickCraft(event);
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Vanilla acceptance rules for adding a slot to the drag-distribute set. */
    private void tryAddQuickCraftSlot(int invIndex) {
        var player = minecraft().player;
        ItemStack cursor = carried();
        if (player == null || cursor.isEmpty()) return;
        Slot slot = player.inventoryMenu.getSlot(menuSlot(invIndex));
        if (!AbstractContainerMenu.canItemQuickReplace(slot, cursor, true)) return;
        if (!slot.mayPlace(cursor)) return;
        if (quickCraftType == AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY
                && quickCraftSlots.size() >= cursor.getCount()) return;
        quickCraftSlots.add(invIndex);
    }

    private void finishQuickCraft(MouseButtonEvent event) {
        quickCrafting = false;
        int button = quickCraftType == AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE ? 0 : 1;

        if (quickCraftSlots.size() > 1) {
            // Real drag: replay it through vanilla's QUICK_CRAFT protocol so the
            // even/one-by-one distribution is computed by the shared menu logic.
            containerClick(-999, AbstractContainerMenu.getQuickcraftMask(
                    AbstractContainerMenu.QUICKCRAFT_HEADER_START, quickCraftType), ContainerInput.QUICK_CRAFT);
            for (int invIndex : quickCraftSlots) {
                containerClick(menuSlot(invIndex), AbstractContainerMenu.getQuickcraftMask(
                        AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE, quickCraftType), ContainerInput.QUICK_CRAFT);
            }
            containerClick(-999, AbstractContainerMenu.getQuickcraftMask(
                    AbstractContainerMenu.QUICKCRAFT_HEADER_END, quickCraftType), ContainerInput.QUICK_CRAFT);
        } else {
            // Plain click (possibly a swap with an incompatible stack): apply a
            // normal PICKUP to the slot under the cursor at release time.
            int target = quickCraftSlots.size() == 1
                    ? quickCraftSlots.iterator().next()
                    : slotAt(event.x(), event.y());
            if (target >= 0) {
                containerClick(menuSlot(target), button, ContainerInput.PICKUP);
            }
        }
        quickCraftSlots.clear();
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
        var options = minecraft().options;

        // The inventory key closes this screen, exactly like closing the
        // vanilla inventory; so does the mod's own open-board key.
        if (options.keyInventory.matches(event)
                || (Keybinds.openBoard != null && Keybinds.openBoard.matches(event))) {
            onClose();
            return true;
        }

        // Vanilla inventory shortcuts on the hovered slot, honoring the
        // player's actual keybinds: hotbar swap, offhand swap, drop.
        int hovered = slotAt(lastMouseX, lastMouseY);
        if (hovered >= 0) {
            for (int i = 0; i < options.keyHotbarSlots.length && i < 9; i++) {
                if (options.keyHotbarSlots[i].matches(event)) {
                    containerClick(menuSlot(hovered), i, ContainerInput.SWAP);
                    return true;
                }
            }
            if (options.keySwapOffhand.matches(event)) {
                containerClick(menuSlot(hovered), 40, ContainerInput.SWAP);
                return true;
            }
            if (options.keyDrop.matches(event)) {
                containerClick(menuSlot(hovered), hasCtrl() ? 1 : 0, ContainerInput.THROW);
                return true;
            }
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
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (BoardClientState.revision() != seenRevision) {
            syncBoardState(false);
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
        renderHintBar(g);
        renderInventory(g, mouseX, mouseY);

        ItemStack cursor = carried();
        if (!cursor.isEmpty()) {
            // While drag-distributing, show what would remain on the cursor
            // (vanilla behavior); hide it entirely when everything would land.
            ItemStack shown = cursor;
            if (quickCrafting && quickCraftSlots.size() > 1) {
                int remaining = quickCraftRemaining(cursor);
                if (remaining <= 0) return;
                shown = cursor.copyWithCount(remaining);
            }
            g.nextStratum();
            g.item(shown, mouseX - 8, mouseY - 8);
            g.itemDecorations(font, shown, mouseX - 8, mouseY - 8);
        }
    }

    /** Cursor count left over if the current drag-distribute were applied now. */
    private int quickCraftRemaining(ItemStack cursor) {
        var player = minecraft().player;
        if (player == null) return cursor.getCount();
        int remaining = cursor.getCount();
        int perSlot = AbstractContainerMenu.getQuickCraftPlaceCount(
                quickCraftSlots.size(), quickCraftType, cursor);
        for (int invIndex : quickCraftSlots) {
            Slot slot = player.inventoryMenu.getSlot(menuSlot(invIndex));
            int room = Math.min(cursor.getMaxStackSize(), slot.getMaxStackSize(cursor))
                    - slot.getItem().getCount();
            remaining -= Math.min(perSlot, Math.max(0, room));
        }
        return remaining;
    }

    private void renderHeader(GuiGraphicsExtractor g) {
        g.fill(0, 0, width, HEADER_H, 0xE0141418);
        g.text(font, title, 8, (HEADER_H - 9) / 2, 0xFFFFFFFF);

        if (BoardClientState.hasBoard()) {
            Component progress = Component.literal(
                    BoardClientState.submittedCount() + "/" + BoardClientState.totalCells())
                    .withStyle(ChatFormatting.GREEN);
            Component mode = Component.translatable(modeKey(BoardClientState.gameMode()))
                    .withStyle(ChatFormatting.AQUA);
            var right = Component.empty().append(progress).append("  ").append(mode);
            if (BoardClientState.fogSubmitLock()) {
                right.append("  ").append(Component.translatable("itembingo.screen.fog_submit_lock")
                        .withStyle(ChatFormatting.RED));
            }
            g.text(font, right, width - font.width(right) - 8, (HEADER_H - 9) / 2, 0xFFFFFFFF);
        }
    }

    /** The band between board and inventory: controls hint + current zoom. */
    private void renderHintBar(GuiGraphicsExtractor g) {
        int top = boardBottom();
        g.fill(0, top, width, invTop(), 0xE0141418);
        int textY = top + (HINT_BAR_H - 9) / 2 + 1;

        String zoom = (int) Math.round(camera.zoom * 100) + "%";
        g.text(font, zoom, width - font.width(zoom) - 8, textY, 0xFF8899AA);

        Component hint = Component.translatable("itembingo.screen.hint");
        g.text(font, hint, 8, textY, 0x9099AABB);
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
        int hoveredIdx = panning ? -1 : cellIndexAt(mouseX, mouseY);
        ItemStack cursor = carried();
        long now = System.currentTimeMillis();

        for (int row = 0; row < h; row++) {
            int y = (int) Math.round(camera.cellScreenY(row, top));
            if (y + size < top || y > bottom) continue;
            for (int col = 0; col < w; col++) {
                int x = (int) Math.round(camera.cellScreenX(col, 0));
                if (x + size < 0 || x > width) continue;
                int idx = row * w + col;
                CellState cell = BoardClientState.cell(col, row);
                if (cell == null) continue;
                renderCell(g, cell, x, y, (int) Math.round(size),
                        hoveredIdx == idx, cursor, mouseX, mouseY, flashAlpha(idx, now));
            }
        }

        g.disableScissor();
    }

    /** 0..1 glow strength for a freshly-submitted cell, 0 when idle. */
    private float flashAlpha(int idx, long now) {
        Long start = flashes.get(idx);
        if (start == null) return 0;
        long age = now - start;
        if (age >= FLASH_MS) {
            flashes.remove(idx);
            return 0;
        }
        return 1.0f - (float) age / FLASH_MS;
    }

    private void renderCell(GuiGraphicsExtractor g, CellState cell, int x, int y, int size,
                            boolean hovered, ItemStack cursor, int mouseX, int mouseY, float flash) {
        boolean carryingMatch = !cursor.isEmpty() && cell.isVisible()
                && cell.item() != null && cursor.getItem() == cell.item();

        if (cell.kind() == ModProtocol.CELL_HIDDEN) {
            // Fog: a soft slate-blue gradient, clearly different from the flat
            // near-black of ordinary cells — no glyph, just "misted over".
            g.fillGradient(x + 1, y + 1, x + size - 1, y + size - 1, 0xF02A3247, 0xF0161B26);
        } else {
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1,
                    cell.kind() == ModProtocol.CELL_LOCKED ? 0x80581414 : 0x60000000);
        }

        switch (cell.kind()) {
            case ModProtocol.CELL_HIDDEN -> { /* fog is just the tinted cell */ }
            case ModProtocol.CELL_LOCKED -> {
                drawScaledItem(g, new ItemStack(Items.BARRIER), x, y, size);
                if (hovered) {
                    g.setTooltipForNextFrame(Component.translatable("itembingo.cell.locked")
                            .withStyle(ChatFormatting.RED), mouseX, mouseY);
                }
            }
            case ModProtocol.CELL_VISIBLE, ModProtocol.CELL_SUBMITTED -> {
                if (cell.item() != null) {
                    drawScaledItem(g, new ItemStack(cell.item()), x, y, size);
                } else {
                    g.centeredText(font, "?", x + size / 2, y + (size - 9) / 2, 0xFFFFCC44);
                }
                if (cell.isSubmitted()) {
                    // Layered "done" treatment: icon below, translucent green
                    // wash above it, and a big check on top.
                    g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x8A1E7A2E);
                    Glyphs.check(g, x + size / 2.0f, y + size / 2.0f, size * 0.62f,
                            0xFFEAFFEA, 0x900A2F10);
                }
                if (hovered) {
                    g.setTooltipForNextFrame(cellTooltip(cell, cursor), mouseX, mouseY);
                }
            }
            default -> {}
        }

        if (flash > 0) {
            int alpha = (int) (flash * 0xA0) << 24;
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1, alpha | 0xFFFFFF);
        }

        // Border last so highlights sit above the cell content: gold pulse on
        // cells matching the carried item, white on hover.
        int border = hovered ? 0xFFFFFFFF : carryingMatch ? 0xFFE8C84A : 0xFF3C3C46;
        g.outline(x, y, size, size, border);
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

    private List<FormattedCharSequence> cellTooltip(CellState cell, ItemStack cursor) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        Component name = cell.item() != null
                ? cell.item().getName(new ItemStack(cell.item())).copy().withStyle(ChatFormatting.BOLD)
                : Component.literal(String.valueOf(cell.rawItemKey())).withStyle(ChatFormatting.YELLOW);
        lines.add(name.getVisualOrderText());

        if (cell.isSubmitted()) {
            lines.add(Component.literal("✔ ").withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable("itembingo.cell.submitted").withStyle(ChatFormatting.GREEN))
                    .getVisualOrderText());
            if (cell.hasSubmitter() && !cell.submitterName().isEmpty()) {
                lines.add(Component.translatable("itembingo.cell.submitted_by",
                                Component.literal(cell.submitterName()).withStyle(ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.GRAY).getVisualOrderText());
            }
            if (cell.elapsedSeconds() >= 0) {
                lines.add(Component.translatable("itembingo.cell.time",
                                Component.literal(formatElapsed(cell.elapsedSeconds())).withStyle(ChatFormatting.WHITE))
                        .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            }
        } else if (!cursor.isEmpty()) {
            // Carrying something: say immediately whether this cell takes it.
            boolean match = cell.item() != null && cursor.getItem() == cell.item();
            lines.add((match
                    ? Component.translatable("itembingo.cell.click_to_submit").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("itembingo.cell.wrong_item").withStyle(ChatFormatting.RED))
                    .getVisualOrderText());
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

            boolean dragTarget = quickCrafting && quickCraftSlots.size() > 1 && quickCraftSlots.contains(i);
            g.fill(x, y, x + SLOT, y + SLOT,
                    dragTarget ? 0x60FFFFFF : i == hoveredSlot ? 0x50FFFFFF : 0x30000000);
            g.outline(x, y, SLOT, SLOT, 0xFF2A2A32);

            ItemStack stack = playerStack(i);
            if (dragTarget) {
                // Ghost preview of the distributed result, like vanilla.
                ItemStack cursor = carried();
                int perSlot = AbstractContainerMenu.getQuickCraftPlaceCount(
                        quickCraftSlots.size(), quickCraftType, cursor);
                int projected = Math.min(stack.getCount() + perSlot,
                        Math.min(cursor.getMaxStackSize(),
                                minecraft().player.inventoryMenu.getSlot(menuSlot(i)).getMaxStackSize(cursor)));
                ItemStack ghost = cursor.copyWithCount(Math.max(1, projected));
                g.item(ghost, x + 1, y + 1);
                g.itemDecorations(font, ghost, x + 1, y + 1);
            } else if (!stack.isEmpty()) {
                g.item(stack, x + 1, y + 1);
                g.itemDecorations(font, stack, x + 1, y + 1);
            }
        }

        if (hoveredSlot >= 0 && carried().isEmpty()) {
            ItemStack stack = playerStack(hoveredSlot);
            if (!stack.isEmpty()) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                for (Component line : getTooltipFromItem(minecraft(), stack)) {
                    lines.add(line.getVisualOrderText());
                }
                lines.add(Component.translatable("itembingo.screen.submit_hint")
                        .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
                g.setTooltipForNextFrame(lines, mouseX, mouseY);
            }
        }
    }
}
