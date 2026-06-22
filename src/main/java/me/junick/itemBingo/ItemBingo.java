package me.junick.itemBingo;

import me.junick.itemBingo.admin.AdminClickListener;
import me.junick.itemBingo.admin.AdminCommand;
import me.junick.itemBingo.commands.*;
import me.junick.itemBingo.config.BundleManager;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.gui.BingoViewport;
import me.junick.itemBingo.events.ChatListener;
import me.junick.itemBingo.events.EffectListener;
import me.junick.itemBingo.events.LavaMovement;
import me.junick.itemBingo.events.LungeMovement;
import me.junick.itemBingo.events.gui.*;
import me.junick.itemBingo.events.items.*;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.*;
import org.bukkit.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemBingo extends JavaPlugin {
    private static ItemBingo instance;
    private BingoTagLoader tagLoader;
    public TeamManager teamManager;

    public static ItemBingo getInstance() {
        return instance;
    }
    public BingoTagLoader getTagLoader() { return tagLoader; }
    public TeamManager getTeamManager() { return teamManager; }

    public static BingoBoard currentBingo = null;
    public static NamespacedKey KEY_EFFECT;
    public static NamespacedKey KEY_BUNDLE_TEMPLATE;

    @Override
    public void onEnable() {
        instance = this;

        // Extract the default bundle templates on first run (does not overwrite admin edits).
        saveResource("bundle.yml", false);

        tagLoader = new BingoTagLoader(this);

        teamManager = new TeamManager(this);

        ChatManager.load();

        KEY_EFFECT = new NamespacedKey(this, "effect");
        KEY_BUNDLE_TEMPLATE = new NamespacedKey(this, "bundle_template");

        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(new AdminClickListener(this), this);

        Bukkit.getPluginManager().registerEvents(new MenuClickEvent(), this);
        Bukkit.getPluginManager().registerEvents(new BingoClickEvent(), this);
        Bukkit.getPluginManager().registerEvents(new ShopClickEvent(), this);
        Bukkit.getPluginManager().registerEvents(new EffectShopClickEvent(), this);
        Bukkit.getPluginManager().registerEvents(new ItemShopClickEvent(), this);
        Bukkit.getPluginManager().registerEvents(new DiamondExchangeClickEvent(), this);

        Bukkit.getPluginManager().registerEvents(new DyeSelectorEvent(), this);
        Bukkit.getPluginManager().registerEvents(new CopperOxidizerEvent(), this);
        Bukkit.getPluginManager().registerEvents(new ExplorerMapEvent(), this);
        Bukkit.getPluginManager().registerEvents(new BiomeMapEvent(), this);
        Bukkit.getPluginManager().registerEvents(new LavaMovement(), this);
        Bukkit.getPluginManager().registerEvents(new LungeMovement(), this);

        Bukkit.getPluginManager().registerEvents(new DiamondEvent(), this);
        Bukkit.getPluginManager().registerEvents(new ShovelOxidizeEvent(), this);

        Bukkit.getPluginManager().registerEvents(new EffectListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new BundleClickEvent(), this);
        Bukkit.getPluginManager().registerEvents(new PresetClickEvent(), this);
        Bukkit.getPluginManager().registerEvents(new PresetEditorClickEvent(), this);
        EffectApplier.start();

        // /admin
        getCommand("admin").setExecutor(new AdminCommand(this));

        getCommand("menu").setExecutor(new MenuCommand());

        TeamsCommand tc = new TeamsCommand();
        getCommand("teams").setExecutor(tc);
        getCommand("teams").setTabCompleter(tc);

        getCommand("teammate").setExecutor(new TeammateCommand());

        ChatCommand chat = new ChatCommand();
        getCommand("chat").setExecutor(chat);
        getCommand("chat").setTabCompleter(chat);
        getCommand("ac").setExecutor(new AllChatCommand());
        getCommand("tc").setExecutor(new TeamChatCommand());

        getCommand("bingo").setExecutor(new ViewBingo());
        getCommand("rank").setExecutor(new RankCommand());
        getCommand("shop").setExecutor(new ShopCommand());

        RollBingo rb = new RollBingo();
        getCommand("rollbingo").setExecutor(rb);
        getCommand("rollbingo").setTabCompleter(rb);
        getCommand("timer").setExecutor(new TimerCommand());

        NewBingoCommand nb = new NewBingoCommand();
        getCommand("newbingo").setExecutor(nb);
        getCommand("newbingo").setTabCompleter(nb);

        SetBingoCommand sb = new SetBingoCommand();
        getCommand("setbingo").setExecutor(sb);
        getCommand("setbingo").setTabCompleter(sb);

        EditBingoCommand eb = new EditBingoCommand();
        getCommand("editbingo").setExecutor(eb);
        getCommand("editbingo").setTabCompleter(eb);

        getCommand("startbingo").setExecutor(new StartBingoCommand());
        getCommand("pointadd").setExecutor((CommandExecutor)new PointCommand());
        getCommand("customitem").setExecutor((CommandExecutor)new CustomItemCommand());
        getCommand("setbundle").setExecutor((CommandExecutor)new SetBundleCommand());
        getCommand("bundle").setExecutor((CommandExecutor)new BundleCommand());

//        MaterialExporter.exportToFile();

        new BingoScoreboardUpdater().runTaskTimer(this, 0L, 20L);

        BingoBoard loaded = BingoStorage.load();
        if (loaded != null) {
            currentBingo = loaded;
            getLogger().info("Loaded existing bingo board from file.");
        }

        TimerManager.loadState();

        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRules.KEEP_INVENTORY, true);
            world.setGameRule(GameRules.PVP, false);
        }

        Settings.load(this);

        getLogger().info("ItemBingo plugin has been enabled!");

    }

    /**
     * Installs {@code board} as the active game: wipes all per-player/team progress and the
     * once-per-game bundle list, persists the board, and refreshes any open board/shop GUIs.
     * Shared by {@code /rollbingo} (fresh roll) and {@code /setbingo} (saved preset).
     */
    public static void applyNewBoard(BingoBoard board) {
        BundleManager.resetPlayerList();
        PlayerDataManager.resetAll();
        TeamDataManager.resetAll();

        currentBingo = board;
        BingoStorage.save(board);

        // The old scroll positions don't map onto the new board, so forget them;
        // each player re-centers on their next open.
        BingoViewport.clearAll();

        // Reset wiped every board + currency, so refresh anyone looking at the old
        // board or a shop (their balances just dropped to zero).
        GuiSync.refreshAllGameViews();
    }

    @Override
    public void onDisable() {
        instance = null;

        if (currentBingo != null) {
            BingoStorage.save(currentBingo);
            getLogger().info("Saved current bingo board to file.");
        }

        TimerManager.saveState();
        Settings.save(this);
        ChatManager.save();
    }
}
