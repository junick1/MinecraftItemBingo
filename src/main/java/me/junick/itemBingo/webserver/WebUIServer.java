package me.junick.itemBingo.webserver;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.websocket.WsContext;
import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.FogOfWar;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.ProgressFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WebUIServer {

    // 연결된 유저들의 웹소켓 세션을 저장하는 안전한 Map (Key: UUID / Value: 소켓 세션)
    private static final Map<UUID, WsContext> sessions = new ConcurrentHashMap<>();
    private Javalin app;
    private static Gson gson = new Gson();

    public void startServer(int port) {
        // Javalin 웹 서버 시작
        app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(CorsPluginConfig.CorsRule::anyHost);
            });
        }).start(port);

        // 웹소켓 엔드포인트 설정 (ws://localhost:port/ui)
        app.ws("/player", ws -> {

            // 1. 브라우저가 소켓에 연결되었을 때
            ws.onConnect(ctx -> {
                // 브라우저가 보낸 쿼리 스트링에서 uuid 추출 (예: ws://.../ui?uuid=PlayerUUID)
                String nick = ctx.queryParam("nick");
                if (nick == null) nick = "1";
                UUID uuid = Bukkit.getOfflinePlayer(nick).getUniqueId();
                sessions.put(uuid, ctx);
                Player p = Bukkit.getPlayer(uuid);
                System.out.println("[WebUI] 유저 연결 성공: " + uuid);
                if (p != null) {
                    sendBingoBoard(p);
                }
            });

            // 2. 브라우저가 연결을 끊었을 때
            ws.onClose(ctx -> {
                String nick = ctx.queryParam("nick");
                if (nick == null) nick = "1";
                UUID uuid = Bukkit.getOfflinePlayer(nick).getUniqueId();
                sessions.remove(uuid);
                System.out.println("[WebUI] 유저 연결 종료: " + uuid);
            });

            // 3. 에러가 발생했을 때
            ws.onError(ctx -> {
                String nick = ctx.queryParam("nick");
                if (nick == null) nick = "1";
                UUID uuid = Bukkit.getOfflinePlayer(nick).getUniqueId();
                sessions.remove(uuid);
            });
        });

        app.get("/bingoboard", ctx -> {
            // 브라우저에게 JSON 형식으로 응답 던져주기!
            if (ItemBingo.currentBingo != null) {
                ctx.contentType("application/json");
                String nick = ctx.queryParam("nick");
                if (nick == null) nick = "";
                UUID uuid = Bukkit.getOfflinePlayer(nick).getUniqueId();
                Player p = Bukkit.getPlayer(uuid);
                BingoBoardModeling bb = getBingoBoardViewFromPlayer(p);
                ctx.result(gson.toJson(bb));
            } else {
                ctx.status(400).result("{\"text\":\"빙고판이 비어 있다?\"}");
            }
        });
    }

    public void stopServer() {
        if (app != null) {
            app.stop();
        }
    }

    public static void sendPlayerProgressUpdate(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(ItemBingo.getInstance(), () -> {
            if (p == null) return;
            var prog = ProgressFactory.of(p).getSubmittedSlots();
            // Set<Integer> slot = prog.getSubmittedSlots();
            if (prog != null) {
                sendTargetedUpdate(p.getUniqueId(), gson.toJson(prog));
            }
        });
    }

    public static void sendBingoBoard(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(ItemBingo.getInstance(), () -> {
            var prog = getBingoBoardViewFromPlayer(p);
            sendTargetedUpdate(p.getUniqueId(), gson.toJson(prog));
        });
    }

    private static BingoBoardModeling getBingoBoardViewFromPlayer(Player p) {
        Set<Integer> prog = (p != null) ? ProgressFactory.of(p).getSubmittedSlots() : Set.of();
        BingoBoardModeling bb = new BingoBoardModeling(ItemBingo.currentBingo.getWidth(),
                ItemBingo.currentBingo.getHeight(), ItemBingo.currentBingo.getItems(), prog);
        if (Settings.isFogOfWarMode()) {
            var reveal = FogOfWar.revealedSlots(bb.width, bb.height, prog, Settings.isFogDiagonalReveal());
            for (int i = 0; i < bb.width * bb.height; i++) {
                if (!reveal.contains(i)) {
                    bb.items.set(i, new ItemStack(Material.BARRIER).serialize());
                }
            }
        }
        return bb;
    }

    /**
     * 마인크래프트 게임 서버에서 데이터가 바뀌면 이 메서드를 호출합니다!
     * ⚠️ 주의: 반드시 Bukkit 비동기 스케줄러 내부에서 호출해 주세요!
     */
    public static void sendTargetedUpdate(UUID uuid, String message) {
        WsContext ctx = sessions.get(uuid);

        // 유저가 웹페이지를 켜놓고 있고, 소켓이 열려있다면 실시간 데이터 전송!
        if (ctx != null && ctx.session.isOpen()) {
            ctx.send(message);
        }
    }
}

class BingoBoardModeling {
    public int width;
    public int height;
    public List<Map<String, Object>> items;
    public Set<Integer> submittedSlots;

    public BingoBoardModeling(int width, int height, List<ItemStack> items, Set<Integer> submittedSlots) {
        this.width = width;
        this.height = height;
        var serializedItems = items.stream().map(ItemStack::serialize);
        this.items = new ArrayList<>(serializedItems.toList());
        this.submittedSlots = submittedSlots;
    }
}
