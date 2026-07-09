package com.example.themedgui.client.automining;

import com.example.themedgui.client.ThemedGuiModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Auto-mining functionality for private server testing only.
 * WARNING: Do not use on public servers like Hypixel.
 */
public class AutoMiner {

    private static final Minecraft mc = Minecraft.getInstance();
    private long lastMineTime = 0;
    private BlockPos currentTarget = null;

    public void tick() {
        if (!ThemedGuiModClient.CONFIG.autoMiningEnabled) {
            return;
        }

        if (mc.player == null || mc.level == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long delay = ThemedGuiModClient.CONFIG.miningDelay;

        if (currentTime - lastMineTime < delay) {
            return;
        }

        Player player = mc.player;
        Level level = mc.level;

        // Find target block in front of player
        BlockPos target = findTargetBlock(player, level);
        if (target == null) {
            currentTarget = null;
            return;
        }

        currentTarget = target;

        // Auto switch tool if enabled
        if (ThemedGuiModClient.CONFIG.autoSwitchTool) {
            switchToBestTool(player, level.getBlockState(target));
        }

        // Break the block
        breakBlock(player, level, target);
        lastMineTime = currentTime;
    }

    private BlockPos findTargetBlock(Player player, Level level) {
        int range = ThemedGuiModClient.CONFIG.miningRange;

        // Get player's facing direction
        Direction facing = player.getDirection();
        BlockPos playerPos = player.blockPosition();

        // Check blocks in front of player
        for (int i = 1; i <= range; i++) {
            BlockPos target = playerPos.relative(facing, i);
            BlockState state = level.getBlockState(target);

            // Only mine breakable blocks (not air or unbreakable)
            if (!state.isAir() && state.getDestroySpeed(level, target) >= 0) {
                return target;
            }
        }

        return null;
    }

    private void switchToBestTool(Player player, BlockState targetState) {
        float bestSpeed = 0;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                float speed = stack.getDestroySpeed(targetState);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot >= 0) {
            mc.options.keyHotbarSlots[bestSlot].setDown(true);
            mc.options.keyHotbarSlots[bestSlot].setDown(false);
        }
    }

    private void breakBlock(Player player, Level level, BlockPos pos) {
        if (mc.gameMode == null) {
            return;
        }

        // Simulate left-click to break the block
        mc.options.keyAttack.setDown(true);
        mc.gameMode.destroyBlock(pos);
        mc.options.keyAttack.setDown(false);
    }

    public BlockPos getCurrentTarget() {
        return currentTarget;
    }

    public boolean isEnabled() {
        return ThemedGuiModClient.CONFIG.autoMiningEnabled;
    }
}