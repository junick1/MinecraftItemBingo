package me.junick.itembingo.client.screen;

import me.junick.itembingo.client.Keybinds;
import me.junick.itembingo.client.config.ModConfig;
import me.junick.itembingo.client.export.BoardImageExporter;
import me.junick.itembingo.client.net.ClientNetworking;
import me.junick.itembingo.client.net.ModProtocol;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.CellState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
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
import java.util.LinkedHashSet;
import java.util.List;

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

    private final BoardCamera camera = new BoardCamera();
    private boolean panning;

    /** Vanilla quick-craft (drag-distribute) state, mirroring AbstractContainerScreen. */
    private boolean quickCrafting;
    private int quickCraftType; // QUICKCRAFT_TYPE_CHARITABLE (left) or _GREEDY (right)
    private final LinkedHashSet<Integer> quickCraftSlots = new LinkedHashSet<>(); // vanilla inv indexes

    private int lastMouseX;
    private int lastMouseY;

    /** Simple hit-test rectangle for the hand-drawn buttons. */
    private record Rect(int x, int y, int w, int h) {
        static final Rect EMPTY = new Rect(0, 0, 0, 0);

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private boolean exportPopupOpen;
    private Rect btnExport = Rect.EMPTY;
    private Rect btnBingoToggle = Rect.EMPTY;
    private Rect btnOrigCopy = Rect.EMPTY;
    private Rect btnOrigSave = Rect.EMPTY;
    private Rect btnProgCopy = Rect.EMPTY;
    private Rect btnProgSave = Rect.EMPTY;
    private Rect popupPanel = Rect.EMPTY;

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
    public void onClose() {
        BoardImageExporter.cancel();
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

        if (BoardImageExporter.armed()) return true; // capturing: ignore input
        if (BoardImageExporter.showing()) {
            BoardImageExporter.dismiss(); // any click ends the confirmation beat
            return true;
        }

        if (exportPopupOpen) {
            if (left) {
                if (btnOrigCopy.contains(event.x(), event.y())) {
                    BoardImageExporter.begin(BoardImageExporter.Variant.ORIGINAL, BoardImageExporter.Action.COPY);
                } else if (btnOrigSave.contains(event.x(), event.y())) {
                    BoardImageExporter.begin(BoardImageExporter.Variant.ORIGINAL, BoardImageExporter.Action.SAVE);
                } else if (btnProgCopy.contains(event.x(), event.y())) {
                    BoardImageExporter.begin(BoardImageExporter.Variant.PROGRESS, BoardImageExporter.Action.COPY);
                } else if (btnProgSave.contains(event.x(), event.y())) {
                    BoardImageExporter.begin(BoardImageExporter.Variant.PROGRESS, BoardImageExporter.Action.SAVE);
                } else if (!popupPanel.contains(event.x(), event.y())) {
                    exportPopupOpen = false;
                }
            }
            return true; // popup swallows everything
        }

        if (left && btnExport.contains(event.x(), event.y())) {
            exportPopupOpen = true;
            return true;
        }
        if (left && btnBingoToggle.contains(event.x(), event.y())) {
            ModConfig.toggleOverrideBingo();
            return true;
        }

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
        if (exportPopupOpen || BoardImageExporter.armed()) return true;
        if (hasCtrl()) {
            camera.zoomAt(mouseX, mouseY - boardTop(), scrollY);
        } else if (hasShift()) {
            camera.pan((scrollY + scrollX) * BoardCamera.CELL * camera.zoom() / 2.0, 0);
        } else {
            camera.pan(scrollX * BoardCamera.CELL * camera.zoom() / 2.0,
                    scrollY * BoardCamera.CELL * camera.zoom() / 2.0);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        var options = minecraft().options;

        if (BoardImageExporter.showing()) {
            BoardImageExporter.dismiss();
            return true;
        }
        if (exportPopupOpen && event.isEscape()) {
            exportPopupOpen = false;
            return true;
        }

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
        camera.sync(BoardClientState.width(), BoardClientState.height(),
                width, boardBottom() - boardTop());

        if (BoardImageExporter.armed() || BoardImageExporter.showing()) {
            exportPopupOpen = false;
            renderExportFrame(g);
            return;
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
        renderHintBar(g, mouseX, mouseY);
        renderInventory(g, mouseX, mouseY);
        if (exportPopupOpen) {
            renderExportPopup(g, mouseX, mouseY);
        }

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
            Component stage = stageBadge(BoardClientState.gameStage());
            var right = Component.empty().append(progress).append("  ").append(mode)
                    .append("  ").append(stage);
            if (BoardClientState.fogSubmitLock()) {
                right.append("  ").append(Component.translatable("itembingo.screen.fog_submit_lock")
                        .withStyle(ChatFormatting.RED));
            }
            g.text(font, right, width - font.width(right) - 8, (HEADER_H - 9) / 2, 0xFFFFFFFF);
        }
    }

    /** The band between board and inventory: hint, export/settings buttons, zoom. */
    private void renderHintBar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int top = boardBottom();
        g.fill(0, top, width, invTop(), 0xE0141418);
        int textY = top + (HINT_BAR_H - 9) / 2 + 1;

        String zoom = (int) Math.round(camera.zoom() * 100) + "%";
        int zoomX = width - font.width(zoom) - 8;
        g.text(font, zoom, zoomX, textY, 0xFF8899AA);

        Component toggleLabel = Component.translatable(
                ModConfig.overrideBingo() ? "itembingo.settings.bingo_mod" : "itembingo.settings.bingo_server");
        btnBingoToggle = placeButton(g, zoomX - 8, top, toggleLabel, mouseX, mouseY);
        if (btnBingoToggle.contains(mouseX, mouseY)) {
            g.setTooltipForNextFrame(Component.translatable("itembingo.settings.override_tooltip"), mouseX, mouseY);
        }

        Component exportLabel = Component.translatable("itembingo.export.button");
        btnExport = placeButton(g, btnBingoToggle.x() - 6, top, exportLabel, mouseX, mouseY);

        Component hint = Component.translatable("itembingo.screen.hint");
        if (8 + font.width(hint) < btnExport.x() - 8) {
            g.text(font, hint, 8, textY, 0x9099AABB);
        }
    }

    /** Draws a small chip button whose RIGHT edge sits at {@code rightX}. */
    private Rect placeButton(GuiGraphicsExtractor g, int rightX, int barTop, Component label,
                             int mouseX, int mouseY) {
        int w = font.width(label) + 10;
        int h = HINT_BAR_H - 4;
        int x = rightX - w;
        int y = barTop + 2;
        Rect rect = new Rect(x, y, w, h);
        g.fill(x, y, x + w, y + h, rect.contains(mouseX, mouseY) ? 0x50FFFFFF : 0x22FFFFFF);
        g.outline(x, y, w, h, 0xFF3C3C46);
        g.text(font, label, x + 5, y + (h - 9) / 2 + 1, 0xFFDDE2EE);
        return rect;
    }

    private void renderExportPopup(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.nextStratum();
        g.fill(0, 0, width, height, 0x90000000);

        Component title = Component.translatable("itembingo.export.title");
        Component copy = Component.translatable("itembingo.export.copy");
        Component save = Component.translatable("itembingo.export.save");
        Component original = Component.translatable("itembingo.export.original");
        Component progress = Component.translatable(BoardClientState.isTeamMode()
                ? "itembingo.export.progress.team" : "itembingo.export.progress");
        Component status = BoardImageExporter.status();

        int bw = Math.max(font.width(copy), font.width(save)) + 12;
        int panelW = Math.max(240, font.width(title) + 24);
        int panelH = 78 + (status != null ? 16 : 0);
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2 - 20;
        popupPanel = new Rect(px, py, panelW, panelH);

        g.fill(px, py, px + panelW, py + panelH, 0xF8181A20);
        g.outline(px, py, panelW, panelH, 0xFF4A4A56);
        g.text(font, title, px + 10, py + 8, 0xFFFFFFFF);

        btnOrigSave = popupRowButton(g, save, px + panelW - 10, py + 24, bw, mouseX, mouseY);
        btnOrigCopy = popupRowButton(g, copy, btnOrigSave.x() - 4, py + 24, bw, mouseX, mouseY);
        g.text(font, original, px + 10, py + 28, 0xFFB8C0D0);

        btnProgSave = popupRowButton(g, save, px + panelW - 10, py + 46, bw, mouseX, mouseY);
        btnProgCopy = popupRowButton(g, copy, btnProgSave.x() - 4, py + 46, bw, mouseX, mouseY);
        g.text(font, progress, px + 10, py + 50, 0xFFB8C0D0);

        if (status != null) {
            g.text(font, status, px + 10, py + panelH - 14, 0xFFFFFFFF);
        }
    }

    private Rect popupRowButton(GuiGraphicsExtractor g, Component label, int rightX, int y, int w,
                                int mouseX, int mouseY) {
        int h = 16;
        int x = rightX - w;
        Rect rect = new Rect(x, y, w, h);
        g.fill(x, y, x + w, y + h, rect.contains(mouseX, mouseY) ? 0x60FFFFFF : 0x28FFFFFF);
        g.outline(x, y, w, h, 0xFF4A4A56);
        g.text(font, label, x + (w - font.width(label)) / 2, y + 4, 0xFFE8ECF4);
        return rect;
    }

    /** One clean frame for the framebuffer capture: the "results card". */
    private void renderExportFrame(GuiGraphicsExtractor g) {
        g.fill(0, 0, width, height, 0xFF0E0F12);

        CellState[] cells = BoardImageExporter.cells();
        int w = BoardImageExporter.boardWidth();
        int h = BoardImageExporter.boardHeight();
        if (cells == null || w <= 0 || h <= 0) return;

        List<BoardImageExporter.Contribution> contribs = BoardImageExporter.contributions();
        int shownRows = Math.min(contribs.size(), 8);
        boolean truncated = contribs.size() > shownRows;

        int pad = 8;
        int headerH = 24;
        int contribH = shownRows > 0 ? 18 + shownRows * 22 + (truncated ? 12 : 0) : 0;
        int cell = Math.clamp(Math.min((width - 2 * pad - 16) / w,
                (height - 2 * pad - 16 - headerH - contribH) / h), 6, 48);
        int gridW = cell * w;
        int panelW = Math.max(gridW, shownRows > 0 ? 230 : 170);
        int totalH = headerH + cell * h + contribH;
        int x0 = (width - panelW) / 2;
        int y0 = (height - totalH) / 2;

        g.fill(x0 - pad, y0 - pad, x0 + panelW + pad, y0 + totalH + pad, 0xFF14161C);
        g.outline(x0 - pad, y0 - pad, panelW + 2 * pad, totalH + 2 * pad, 0xFF2A303E);

        // Header: title at 1.4x, chips right-aligned.
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x0, y0 + 3);
        pose.scale(1.4f, 1.4f);
        g.text(font, BoardImageExporter.title(), 0, 0, 0xFFFFFFFF);
        pose.popMatrix();

        int chipRight = x0 + panelW;
        List<Component> chipList = BoardImageExporter.chips();
        for (int i = chipList.size() - 1; i >= 0; i--) {
            Component chip = chipList.get(i);
            int cw = font.width(chip) + 8;
            int cx = chipRight - cw;
            g.fill(cx, y0 + 2, cx + cw, y0 + 16, 0xFF1E2430);
            g.outline(cx, y0 + 2, cw, 14, 0xFF394152);
            g.text(font, chip, cx + 4, y0 + 5, 0xFFB8C4DA);
            chipRight = cx - 4;
        }

        // Board grid, centered when the contribution list is wider.
        int gx = x0 + (panelW - gridW) / 2;
        int gy = y0 + headerH;
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                drawExportCell(g, cells[row * w + col], gx + col * cell, gy + row * cell, cell);
            }
        }

        if (shownRows > 0) {
            renderContributions(g, contribs, shownRows, truncated, x0, gy + cell * h + 4, panelW);
        }

        BoardImageExporter.onExportFrame(x0 - pad, y0 - pad, panelW + 2 * pad, totalH + 2 * pad);

        // Post-capture confirmation beat: the card holds with a result badge
        // (kept OUTSIDE the reported crop rect so it never bakes into the PNG).
        if (BoardImageExporter.showing()) {
            Component badge = BoardImageExporter.resultBadge();
            if (badge != null) {
                int bw = font.width(badge) + 16;
                int bx = (width - bw) / 2;
                int by = y0 + totalH + pad + 8;
                g.fill(bx, by, bx + bw, by + 16, 0xF0181A20);
                g.outline(bx, by, bw, 16, 0xFF4A5266);
                g.text(font, badge, bx + 8, by + 4, 0xFFFFFFFF);
            }
        }
    }

    /** Ranked teammate list: avatar, name, bar, cell count; gold #1 row. */
    private void renderContributions(GuiGraphicsExtractor g, List<BoardImageExporter.Contribution> contribs,
                                     int shownRows, boolean truncated, int x0, int y, int panelW) {
        g.text(font, Component.translatable("itembingo.export.contribution"), x0, y + 3, 0xFFDDE2EE);

        int maxCount = Math.max(1, contribs.getFirst().count());
        int nameColW = 40;
        for (int i = 0; i < shownRows; i++) {
            nameColW = Math.max(nameColW, Math.min(104, font.width(contribs.get(i).name()) + 6));
        }
        int rowsTop = y + 15;
        for (int i = 0; i < shownRows; i++) {
            BoardImageExporter.Contribution entry = contribs.get(i);
            int ry = rowsTop + i * 22;
            boolean mvp = i == 0;

            g.fill(x0, ry, x0 + panelW, ry + 20, mvp ? 0x30E8C84A : 0x14FFFFFF);
            if (mvp) {
                g.outline(x0, ry, panelW, 20, 0xFFE8C84A);
            }

            g.text(font, String.valueOf(i + 1), x0 + 6, ry + 6, mvp ? 0xFFE8C84A : 0xFF8A93A6);

            drawPlayerFace(g, entry.name(), x0 + 18, ry + 1, 18);

            int nameX = x0 + 42;
            g.text(font, entry.name(), nameX, ry + 6, mvp ? 0xFFF6E6A8 : 0xFFE8ECF4);

            String count = String.valueOf(entry.count());
            int countW = font.width(count);
            g.text(font, count, x0 + panelW - countW - 6, ry + 6, 0xFFFFFFFF);

            int barX = nameX + nameColW;
            int barW = x0 + panelW - countW - 14 - barX;
            if (barW > 24) {
                int barY = ry + 8;
                g.fill(barX, barY, barX + barW, barY + 4, 0xFF262B36);
                int fill = Math.max(2, (int) ((long) barW * entry.count() / maxCount));
                g.fill(barX, barY, barX + fill, barY + 4, mvp ? 0xFFE8C84A : 0xFF4C7DD8);
            }
        }

        if (truncated) {
            g.text(font, Component.translatable("itembingo.export.more", contribs.size() - shownRows),
                    x0 + 4, rowsTop + shownRows * 22 + 2, 0xFF8A93A6);
        }
    }

    /** Skin face for online teammates; tinted initial tile for offline ones. */
    private void drawPlayerFace(GuiGraphicsExtractor g, String name, int x, int y, int size) {
        var connection = minecraft().getConnection();
        var info = connection != null ? connection.getPlayerInfo(name) : null;
        if (info != null) {
            PlayerFaceExtractor.extractRenderState(g, info.getSkin(), x, y, size);
            return;
        }
        int[] palette = {0xFF534AB7, 0xFF0F6E56, 0xFF993C1D, 0xFF993556, 0xFF185FA5, 0xFF854F0B};
        g.fill(x, y, x + size, y + size, palette[Math.floorMod(name.hashCode(), palette.length)]);
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        g.centeredText(font, initial, x + size / 2, y + (size - 9) / 2 + 1, 0xFFFFFFFF);
    }

    private void drawExportCell(GuiGraphicsExtractor g, CellState cell, int x, int y, int size) {
        if (cell == null) return;
        if (cell.kind() == ModProtocol.CELL_HIDDEN) {
            g.fillGradient(x + 1, y + 1, x + size - 1, y + size - 1, 0xF02A3247, 0xF0161B26);
        } else {
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1,
                    cell.kind() == ModProtocol.CELL_LOCKED ? 0x80581414 : 0x60000000);
        }
        switch (cell.kind()) {
            case ModProtocol.CELL_LOCKED -> drawScaledItem(g, CellState.BARRIER_STACK, x, y, size);
            case ModProtocol.CELL_VISIBLE, ModProtocol.CELL_SUBMITTED -> {
                if (cell.stack() != null) {
                    drawScaledItem(g, cell.stack(), x, y, size);
                } else {
                    g.centeredText(font, "?", x + size / 2, y + (size - 9) / 2, 0xFFFFCC44);
                }
                if (cell.isSubmitted()) {
                    g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x8A1E7A2E);
                    Glyphs.check(g, x + size / 2.0f, y + size / 2.0f, size * 0.62f,
                            0xFFEAFFEA, 0x900A2F10);
                }
            }
            default -> {}
        }
        g.outline(x, y, size, size, 0xFF3C3C46);
    }

    private static String modeKey(byte mode) {
        return switch (mode) {
            case ModProtocol.MODE_SWAPPAGE -> "itembingo.screen.mode.swappage";
            case ModProtocol.MODE_FOG_OF_WAR -> "itembingo.screen.mode.fog_of_war";
            case ModProtocol.MODE_LOCKOUT -> "itembingo.screen.mode.lockout";
            default -> "itembingo.screen.mode.normal";
        };
    }

    /** Localized, color-coded game stage (gray / green / gold). */
    public static Component stageBadge(byte stage) {
        return switch (stage) {
            case ModProtocol.STAGE_RUNNING -> Component.translatable("itembingo.screen.stage.in_progress")
                    .withStyle(ChatFormatting.GREEN);
            case ModProtocol.STAGE_ENDED -> Component.translatable("itembingo.screen.stage.finished")
                    .withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("itembingo.screen.stage.not_started")
                    .withStyle(ChatFormatting.GRAY);
        };
    }

    private void renderBoard(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int top = boardTop();
        int bottom = boardBottom();
        g.enableScissor(0, top, width, bottom);

        int w = BoardClientState.width();
        int hoveredIdx = panning ? -1 : cellIndexAt(mouseX, mouseY);
        ItemStack cursor = carried();
        long now = System.currentTimeMillis();

        // Matching cells highlight for the carried item — or, hands free, for
        // the inventory stack under the mouse ("does the board want this?").
        ItemStack reference = cursor;
        if (reference.isEmpty()) {
            int hoveredSlot = slotAt(mouseX, mouseY);
            if (hoveredSlot >= 0) reference = playerStack(hoveredSlot);
        }

        // Only the cells actually inside the viewport get touched. Each cell is
        // tiled to its neighbor's ROUNDED edge — rounding position and size
        // independently leaves 1px gaps at fractional zooms.
        int firstRow = camera.firstVisibleRow();
        int lastRow = camera.lastVisibleRow();
        int firstCol = camera.firstVisibleCol();
        int lastCol = camera.lastVisibleCol();

        for (int row = firstRow; row <= lastRow; row++) {
            int y = (int) Math.round(camera.cellScreenY(row, top));
            int y2 = (int) Math.round(camera.cellScreenY(row + 1, top));
            for (int col = firstCol; col <= lastCol; col++) {
                int x = (int) Math.round(camera.cellScreenX(col, 0));
                int x2 = (int) Math.round(camera.cellScreenX(col + 1, 0));
                int idx = row * w + col;
                CellState cell = BoardClientState.cell(col, row);
                if (cell == null) continue;
                renderCell(g, cell, x, y, x2 - x, y2 - y, hoveredIdx == idx,
                        cursor, reference, mouseX, mouseY,
                        BoardClientState.flashAlpha(idx, now), BoardClientState.rejectAlpha(idx, now));
            }
        }

        g.disableScissor();
    }

    private void renderCell(GuiGraphicsExtractor g, CellState cell, int x, int y, int cw, int ch,
                            boolean hovered, ItemStack cursor, ItemStack reference,
                            int mouseX, int mouseY, float flash, float reject) {
        int size = Math.min(cw, ch);
        boolean referenceMatch = !reference.isEmpty() && cell.isVisible()
                && cell.item() != null && reference.getItem() == cell.item();

        int ix = x + (cw - size) / 2;
        int iy = y + (ch - size) / 2;

        if (cell.kind() == ModProtocol.CELL_HIDDEN) {
            // Fog: a soft slate-blue gradient, clearly different from the flat
            // near-black of ordinary cells — no glyph, just "misted over".
            g.fillGradient(x + 1, y + 1, x + cw - 1, y + ch - 1, 0xF02A3247, 0xF0161B26);
        } else {
            g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1,
                    cell.kind() == ModProtocol.CELL_LOCKED ? 0x80581414 : 0x60000000);
        }

        switch (cell.kind()) {
            case ModProtocol.CELL_HIDDEN -> { /* fog is just the tinted cell */ }
            case ModProtocol.CELL_LOCKED -> {
                drawScaledItem(g, CellState.BARRIER_STACK, ix, iy, size);
                if (hovered) {
                    g.setTooltipForNextFrame(Component.translatable("itembingo.cell.locked")
                            .withStyle(ChatFormatting.RED), mouseX, mouseY);
                }
            }
            case ModProtocol.CELL_VISIBLE, ModProtocol.CELL_SUBMITTED -> {
                if (cell.stack() != null) {
                    drawScaledItem(g, cell.stack(), ix, iy, size);
                } else {
                    g.centeredText(font, "?", x + cw / 2, y + (ch - 9) / 2, 0xFFFFCC44);
                }
                if (cell.isSubmitted()) {
                    // Layered "done" treatment: icon below, translucent green
                    // wash above it, and a big check on top.
                    g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, 0x8A1E7A2E);
                    Glyphs.check(g, x + cw / 2.0f, y + ch / 2.0f, size * 0.62f,
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
            g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, alpha | 0xFFFFFF);
        }
        if (reject > 0) {
            int alpha = (int) (reject * 0xA8) << 24;
            g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, alpha | 0xE03030);
        }

        // Border last so highlights sit above the cell content: gold on cells
        // matching the carried or hovered item, white on hover.
        int border = hovered ? 0xFFFFFFFF : referenceMatch ? 0xFFE8C84A : 0xFF3C3C46;
        g.outline(x, y, cw, ch, border);
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

                g.setTooltipForNextFrame(lines, mouseX, mouseY);
            }
        }
    }
}
