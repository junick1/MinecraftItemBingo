package me.junick.itemBingo.network;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.FogOfWar;
import me.junick.itemBingo.util.Lockout;
import me.junick.itemBingo.util.ProgressFactory;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * Serializes the board as ONE viewer is allowed to see it, mirroring the cell
 * priority of {@code BingoGUI.cellIcon}: submitted → lockout-locked →
 * fog-hidden → visible. Fog-hidden cells carry no item data at all — a modded
 * client must not learn hidden cells from packet inspection.
 */
public final class BoardViewBuilder {
    private BoardViewBuilder() {}

    public static byte[] build(Player viewer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            BingoBoard board = ItemBingo.currentBingo;
            if (board == null) {
                out.writeByte(ModProtocol.STATUS_NO_BOARD);
                return bytes.toByteArray();
            }
            if (!BingoGUI.canView(viewer)) {
                out.writeByte(ModProtocol.STATUS_VIEW_DENIED);
                return bytes.toByteArray();
            }

            BingoProgressAccess progress = ProgressFactory.of(viewer);
            int w = board.getWidth();
            int h = board.getHeight();

            boolean fog = Settings.isFogOfWarMode();
            Set<Integer> revealed = fog
                    ? FogOfWar.revealedSlots(w, h, progress.getSubmittedSlots(), Settings.isFogDiagonalReveal())
                    : null;
            Set<Integer> locked = Lockout.lockedSlots(viewer); // empty outside Lockout mode

            int flags = 0;
            if (fog && Settings.isFogSubmitLock()) flags |= ModProtocol.FLAG_FOG_SUBMIT_LOCK;

            out.writeByte(ModProtocol.STATUS_OK);
            out.writeByte(Settings.getGameMode().ordinal());
            out.writeByte(me.junick.itemBingo.util.TimerManager.getStage().ordinal());
            out.writeByte(flags);
            out.writeShort(w);
            out.writeShort(h);
            out.writeShort(progress.getSubmittedSlots().size());
            out.writeShort(board.getItems().size());

            for (int idx = 0; idx < w * h; idx++) {
                if (progress.isSubmitted(idx)) {
                    out.writeByte(ModProtocol.CELL_SUBMITTED);
                    out.writeUTF(board.getItems().get(idx).getType().getKey().toString());
                    String submitter = progress.getSubmitterName(idx);
                    boolean hasSubmitter = progress.getSubmitterId(idx) != null;
                    out.writeBoolean(hasSubmitter);
                    if (hasSubmitter) {
                        out.writeUTF(submitter != null ? submitter : "");
                    }
                    out.writeLong(progress.getSubmissionTime(idx));
                } else if (locked.contains(idx)) {
                    out.writeByte(ModProtocol.CELL_LOCKED);
                } else if (fog && !revealed.contains(idx)) {
                    out.writeByte(ModProtocol.CELL_HIDDEN);
                } else {
                    out.writeByte(ModProtocol.CELL_VISIBLE);
                    out.writeUTF(board.getItems().get(idx).getType().getKey().toString());
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            // ByteArrayOutputStream never throws; required by DataOutputStream's API.
            throw new UncheckedIOException(e);
        }
    }
}
