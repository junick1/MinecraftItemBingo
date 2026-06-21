package me.junick.itemBingo.util;

import me.junick.itemBingo.config.Settings;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class TeamManager {

    public static final int NO_TEAM = -1;

    private final Plugin plugin;

    private final Map<UUID, Integer> assignments = new HashMap<>();

    private int teamCount = 2;

    private boolean useScoreboardTeams = true;
    private final String scoreboardTeamPrefix = "gm_team_";

    private File dataFile;

    public TeamManager(Plugin plugin) {
        this.plugin = plugin;

        dataFile = new File(plugin.getDataFolder(), "teams.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        load();
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();

        yml.set("teamCount", teamCount);

        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<UUID, Integer> e : assignments.entrySet()) {
            map.put(e.getKey().toString(), e.getValue());
        }

        yml.createSection("assignments", map);

        try {
            yml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save teams.yml");
            e.printStackTrace();
        }
    }

    public void load() {
        if (!dataFile.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(dataFile);

        teamCount = Math.max(1, yml.getInt("teamCount", 2));

        assignments.clear();
        if(yml.contains("assignments")) {
            for (String key : yml.getConfigurationSection("assignments").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int teamId = yml.getInt("assignments." + key);

                    if (teamId >= 0 && teamId < teamCount) {
                        assignments.put(uuid, teamId);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        ensureScoreboardTeams();
        syncAllOnlineToScoreboard();
    }

    public int getTeamCount() {
        return teamCount;
    }

    public void setTeamCount(int teamCount) {
        if (teamCount < 1) {
            throw new IllegalArgumentException("teamCount must be >= 1");
        }

        this.teamCount = teamCount;
        assignments.entrySet().removeIf(e -> e.getValue() < 0 || e.getValue() >= teamCount);

        ensureScoreboardTeams();
        syncAllOnlineToScoreboard();

        save();
    }

    public boolean isUseScoreboardTeams() {
        return useScoreboardTeams;
    }

    public void setUseScoreboardTeams(boolean useScoreboardTeams) {
        this.useScoreboardTeams = useScoreboardTeams;
        ensureScoreboardTeams();
        syncAllOnlineToScoreboard();
    }

    /*
     * Query helpers
     */

    public int getTeamId(UUID uuid) {
        return assignments.getOrDefault(uuid, NO_TEAM);
    }

    public int getTeamId(Player player) {
        return getTeamId(player.getUniqueId());
    }

    /**
     * The team a player effectively plays as, honouring the global team-mode
     * toggle. When team mode is OFF, assignments are ignored entirely and every
     * player is treated as solo ({@link #NO_TEAM}); when ON, this is just their
     * real assignment. Gameplay code (progress, feedback, ranking) should use
     * this rather than {@link #getTeamId(Player)} so the admin toggle is the
     * single source of truth.
     */
    public int effectiveTeamId(Player player) {
        if (!Settings.isTeamEnabled()) return NO_TEAM;
        return getTeamId(player);
    }

    /**
     * Online players who are not on any team, excluding OPs (who may run/spectate
     * games without playing). Only meaningful when team mode is ON.
     */
    public List<Player> getUnassignedOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.isOp())
                .filter(p -> getTeamId(p) == NO_TEAM)
                .collect(Collectors.toList());
    }

    public boolean hasTeam(UUID uuid) {
        return getTeamId(uuid) != NO_TEAM;
    }

    public boolean hasTeam(Player player) {
        return hasTeam(player.getUniqueId());
    }

    public Set<UUID> getPlayersOnTeam(int teamId) {
        if (teamId < 0 || teamId >= teamCount) {
            return Collections.emptySet();
        }

        return assignments.entrySet().stream()
                .filter(e -> e.getValue() == teamId)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<Player> getOnlinePlayersOnTeam(int teamId) {
        Set<UUID> ids = getPlayersOnTeam(teamId);
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> ids.contains(p.getUniqueId()))
                .collect(Collectors.toList());
    }

    public List<Player> getOnlineTeammates(Player player) {
        int t = getTeamId(player);
        if (t == NO_TEAM) {
            return List.of();
        }

        return getOnlinePlayersOnTeam(t).stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .collect(Collectors.toList());
    }

    public boolean sameTeam(Player a, Player b) {
        int ta = getTeamId(a);
        int tb = getTeamId(b);
        return ta != NO_TEAM && ta == tb;
    }

    public Map<Integer, Integer> getTeamSizes() {
        Map<Integer, Integer> sizes = new HashMap<>();
        for (int i = 0; i < teamCount; i++) {
            sizes.put(i, 0);
        }
        for (int t : assignments.values()) {
            if (t >= 0 && t < teamCount) {
                sizes.put(t, sizes.get(t) + 1);
            }
        }
        return sizes;
    }

    /*
     * Assignment operations
     */

    public void clearAll() {
        assignments.clear();
        if (useScoreboardTeams) {
            Scoreboard sb = getMainScoreboard();
            for (int i = 0; i < teamCount; i++) {
                Team team = sb.getTeam(scoreboardTeamName(i));
                if (team != null) {
                    for (String entry : new ArrayList<>(team.getEntries())) {
                        team.removeEntry(entry);
                    }
                }
            }
        }
        save();
    }

    public void unassign(UUID uuid) {
        assignments.remove(uuid);
        if (useScoreboardTeams) {
            removeFromAllScoreboardTeams(uuid);
        }
        save();
    }

    public void unassign(Player player) {
        unassign(player.getUniqueId());
    }

    public void assign(UUID uuid, int teamId) {
        if (teamId < 0 || teamId >= teamCount) {
            throw new IllegalArgumentException("teamId must be in range 0.." + (teamCount - 1));
        }

        assignments.put(uuid, teamId);
        if (useScoreboardTeams) {
            putOnScoreboardTeam(uuid, teamId);
        }
        save();
    }

    public void assign(Player player, int teamId) {
        assign(player.getUniqueId(), teamId);
    }

    /**
     *
     * @param players players to consider
     * @param keepExisting if true, players who already have a team remain on it
     */
    public void assignRandomEven(Collection<? extends OfflinePlayer> players, boolean keepExisting) {
        // Current sizes
        int[] sizes = new int[teamCount];
        for (int t : assignments.values()) {
            if (t >= 0 && t < teamCount) {
                sizes[t]++;
            }
        }

        // Build list to assign
        List<OfflinePlayer> toAssign = new ArrayList<>();
        for (OfflinePlayer p : players) {
            if (p == null) {
                continue;
            }

            UUID id = p.getUniqueId();
            if (keepExisting && assignments.containsKey(id)) {
                continue;
            }

            toAssign.add(p);
        }

        // Shuffle so it's fair
        Collections.shuffle(toAssign, ThreadLocalRandom.current());

        for (OfflinePlayer p : toAssign) {
            int bestTeam = pickSmallestTeam(sizes);
            assign(p.getUniqueId(), bestTeam);
            sizes[bestTeam]++;
        }
    }

    private int pickSmallestTeam(int[] sizes) {
        int min = Integer.MAX_VALUE;
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] < min) {
                min = sizes[i];
                candidates.clear();
                candidates.add(i);
            } else if (sizes[i] == min) {
                candidates.add(i);
            }
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    /*
     * Scoreboard helpers
     */

    private Scoreboard getMainScoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private String scoreboardTeamName(int teamId) {
        return scoreboardTeamPrefix + teamId;
    }

    private void ensureScoreboardTeams() {
        if (!useScoreboardTeams) {
            return;
        }

        Scoreboard sb = getMainScoreboard();
        for (int i = 0; i < teamCount; i++) {
            String name = scoreboardTeamName(i);
            Team team = sb.getTeam(name);
            if (team == null) {
                team = sb.registerNewTeam(name);
            }

            team.displayName(Component.text("Team " + (i + 1)));
        }
    }

    private void removeFromAllScoreboardTeams(UUID uuid) {
        Scoreboard sb = getMainScoreboard();
        String entry = entryName(uuid);

        for (Team t : sb.getTeams()) {
            if (t.getName().startsWith(scoreboardTeamPrefix)) {
                if (t.hasEntry(entry)) {
                    t.removeEntry(entry);
                }
            }
        }
    }

    private void putOnScoreboardTeam(UUID uuid, int teamId) {
        Scoreboard sb = getMainScoreboard();
        ensureScoreboardTeams();
        removeFromAllScoreboardTeams(uuid);

        Team team = sb.getTeam(scoreboardTeamName(teamId));
        if (team != null) {
            team.addEntry(entryName(uuid));
        }
    }

    private void syncAllOnlineToScoreboard() {
        if (!useScoreboardTeams) {
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            int t = getTeamId(p);
            if (t == NO_TEAM) {
                removeFromAllScoreboardTeams(p.getUniqueId());
            } else {
                putOnScoreboardTeam(p.getUniqueId(), t);
            }
        }
    }

    private String entryName(UUID uuid) {
        return uuid.toString();
    }
}