package com.example.themedgui.client.automining;

import com.example.themedgui.client.ThemedGuiModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Advanced Route Mining with AOTV teleportation and macro detection evasion.
 * WARNING: For private server testing only.
 */
public class RouteMiner {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Random random = new Random();

    private MiningRoute currentRoute;
    private RouteMinerState state = RouteMinerState.IDLE;
    private long stateStartTime = 0;
    private BlockPos currentTargetBlock = null;
    private int breakAttempts = 0;
    private final int maxBreakAttempts = 30; // 30 ticks = 1.5 seconds

    // Macro detection evasion: randomize timing and movements
    private long baseDelay = 200; // base delay in ms
    private long randomDelayOffset = 0;

    private enum RouteMinerState {
        IDLE,
        MOVING_TO_POINT,
        AIMING_AT_BLOCK,
        BREAKING_BLOCK,
        WAITING_FOR_RESPAWN,
        COMPLETED
    }

    public void tick() {
        if (!ThemedGuiModClient.CONFIG.autoMiningEnabled || currentRoute == null) {
            state = RouteMinerState.IDLE;
            return;
        }

        if (mc.player == null || mc.level == null) {
            state = RouteMinerState.IDLE;
            return;
        }

        switch (state) {
            case IDLE:
                startNextPoint();
                break;
            case MOVING_TO_POINT:
                handleMovement();
                break;
            case AIMING_AT_BLOCK:
                handleAiming();
                break;
            case BREAKING_BLOCK:
                handleBreaking();
                break;
            case WAITING_FOR_RESPAWN:
                handleRespawnWait();
                break;
            case COMPLETED:
                if (currentRoute.isLooping()) {
                    currentRoute.reset();
                    state = RouteMinerState.IDLE;
                }
                break;
        }
    }

    private void startNextPoint() {
        MiningRoute.RoutePoint point = currentRoute.getCurrentPoint();
        if (point == null) {
            state = RouteMinerState.COMPLETED;
            return;
        }

        if (point.useAOTV) {
            executeAOTV(point);
        } else {
            executeNormalMovement(point);
        }

        state = RouteMinerState.MOVING_TO_POINT;
        stateStartTime = System.currentTimeMillis();
        addRandomDelay();
    }

    private void executeAOTV(MiningRoute.RoutePoint point) {
        // AOTV: Right-click to teleport to target position
        // First aim at the target block
        aimAtPosition(point.x, point.y, point.z);

        // Simulate right-click with AOTV
        mc.options.keyUse.setDown(true);
        mc.options.keyUse.setDown(false);
    }

