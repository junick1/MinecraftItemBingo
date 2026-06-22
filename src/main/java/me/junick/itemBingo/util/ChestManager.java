package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages the team/private storage chest.
 * <p>
 * The chest behaves like a real chest: every member who opens it shares the
 * <em>same</em> {@link Inventory} instance, so item moves propagate live between
 * viewers (Bukkit handles the sync for free once they share one inventory).
 * <p>
 * Keying:
 * <ul>
 *   <li>Team mode ON  → one shared chest per team ({@code team-<id>}).</li>
 *   <li>Team mode OFF → one private chest per player ({@code solo-<uuid>}).</li>
 * </ul>
 * Both keyspaces persist independently in {@code chests.yml}, so toggling team
 * mode swaps which chest is shown without destroying the other.
 * <p>
 * Each key has a 54-slot backing array (the full 6 rows). The live inventory
 * only exposes the configured number of rows; items in rows beyond the current
 * capacity are preserved in the backing array and reappear if capacity grows.
 */
public final class ChestManager implements Listener {
    private static final int MAX_SLOTS = 54;

    /** Currently-open shared inventories, keyed as described above. */
    private static final Map<String, Inventory> live = new HashMap<>();
    /** Full 54-slot contents per key (source of truth for persistence + overflow). */
    private static final Map<String, ItemStack[]> backing = new HashMap<>();

    private static ItemBingo plugin;
    private static File file;

    private ChestManager() {}

    /** Factory for the event listener instance (state is static; this is just the handler). */
    public static ChestManager listener() {
        return new ChestManager();
    }

    /** Custom holder so chest inventories can be identified in events by their key. */
    public static final class ChestHolder implements InventoryHolder {
        private final String key;
        private Inventory inventory;

        ChestHolder(String key) { this.key = key; }

        public String getKey() { return key; }
        void setInventory(Inventory inventory) { this.inventory = inventory; }

        @Override
        public Inventory getInventory() { return inventory; }
    }

    /* ========================= Lifecycle ========================= */

    public static void init(ItemBingo pl) {
        plugin = pl;
        file = new File(pl.getDataFolder(), "chests.yml");
        backing.clear();
        live.clear();

        if (!file.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("chests");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            List<?> list = cfg.getList("chests." + key);
            ItemStack[] arr = new ItemStack[MAX_SLOTS];
            if (list != null) {
                for (int i = 0; i < list.size() && i < MAX_SLOTS; i++) {
                    if (list.get(i) instanceof ItemStack is) arr[i] = is;
                }
            }
            backing.put(key, arr);
        }
    }

    public static void save() {
        if (file == null) return;

        // Pull any open inventories back into their backing arrays first.
        for (Map.Entry<String, Inventory> e : live.entrySet()) {
            syncToBacking(e.getKey(), e.getValue());
        }

        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, ItemStack[]> e : backing.entrySet()) {
            ItemStack[] arr = e.getValue();
            if (isEmpty(arr)) continue;
            cfg.set("chests." + e.getKey(), Arrays.asList(arr));
        }

        try {
            cfg.save(file);
        } catch (IOException ex) {
            Bukkit.getLogger().severe("[ItemBingo] Failed to save chests.yml: " + ex.getMessage());
        }
    }

    /** Wipes every chest (live + persisted). Called when a new board is rolled. */
    public static void resetAll() {
        for (Inventory inv : new ArrayList<>(live.values())) {
            for (HumanEntity v : new ArrayList<>(inv.getViewers())) {
                v.closeInventory();
            }
        }
        live.clear();
        backing.clear();
        if (file != null && file.exists() && !file.delete()) {
            Bukkit.getLogger().warning("[ItemBingo] Failed to delete chests.yml");
        }
    }

    /**
     * Closes every open chest (saving contents first) so they rebuild at the new
     * size/key/title on the next open. Called when chest capacity or team mode
     * changes — both of which invalidate the currently-open live inventories.
     */
    public static void invalidateOpenChests() {
        for (Map.Entry<String, Inventory> e : new HashMap<>(live).entrySet()) {
            syncToBacking(e.getKey(), e.getValue());
            for (HumanEntity v : new ArrayList<>(e.getValue().getViewers())) {
                v.closeInventory();
            }
        }
        live.clear();
        save();
    }

    /* ========================= Opening ========================= */

    /** Opens the caller's storage chest, or tells them why it isn't available. */
    public static void open(Player p) {
        if (!Settings.isChestEnabled()) {
            p.sendMessage(Component.text("창고가 비활성화되어 있습니다.", NamedTextColor.RED));
            return;
        }

        String key = keyFor(p);
        if (key == null) {
            p.sendMessage(Component.text("팀에 속해 있지 않아 공유 창고를 열 수 없습니다.", NamedTextColor.RED));
            return;
        }

        Inventory inv = live.computeIfAbsent(key, ChestManager::createLive);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
    }

    private static String keyFor(Player p) {
        if (Settings.isTeamEnabled()) {
            int t = ItemBingo.getInstance().getTeamManager().getTeamId(p);
            if (t == TeamManager.NO_TEAM) return null;
            return "team-" + t;
        }
        return "solo-" + p.getUniqueId();
    }

    private static String title() {
        return Settings.isTeamEnabled() ? "§5공유 창고" : "§5개인 창고";
    }

    private static Inventory createLive(String key) {
        int size = Settings.getChestSlots();
        ChestHolder holder = new ChestHolder(key);
        Inventory inv = Bukkit.createInventory(holder, size, title());
        holder.setInventory(inv);

        ItemStack[] back = backing.computeIfAbsent(key, k -> new ItemStack[MAX_SLOTS]);
        for (int i = 0; i < size && i < MAX_SLOTS; i++) {
            inv.setItem(i, back[i]);
        }
        return inv;
    }

    /* ========================= Events ========================= */

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof ChestHolder holder)) return;

        Inventory inv = e.getInventory();
        syncToBacking(holder.getKey(), inv);

        // The closing player is still counted as a viewer during this event, so
        // check on the next tick whether the chest is now empty of viewers.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (inv.getViewers().isEmpty()) {
                live.remove(holder.getKey());
            }
            save();
        });
    }

    /* ========================= Helpers ========================= */

    /** Copies the live inventory's visible slots into its backing array (overflow rows untouched). */
    private static void syncToBacking(String key, Inventory inv) {
        ItemStack[] back = backing.computeIfAbsent(key, k -> new ItemStack[MAX_SLOTS]);
        int size = Math.min(inv.getSize(), MAX_SLOTS);
        for (int i = 0; i < size; i++) {
            back[i] = inv.getItem(i);
        }
    }

    private static boolean isEmpty(ItemStack[] arr) {
        for (ItemStack it : arr) {
            if (it != null && !it.getType().isAir()) return false;
        }
        return true;
    }
}
