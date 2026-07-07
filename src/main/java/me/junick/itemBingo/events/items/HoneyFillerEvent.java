package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class HoneyFillerEvent
implements Listener {
    @EventHandler
    public void onUseHoneyFiller(PlayerInteractEvent e) {
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
        if (!CustomItems.is(hand, BingoItem.HONEY_FILLER)) { // check honey filler item
            return;
        }
        Block block = e.getClickedBlock();
        e.setCancelled(true);
        if (block.getType() == Material.BEEHIVE || block.getType() == Material.BEE_NEST) {
            // 1. 블록의 상태 데이터를 가져옵니다.
            BlockData blockData = block.getBlockData();

            // 2. BlockData를 Beehive 타입으로 캐스팅(변환)합니다.
            if (blockData instanceof org.bukkit.block.data.type.Beehive beehive) {
                // 3. 꿀 수치를 최대치(5)로 설정합니다!
                beehive.setHoneyLevel(beehive.getMaximumHoneyLevel());

                // 4. 변경된 데이터를 진짜 세계(블록)에 적용합니다!
                block.setBlockData(beehive);

                // (선택 미션) 아이템을 1개 소모하고 싶다면 이 코드를 사용하세요!
                hand.setAmount(hand.getAmount() - 1);

                // 성공 효과음과 파티클을 뿜뿜하면 더 멋져요!
                player.playSound(block.getLocation(), Sound.BLOCK_BEEHIVE_DRIP,2.0f, 1.0f);
                //player.sendMessage(Messages.get(player, "items.honey_filler.used"));
            }
        } else {
            //player.sendMessage(Messages.get(player, "items.honey-filler.invalid-block"));
        }
    }
}

