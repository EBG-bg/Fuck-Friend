package com.fuck_friends;

import com.fuck_friends.config.FuckFriendsConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class FuckFriends implements ModInitializer {

    public static final Map<UUID, Integer> tpCounts = new HashMap<>();
    public static final Map<UUID, Integer> deathCounts = new HashMap<>();

    @Override
    public void onInitialize() {
        FuckFriendsConfig.loadConfig();

        // 针对小地图的特殊处理：原版即使解锁了 /tp 命令，也会在参数解析底层（EntitySelectorParser）
        // 强行拦截非 OP 玩家使用任何 '@' 开头的选择器（包括 @s）。
        // 因此我们手动为 /tp 和 /teleport 注册一个 "@s" 的「字面量(Literal)」分支来绕过选择器解析，
        // 让小地图发出的 `/tp @s x y z` 可以作为普通文本被合法解析并执行。
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var executeLoc = (com.mojang.brigadier.Command<CommandSourceStack>) context -> {
                CommandSourceStack source = context.getSource();
                Entity entity = source.getEntity();
                if (entity != null) {
                    var pos = Vec3Argument.getCoordinates(context, "location").getPosition(source);
                    if (entity instanceof ServerPlayer sp) {
                        sp.teleportTo(source.getLevel(), pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
                    } else {
                        entity.teleportTo(pos.x, pos.y, pos.z);
                    }
                    source.sendSuccess(() -> Component.translatable("commands.teleport.success.location.single", entity.getDisplayName(), pos.x, pos.y, pos.z), true);
                }
                return 1;
            };

            dispatcher.register(Commands.literal("tp")
                .then(Commands.literal("@s")
                    .then(Commands.argument("location", Vec3Argument.vec3()).executes(executeLoc))
                )
            );

            dispatcher.register(Commands.literal("teleport")
                .then(Commands.literal("@s")
                    .then(Commands.argument("location", Vec3Argument.vec3()).executes(executeLoc))
                )
            );
        });

        // 在服务器完全启动，所有命令树都合并完毕后，我们统一解锁权限，并套上次数限制器
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                Field reqField = CommandNode.class.getDeclaredField("requirement");
                reqField.setAccessible(true);

                Field cmdField = CommandNode.class.getDeclaredField("command");
                cmdField.setAccessible(true);

                var dispatcher = server.getCommands().getDispatcher();
                var root = dispatcher.getRoot();

                // Unlock and wrap tp and teleport
                wrapNode(root.getChild("tp"), reqField, cmdField, true);
                wrapNode(root.getChild("teleport"), reqField, cmdField, true);

                // Unlock execute -> in -> dimension -> run
                // 这允许小地图发出的 /execute in <dim> run tp @s ... 能够穿透权限检查
                CommandNode<CommandSourceStack> executeNode = root.getChild("execute");
                if (executeNode != null) {
                    unlockNode(executeNode, reqField);
                    CommandNode<CommandSourceStack> inNode = executeNode.getChild("in");
                    if (inNode != null) {
                        unlockNode(inNode, reqField);
                        CommandNode<CommandSourceStack> dimNode = inNode.getChild("dimension");
                        if (dimNode != null) {
                            unlockNode(dimNode, reqField);
                            CommandNode<CommandSourceStack> runNode = dimNode.getChild("run");
                            if (runNode != null) {
                                unlockNode(runNode, reqField);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("[FuckFriends] Failed to wrap command nodes:");
                e.printStackTrace();
            }
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            long currentTime = server.overworld().getGameTime();
            int interval = FuckFriendsConfig.getInstance().resetIntervalTicks;
            
            if (currentTime > 0 && currentTime % interval == 0) {
                resetLimits(server);
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isSpectatorDueToDeath(player.getUUID()) && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    long remainingTicks = interval - (currentTime % interval);
                    long remainingSeconds = remainingTicks / 20;
                    long minutes = remainingSeconds / 60;
                    long seconds = remainingSeconds % 60;
                    
                    String actionBarText = String.format(FuckFriendsConfig.getInstance().actionbarSpectatorTime, minutes, seconds);
                    player.displayClientMessage(Component.literal(actionBarText), true);
                }
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            UUID uuid = newPlayer.getUUID();
            if (newPlayer.server.getPlayerList().isOp(newPlayer.getGameProfile())) {
                return;
            }
            
            int deaths = deathCounts.getOrDefault(uuid, 0) + 1;
            deathCounts.put(uuid, deaths);
            
            if (deaths > FuckFriendsConfig.getInstance().maxDeathCount) {
                newPlayer.setGameMode(GameType.SPECTATOR);
                newPlayer.sendSystemMessage(Component.literal(FuckFriendsConfig.getInstance().messageSpectatorMode));
            }
        });
    }

    // 专用的命令执行外壳，用于拦截原生逻辑并加入次数校验
    private static class LimitWrapper implements com.mojang.brigadier.Command<CommandSourceStack> {
        private final com.mojang.brigadier.Command<CommandSourceStack> original;

        public LimitWrapper(com.mojang.brigadier.Command<CommandSourceStack> original) {
            this.original = original;
        }

        @Override
        public int run(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
            CommandSourceStack source = context.getSource();
            if (!source.hasPermission(2) && source.isPlayer()) {
                ServerPlayer player = source.getPlayer();
                if (player != null) {
                    UUID uuid = player.getUUID();
                    int currentTpCount = FuckFriends.tpCounts.getOrDefault(uuid, 0);

                    if (currentTpCount >= FuckFriendsConfig.getInstance().maxTpCount) {
                        player.sendSystemMessage(Component.literal(FuckFriendsConfig.getInstance().messageTpLimitReached));
                        return 0; // 拒绝执行
                    }
                    
                    int result = original.run(context);
                    if (result > 0) {
                        FuckFriends.tpCounts.put(uuid, currentTpCount + 1); // 成功传送则次数+1
                    }
                    return result;
                }
            }
            // OP 玩家或者非玩家实体直接放行
            return original.run(context);
        }
    }

    private static void unlockNode(CommandNode<CommandSourceStack> node, Field reqField) throws IllegalAccessException {
        if (node == null) return;
        reqField.set(node, (Predicate<CommandSourceStack>) s -> true);
    }

    private static void wrapNode(CommandNode<CommandSourceStack> node, Field reqField, Field cmdField, boolean unlock) throws IllegalAccessException {
        if (node == null) return;
        
        if (unlock) {
            unlockNode(node, reqField);
        }
        
        com.mojang.brigadier.Command<CommandSourceStack> originalCmd = node.getCommand();
        if (originalCmd != null && !(originalCmd instanceof LimitWrapper)) {
            cmdField.set(node, new LimitWrapper(originalCmd));
        }
        
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            wrapNode(child, reqField, cmdField, unlock);
        }
    }

    private void resetLimits(MinecraftServer server) {
        tpCounts.clear();
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isSpectatorDueToDeath(player.getUUID()) && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                player.setGameMode(GameType.SURVIVAL); // Reset to survival
                
                // 获取玩家私人重生点所在的维度
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> respawnDimension = player.getRespawnDimension();
                net.minecraft.server.level.ServerLevel targetLevel = server.getLevel(respawnDimension);
                net.minecraft.core.BlockPos respawnPos = player.getRespawnPosition();
                float respawnAngle = player.getRespawnAngle();
                
                boolean spawnedAtBed = false;
                
                if (targetLevel != null && respawnPos != null) {
                    // 获取床附近的安全位置（为了简单不依赖未知的混淆方法，直接加 0.5 偏移和 1 格高度）
                    double x = respawnPos.getX() + 0.5;
                    double y = respawnPos.getY() + 1.0;
                    double z = respawnPos.getZ() + 0.5;

                    player.teleportTo(targetLevel, x, y, z, respawnAngle, 0);
                    spawnedAtBed = true;
                }
                
                // 如果没有床、床被破坏或维度不存在，兜底传送到主世界中心出生点
                if (!spawnedAtBed) {
                    net.minecraft.server.level.ServerLevel overworld = server.overworld();
                    net.minecraft.core.BlockPos spawnPos = overworld.getSharedSpawnPos();
                    player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, overworld.getSharedSpawnAngle(), 0);
                }
            }
        }
        
        deathCounts.clear();
        
        Component resetMessage = Component.literal(FuckFriendsConfig.getInstance().messageReset);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(resetMessage);
        }
    }

    public static boolean isSpectatorDueToDeath(UUID uuid) {
        return deathCounts.getOrDefault(uuid, 0) > FuckFriendsConfig.getInstance().maxDeathCount;
    }
}
