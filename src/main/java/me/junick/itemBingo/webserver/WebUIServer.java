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
import me.junick.itemBingo.util.PasswordManager;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.ProgressFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jetty.util.security.Password;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WebUIServer {

    // 플레이어당 여러 세션 지원 (Key: UUID / Value: 세션 ID -> WsContext)
    private static final Map<UUID, Map<String, WsContext>> playerSessions = new ConcurrentHashMap<>();
    // 세션 ID -> UUID 매핑 (종료 시 빠른 조회용)
    private static final Map<String, UUID> sessionToUuid = new ConcurrentHashMap<>();
    // 핑 스케줄 태스크 ID (서버 정지 시 취소용)
    private int pingTaskId = -1;
    private Javalin app;
    private static Gson gson = new Gson();
    private static boolean logEnabled = false;

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
                try {
                    // 브라우저가 보낸 쿼리 스트링에서 uuid 추출
                    String nick = ctx.queryParam("nick");
                    if (nick == null || nick.isEmpty()) {
                        ctx.send(gson.toJson(new ErrorResponse(400, "닉네임이 없습니다")));
                        ctx.session.close();
                        return;
                    }

                    String pass = ctx.queryParam("password");
                    UUID uuid = Bukkit.getOfflinePlayer(nick).getUniqueId();
                    String userPass = PasswordManager.getPassword(uuid);

                    // 비밀번호 검증
                    if (pass == null) pass = "";
                    if (!(pass.isEmpty() && userPass == null) && !pass.equals(userPass)) {
                        ctx.send(gson.toJson(new ErrorResponse(401, "비밀번호가 맞지 않습니다")));
                        ctx.session.close();
                        return;
                    }

                    // 세션 저장 (여러 세션 지원)
                    String sessionId = UUID.randomUUID().toString();
                    playerSessions.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(sessionId, ctx);
                    sessionToUuid.put(sessionId, uuid);

                    log("[WebUI] 유저 연결 성공: " + uuid + " (세션: " + sessionId + ")");

                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        sendBingoBoard(p);
                    }
                } catch (Exception e) {
                    err("[WebUI] 연결 중 오류: " + e.getMessage());
                    try {
                        ctx.send(gson.toJson(new ErrorResponse(500, "연결 오류: " + e.getMessage())));
                    } catch (Exception ignored) {}
                    try {
                        ctx.session.close();
                    } catch (Exception ignored) {}
                }
            });

            // 2. 브라우저가 연결을 끊었을 때
            ws.onClose(ctx -> {
                try {
                    String nick = ctx.queryParam("nick");
                    if (nick == null || nick.isEmpty()) return;

                    UUID uuid = Bukkit.getOfflinePlayer(nick).getUniqueId();
                    Map<String, WsContext> userSessions = playerSessions.get(uuid);

                    // 현재 ctx와 일치하는 세션 찾아서 제거
                    if (userSessions != null) {
                        String sessionIdToRemove = null;
                        for (Map.Entry<String, WsContext> entry : userSessions.entrySet()) {
                            if (entry.getValue() == ctx) {
                                sessionIdToRemove = entry.getKey();
                                break;
                            }
                        }

                        if (sessionIdToRemove != null) {
                            userSessions.remove(sessionIdToRemove);
                            sessionToUuid.remove(sessionIdToRemove);
                            log("[WebUI] 유저 연결 종료: " + uuid + " (세션: " + sessionIdToRemove + ")");

                            // 모든 세션이 종료된 경우에만 플레이어 정보 제거
                            if (userSessions.isEmpty()) {
                                playerSessions.remove(uuid);
                                log("[WebUI] 플레이어 " + uuid + "의 모든 세션 종료");
                            }
                        }
                    }
                } catch (Exception e) {
                    err("[WebUI] 연결 종료 중 오류: " + e.getMessage());
                }
            });

            // 3. 에러가 발생했을 때
            ws.onError((ctx) -> {
                try {
                    String nick = ctx.queryParam("nick");
                    if (nick == null || nick.isEmpty()) {
                        err("[WebUI] 웹소켓 에러 (닉네임 없음): ");
                        return;
                    }

                    UUID uuid = Bukkit.getOfflinePlayer(nick).getUniqueId();
                    Map<String, WsContext> userSessions = playerSessions.get(uuid);

                    // 에러 발생한 세션 찾아서 제거
                    if (userSessions != null) {
                        String sessionIdToRemove = null;
                        for (Map.Entry<String, WsContext> entry : userSessions.entrySet()) {
                            if (entry.getValue() == ctx) {
                                sessionIdToRemove = entry.getKey();
                                break;
                            }
                        }

                        if (sessionIdToRemove != null) {
                            userSessions.remove(sessionIdToRemove);
                            sessionToUuid.remove(sessionIdToRemove);
                            err("[WebUI] 유저 연결 에러 및 종료: " + uuid + " (세션: " + sessionIdToRemove + ")");

                            if (userSessions.isEmpty()) {
                                playerSessions.remove(uuid);
                            }
                        }
                    }
                } catch (Exception e) {
                    err("[WebUI] 에러 처리 중 문제: " + e.getMessage());
                }
            });

            // 4. 클라이언트로부터 메시지 수신 (핑에 대한 응답)
            ws.onMessage(ctx -> {
                // 클라이언트의 응답(pong)을 받으면 간단히 로그 출력만 함
                // 연결이 살아있다는 증거이므로 추가 처리는 불필요
            });
        });

        // 10초마다 모든 클라이언트에 핑 전송 (keepalive)
        startPingTask();
    }

    public void stopServer() {
        // 핑 태스크 취소
        if (pingTaskId != -1) {
            Bukkit.getScheduler().cancelTask(pingTaskId);
            log("[WebUI] 핑 태스크 취소됨");
        }

        if (app != null) {
            app.stop();
            log("[WebUI] 서버 종료됨");
        }
    }

    /**
     * 30초마다 모든 클라이언트에 핑 메시지를 전송하여 연결 유지 (keepalive)
     */
    private void startPingTask() {
        pingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(ItemBingo.getInstance(), () -> {
            String pingMessage = gson.toJson(new PingMessage());
            playerSessions.forEach((uuid, sessions) -> {
                sessions.forEach((sessionId, ctx) -> {
                    try {
                        if (ctx != null && ctx.session.isOpen()) {
                            ctx.send(pingMessage);
                        }
                    } catch (Exception e) {
                        err("[WebUI] 핑 전송 실패 (플레이어: " + uuid + ", 세션: " + sessionId + "): " + e.getMessage());
                    }
                });
            });
        }, 0L, 200L); // 10초
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
     * 해당 플레이어의 모든 브라우저 세션에 메시지를 전송합니다.
     */
    public static void sendTargetedUpdate(UUID uuid, String message) {
        Map<String, WsContext> userSessions = playerSessions.get(uuid);

        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }

        // 모든 세션에 메시지 전송 (닫힌 세션은 자동으로 제거)
        userSessions.entrySet().removeIf(entry -> {
            WsContext ctx = entry.getValue();
            try {
                if (ctx != null && ctx.session.isOpen()) {
                    ctx.send(message);
                    return false; // 유지
                }
                return true; // 제거
            } catch (Exception e) {
                err("[WebUI] 메시지 전송 실패 (세션: " + entry.getKey() + "): " + e.getMessage());
                return true; // 제거
            }
        });

        // 모든 세션이 종료되면 플레이어 정보 제거
        if (userSessions.isEmpty()) {
            playerSessions.remove(uuid);
        }
    }

    private static void log(Object message) {
        if (logEnabled) {
            System.out.println(message);
        }
    }

    private static void err(Object message) {
        if (logEnabled) {
            System.err.println(message);
        }
    }
}

class BingoBoardModeling {
    public String type = "bingo";
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

/**
 * 에러 응답 모델 (HTTP 상태 코드와 메시지)
 */
class ErrorResponse {
    public int code;
    public String message;

    public ErrorResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

/**
 * 핑/퐁 메시지 모델 (WebSocket 연결 유지용)
 */
class PingMessage {
    public String type = "ping";
    public long timestamp;

    PingMessage() {
        timestamp = System.currentTimeMillis();
    }
}
