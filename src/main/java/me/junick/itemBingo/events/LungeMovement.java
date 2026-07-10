package me.junick.itemBingo.events;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import me.junick.itemBingo.ItemBingo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class LungeMovement implements Listener {

    @EventHandler
    public void onLungeAttack(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 1. 좌클릭(공기 중 또는 블록)인지 확인해요!
        if (!event.getAction().isLeftClick()) {
            return;
        }

        // 2. 아이템이 우리가 설정한 그 "창"인지 확인합니다 (이름이나 PDC 사용)
        if (item == null || !item.getType().name().toLowerCase().contains("spear")) return;
        // 아리스의 추천: 이름으로 판별하기 (PDC를 쓰면 더 좋지만 일단 이름으로!)
        if (!item.getItemMeta().getDisplayName().contains("특정한 창 이름")) return;

        // 3. ⭐ 핵심: 플레이어의 공격 쿨타임이 꽉 찼는지 확인합니다!
        // getCooledAttackStrength는 0.0 ~ 1.0 사이의 값을 반환해요. 1.0이 완충 상태입니다.
        if (player.getCooledAttackStrength(0.0f) > 0.9f) {
            // 아직 쿨타임 중이라면 발동시키지 않아요!
            return;
        }

        var init_velocity = player.getLocation().getDirection().normalize().multiply(1.0);
        var velocity = player.getLocation().getDirection().normalize().multiply(0.1);
        //velocity.add(new Vector(0, 0.42, 0));
        var initial = player.getVelocity();
        player.setVelocity(initial.add(init_velocity));

        new BukkitRunnable() {
            int timer = 0;
            final int duration = 10; // 10틱 동안 지속 (0.5초)
            @Override
            public void run() {
                // 3. 타이머가 다 되면 마법을 종료해요!
                if (timer >= duration || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // 4. 매 틱마다 플레이어에게 속도를 부여합니다.
                // 0.5f 정도면 공기 저항을 이기고 부드럽게 나아가는 느낌이 나요!
               // player.setVelocity(direction.clone().multiply(0.8));
                player.setVelocity(player.getVelocity().add(velocity));

                // 5. (선택) 돌진 중에 멋진 파티클 발동!
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 1, 0.1, 0.1, 0.1, 0.05);

                timer++;
            }
        }.runTaskTimer(ItemBingo.getInstance(), 0L, 1L);

        // 4. 돌진 로직 실행! (모험가님이 만드신 코드)


        // 5. (선택 사항) 돌진 후 공격 쿨타임을 강제로 초기화하고 싶다면?
        player.resetCooldown();
    }

    private static final Map<UUID, Long> lungeCooldowns = new HashMap<>();

    @EventHandler
    public void onLungeAttack2(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 1. 좌클릭(공기 중 또는 블록)인지 확인해요!
        if (!event.getAction().isLeftClick()) {
            return;
        }

        // 2. 아이템이 우리가 설정한 그 "창"인지 확인합니다 (이름이나 PDC 사용)
        if (item == null || !item.getType().name().toLowerCase().contains("spear")) return;

        // 2. 아이템이 우리가 설정한 그 "창"인지 확인합니다 (이름이나 PDC 사용)
        //if (item == null || !item.getType().name().toLowerCase().contains("spear")) return;
        // 아리스의 추천: 이름으로 판별하기 (PDC를 쓰면 더 좋지만 일단 이름으로!)
        //if (!item.getItemMeta().getDisplayName().contains("특정한 창 이름")) return;
        if (item.getType() != Material.COPPER_SPEAR) return;

        // 3. ⭐ 핵심: 플레이어의 공격 쿨타임이 꽉 찼는지 확인합니다!
        // getCooledAttackStrength는 0.0 ~ 1.0 사이의 값을 반환해요. 1.0이 완충 상태입니다.

        long currentTime = System.currentTimeMillis();
        if (lungeCooldowns.containsKey(player.getUniqueId())) {
            long expireTime = lungeCooldowns.get(player.getUniqueId());

            if (currentTime < expireTime) {
                double timeLeft = (double) (expireTime - currentTime) / 1000; // 남은 초 계산
                player.sendMessage("§c[쿨타임 중 (" + timeLeft + "초)");
                return;
            }
        }

        var init_velocity = player.getLocation().getDirection().normalize().multiply(1.0);
        var velocity = player.getLocation().getDirection().normalize().multiply(0.11);
        //velocity.add(new Vector(0, 0.42, 0));
        var initial = player.getVelocity();
        player.setVelocity(initial.add(init_velocity));

        new BukkitRunnable() {
            int timer = 0;
            final int duration = 13; // 10틱 동안 지속 (0.5초)
            @Override
            public void run() {
                // 3. 타이머가 다 되면 마법을 종료해요!
                if (timer >= duration || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // 4. 매 틱마다 플레이어에게 속도를 부여합니다.
                // 0.5f 정도면 공기 저항을 이기고 부드럽게 나아가는 느낌이 나요!
                // player.setVelocity(direction.clone().multiply(0.8));
                player.setVelocity(player.getVelocity().add(velocity));

                // 5. (선택) 돌진 중에 멋진 파티클 발동!
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 3, 0.1, 0.1, 0.1, 0.05);

                timer++;
            }
        }.runTaskTimer(ItemBingo.getInstance(), 0L, 1L);

        lungeCooldowns.put(player.getUniqueId(), currentTime + 5000);

        // 4. 돌진 로직 실행! (모험가님이 만드신 코드)


        // 5. (선택 사항) 돌진 후 공격 쿨타임을 강제로 초기화하고 싶다면?
        player.resetCooldown();
    }

}
