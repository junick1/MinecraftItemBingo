package me.junick.itemBingo;

import me.junick.itemBingo.admin.AdminClickListener;
import me.junick.itemBingo.admin.AdminCommand;
import me.junick.itemBingo.commands.*;
import me.junick.itemBingo.events.EffectListener;
import me.junick.itemBingo.events.gui.*;
import me.junick.itemBingo.events.items.*;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.*;
import org.bukkit.*;
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

    @Override
    public void onEnable() {
        instance = this;
        tagLoader = new BingoTagLoader(this);

        teamManager = new TeamManager(this);
        teamManager.setTeamCount(2);

        KEY_EFFECT = new NamespacedKey(this, "effect");

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

        Bukkit.getPluginManager().registerEvents(new DiamondEvent(), this);
        Bukkit.getPluginManager().registerEvents(new ShovelOxidizeEvent(), this);

        Bukkit.getPluginManager().registerEvents(new EffectListener(), this);
        EffectApplier.start();

        // /admin
        getCommand("admin").setExecutor(new AdminCommand(this));

        getCommand("menu").setExecutor(new MenuCommand());

        TeamsCommand tc = new TeamsCommand();
        getCommand("teams").setExecutor(tc);
        getCommand("teams").setTabCompleter(tc);

        getCommand("teammate").setExecutor(new TeammateCommand());

        getCommand("bingo").setExecutor(new ViewBingo());
        getCommand("rank").setExecutor(new RankCommand());
        getCommand("shop").setExecutor(new ShopCommand());

        getCommand("rollbingo").setExecutor(new RollBingo());
        getCommand("timer").setExecutor(new TimerCommand());

        getCommand("startbingo").setExecutor(new StartBingoCommand());

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
        }

        getLogger().info("ItemBingo plugin has been enabled!");

    }

    @Override
    public void onDisable() {
        instance = null;

        if (currentBingo != null) {
            BingoStorage.save(currentBingo);
            getLogger().info("Saved current bingo board to file.");
        }

        TimerManager.saveState();
    }
}
