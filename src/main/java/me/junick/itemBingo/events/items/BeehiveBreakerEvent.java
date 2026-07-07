package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.CustomItems;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BeehiveBreakerEvent implements Listener {
    @EventHandler
    public void onUseBeehiveBreakerRight(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!e.getAction().isRightClick()) {
            return;
        }
        if (e.getClickedBlock() == null) {
            return;
        }
        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, BingoItem.BEEHIVE_BREAKER)) {
            return;
        }

        Block block = e.getClickedBlock();
        Material current = block.getType();

        // 1. 클릭한 블록이 벌집 또는 벌통인지 확인합니다!
        if (current == Material.BEEHIVE || current == Material.BEE_NEST) {
            e.setCancelled(true); // 벌집일 때만 이벤트를 취소해요!

            Location loc = block.getLocation();
            World world = block.getWorld();

            // 2. 벌집 안에 실제로 들어있는 벌들의 수를 파악하기 위해 BlockState를 가져옵니다.
            // (주의: 이번엔 block.data가 아니라 org.bukkit.block.Beehive 예요!)
            int beeCount = 0; // 기본값으로 0마리를 소환하도록 설정!
            if (block.getState() instanceof org.bukkit.block.Beehive) {
                org.bukkit.block.Beehive beehiveState = (org.bukkit.block.Beehive) block.getState();
                // 만약 벌집 안에 진짜 벌들이 살고 있다면, 그 마리 수만큼 소환하게 응용할 수도 있어요!
                beeCount = beehiveState.getEntityCount();
            }

            // 3. 벌집을 공기(Air)로 만들어서 파괴합니다! 콰쾅!
            block.setType(Material.AIR);

            // 4. 밀랍(Honeycomb) 3개를 드롭합니다!
            world.dropItemNaturally(loc, new ItemStack(Material.HONEYCOMB, 3));

            // 5. 화난 벌들을 소환해서 플레이어를 타겟으로 지정합니다! 레이드 시작이에요!
            for (int i = 0; i < beeCount; i++) {
                // 벌집 위치보다 약간 위(+0.5)에서 스폰되도록 조절하면 예뻐요
                Bee bee = (Bee) world.spawnEntity(loc.add(0, 0.5, 0), EntityType.BEE);

                bee.setAnger(600); // 벌이 화나는 시간 설정 (600틱 = 30초)
                bee.setTarget(player); // 맹공격 대상을 용사님으로 지정!
            }

            // 파괴 효과음과 이펙트를 주면 타격감이 배가 돼요!
            player.playSound(loc, Sound.BLOCK_BEEHIVE_EXIT, 1.0f, 0.5f);

            // (선택 미션) 브레이커 아이템 소모하기
            hand.setAmount(hand.getAmount() - 1);
        }
    }

    @EventHandler
    public void onUseBeehiveBreakerLeft(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!e.getAction().isLeftClick()) {
            return;
        }
        if (e.getClickedBlock() == null) {
            return;
        }
        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, BingoItem.BEEHIVE_BREAKER)) {
            return;
        }

        Block block = e.getClickedBlock();
        Material current = block.getType();

        // 1. 클릭한 블록이 벌집 또는 벌통인지 확인합니다!
        if (current == Material.BEEHIVE || current == Material.BEE_NEST) {
            e.setCancelled(true); // 벌집일 때만 이벤트를 취소해요!

            Location loc = block.getLocation();
            World world = block.getWorld();

            // 2. 벌집 안에 실제로 들어있는 벌들의 수를 파악하기 위해 BlockState를 가져옵니다.
            // (주의: 이번엔 block.data가 아니라 org.bukkit.block.Beehive 예요!)
            int beeCount = 0; // 기본값으로 0마리를 소환하도록 설정!
            if (block.getState() instanceof org.bukkit.block.Beehive) {
                org.bukkit.block.Beehive beehiveState = (org.bukkit.block.Beehive) block.getState();
                // 만약 벌집 안에 진짜 벌들이 살고 있다면, 그 마리 수만큼 소환하게 응용할 수도 있어요!
                beeCount = beehiveState.getEntityCount();
            }

            // 3. 벌집을 공기(Air)로 만들어서 파괴합니다! 콰쾅!
            block.setType(Material.AIR);

            // 4. 밀랍(Honeycomb) 3개를 드롭합니다!
            world.dropItemNaturally(loc, new ItemStack(Material.HONEY_BOTTLE, 1));

            // 5. 화난 벌들을 소환해서 플레이어를 타겟으로 지정합니다! 레이드 시작이에요!
            for (int i = 0; i < beeCount; i++) {
                // 벌집 위치보다 약간 위(+0.5)에서 스폰되도록 조절하면 예뻐요
                Bee bee = (Bee) world.spawnEntity(loc.add(0, 0.5, 0), EntityType.BEE);

                bee.setAnger(600); // 벌이 화나는 시간 설정 (600틱 = 30초)
                bee.setTarget(player); // 맹공격 대상을 용사님으로 지정!
            }

            // 파괴 효과음과 이펙트를 주면 타격감이 배가 돼요!
            player.playSound(loc, Sound.BLOCK_BEEHIVE_EXIT, 1.0f, 0.5f);
            //player.sendMessage(Messages.get(player, "items.beehive-breaker.used"));
            // (선택 미션) 브레이커 아이템 소모하기
            hand.setAmount(hand.getAmount() - 1);
        } else {
            //player.sendMessage(Messages.get(player, "items.beehive-breaker.invalid-block"));
        }
    }
}