    private void executeNormalMovement(MiningRoute.RoutePoint point) {
        // Normal movement using beachball-like functionality
        // This would need to be integrated with existing movement system
        aimAtPosition(point.x, point.y, point.z);

        // Move towards target (simplified - actual implementation would use pathfinding)
        LocalPlayer player = mc.player;
        if (player != null) {
            // Set movement towards target using options keys
            double dx = point.x - player.getX();
            double dz = point.z - player.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance > 0.5) {
                if (dx < 0) mc.options.keyLeft.setDown(true);
                if (dx > 0) mc.options.keyRight.setDown(true);
                if (dz < 0) mc.options.keyUp.setDown(true);
                if (dz > 0) mc.options.keyDown.setDown(true);
            }
        }
    }

    private void handleMovement() {
        MiningRoute.RoutePoint point = currentRoute.getCurrentPoint();
        if (point == null) {
            state = RouteMinerState.COMPLETED;
            return;
        }

        // Check if we've reached the target position
        LocalPlayer player = mc.player;
        if (player != null) {
            double distance = Math.sqrt(
                    Math.pow(point.x - player.getX(), 2) +
                            Math.pow(point.y - player.getY(), 2) +
                            Math.pow(point.z - player.getZ(), 2)
            );

            if (distance < 2.0) { // Close enough
                state = RouteMinerState.AIMING_AT_BLOCK;
                stateStartTime = System.currentTimeMillis();
                addRandomDelay();
            }
        }

        // Timeout after 5 seconds
        if (System.currentTimeMillis() - stateStartTime > 5000) {
            // Force move to next point
            state = RouteMinerState.AIMING_AT_BLOCK;
            stateStartTime = System.currentTimeMillis();
        }
    }

    private void handleAiming() {
        MiningRoute.RoutePoint point = currentRoute.getCurrentPoint();
        if (point == null) {
            state = RouteMinerState.COMPLETED;
            return;
        }

        // Set precise angles
        aimAtAngles(point.pitch, point.yaw);

        // Small delay for smoothness
        if (System.currentTimeMillis() - stateStartTime > getRandomDelay()) {
            state = RouteMinerState.BREAKING_BLOCK;
            stateStartTime = System.currentTimeMillis();
            breakAttempts = 0;
            currentTargetBlock = findTargetBlock();
        }
    }

    private void handleBreaking() {
        if (currentTargetBlock == null) {
            currentTargetBlock = findTargetBlock();
            if (currentTargetBlock == null) {
                // No block found, move to next point
                currentRoute.getNextPoint();
                state = RouteMinerState.IDLE;
                return;
            }
        }

        // Check if block is already air
        Level level = mc.level;
        BlockState state = level.getBlockState(currentTargetBlock);

        if (state.isAir()) {
            // Block destroyed
            handleBlockDestroyed();
            return;
        }

        // Break the block
        breakBlock(currentTargetBlock);
        breakAttempts++;

        // Timeout or max attempts
        if (breakAttempts >= maxBreakAttempts ||
                System.currentTimeMillis() - stateStartTime > 3000) {
            // Block didn't break, might be bedrock or permanent
            handleBlockDestroyed(); // Move to next anyway
        }
    }

    private void handleBlockDestroyed() {
        // Check if this is a respawning ore type
        if (isRespawningOre(currentTargetBlock)) {
            state = RouteMinerState.WAITING_FOR_RESPAWN;
            stateStartTime = System.currentTimeMillis();
            // Wait 3-5 seconds for respawn (randomized)
            randomDelayOffset = 3000 + random.nextInt(2000);
        } else {
            // Permanent ore, move to next point
            currentRoute.getNextPoint();
            state = RouteMinerState.IDLE;
        }

        currentTargetBlock = null;
    }

    private void handleRespawnWait() {
        if (System.currentTimeMillis() - stateStartTime > randomDelayOffset) {
            // Respawn wait over, move to next point
            currentRoute.getNextPoint();
            state = RouteMinerState.IDLE;
        }
    }

    private BlockPos findTargetBlock() {
        // Find the block in front of the player
        LocalPlayer player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) return null;

        // Check blocks in front (1-3 blocks range)
        for (int i = 1; i <= 3; i++) {
            BlockPos pos = player.blockPosition().relative(player.getDirection(), i);
            BlockState state = level.getBlockState(pos);

            if (!state.isAir() && isMineable(state)) {
                return pos;
            }
        }

        return null;
    }

    private boolean isMineable(BlockState state) {
        // Check if block is mineable (not bedrock, etc.)
        float destroySpeed = state.getDestroySpeed(mc.level, BlockPos.ZERO);
        return destroySpeed >= 0 && destroySpeed < Float.MAX_VALUE;
    }

    private boolean isRespawningOre(BlockPos pos) {
        // Check if this is a respawning ore type
        // This would need to be configured based on server's ore types
        Level level = mc.level;
        BlockState state = level.getBlockState(pos);

        // Example: Check for specific ore blocks that respawn
        // This is server-specific and needs configuration
        String blockName = state.getBlock().toString().toLowerCase();
        return blockName.contains("ore") || blockName.contains("coal") ||
                blockName.contains("iron") || blockName.contains("gold");
    }

    private void breakBlock(BlockPos pos) {
        if (mc.gameMode == null) return;

        // Auto switch to best tool
        if (ThemedGuiModClient.CONFIG.autoSwitchTool) {
            switchToBestTool(mc.level.getBlockState(pos));
        }

        // Simulate left-click with randomization
        mc.options.keyAttack.setDown(true);

        // Random small delay for macro evasion
        try {
            Thread.sleep(50 + random.nextInt(20));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        mc.gameMode.destroyBlock(pos);

        mc.options.keyAttack.setDown(false);
    }

    private void switchToBestTool(BlockState targetState) {
        Player player = mc.player;
        if (player == null) return;

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

    private void aimAtPosition(double x, double y, double z) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        double dx = x - player.getX();
        double dy = y - (player.getY() + player.getEyeHeight());
        double dz = z - player.getZ();

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

        aimAtAngles(pitch, yaw);
    }

    private void aimAtAngles(float pitch, float yaw) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Add small random offset for macro evasion
        float randomPitchOffset = (random.nextFloat() - 0.5f) * 2.0f; // ±1 degree
        float randomYawOffset = (random.nextFloat() - 0.5f) * 2.0f; // ±1 degree

        player.setXRot(pitch + randomPitchOffset);
        player.setYRot(yaw + randomYawOffset);
    }

    private void addRandomDelay() {
        // Add random delay for macro evasion (±20% of base delay)
        randomDelayOffset = (long) (baseDelay * 0.2 * (random.nextFloat() - 0.5) * 2);
    }

    private long getRandomDelay() {
        return baseDelay + randomDelayOffset;
    }

    public void setRoute(MiningRoute route) {
        this.currentRoute = route;
        this.state = RouteMinerState.IDLE;
        if (route != null) {
            route.reset();
        }
    }

    public MiningRoute getRoute() {
        return currentRoute;
    }

    public boolean isActive() {
        return state != RouteMinerState.IDLE && state != RouteMinerState.COMPLETED;
    }

    public String getStateName() {
        return state.name();
    }

    public void setBaseDelay(long delay) {
        this.baseDelay = delay;
    }
}