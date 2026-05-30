package com.fuck_friends;

import com.fuck_friends.config.FuckFriendsConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuckFriends implements ModInitializer {

    public static final Map<UUID, Integer> tpCounts = new HashMap<>();
    public static final Map<UUID, Integer> deathCounts = new HashMap<>();

    // 匹配 Xaero 跨维度发出的指令: "execute in minecraft:the_nether run tp @s 1 2 3"
    private static final Pattern EXECUTE_TP_PATTERN = Pattern.compile("^execute\\s+in\\s+([a-zA-Z0-9_:]+)\\s+run\\s+(?:tp|teleport)\\s+@s\\s+([-\\d.]+)\\s+([-\\d.]+)\\s+([-\\d.]+)$");
    // 匹配常规的小地图同维度传送: "tp @s 1 2 3"
    private static final Pattern NORMAL_TP_PATTERN = Pattern.compile("^(?:tp|teleport)\\s+@s\\s+([-\\d.]+)\\s+([-\\d.]+)\\s+([-\\d.]+)$");

    @Override
    public void onInitialize() {
        FuckFriendsConfig.loadConfig();

        // 重新注册无权限要求的原生命令。这会让客户端同步后知道这些命令是合法的。
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var executeLoc = (com.mojang.brigadier.Command<net.minecraft.commands.CommandSourceStack>) context -> {
                net.minecraft.commands.CommandSourceStack source = context.getSource();
                if (!checkAndConsumeTp(source)) return 0;
                
                Entity entity = source.getEntity();
                if (entity != null) {
                    var pos = Vec3Argument.getCoordinates(context, "location").getPosition(source);
                    if (entity instanceof ServerPlayer sp) {
                        sp.teleportTo(sp.serverLevel(), pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
                    } else {
                        entity.teleportTo(pos.x, pos.y, pos.z);
                    }
                }
                return 1;
            };

            var executeEnt = (com.mojang.brigadier.Command<net.minecraft.commands.CommandSourceStack>) context -> {
                net.minecraft.commands.CommandSourceStack source = context.getSource();
                if (!checkAndConsumeTp(source)) return 0;
                
                Entity targetEntity = EntityArgument.getEntity(context, "destination");
                Entity entity = source.getEntity();
                if (entity != null) {
                    if (entity instanceof ServerPlayer sp) {
                        sp.teleportTo((net.minecraft.server.level.ServerLevel) targetEntity.level(), targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(), targetEntity.getYRot(), targetEntity.getXRot());
                    } else {
                        entity.teleportTo(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ());
                    }
                }
                return 1;
            };

            // 注册同名的 tp / teleport 节点，强制把 require 设为 true 使得所有人都能发起命令
            var tpNode = Commands.literal("tp")
                .requires(source -> true)
                .then(Commands.argument("location", Vec3Argument.vec3()).executes(executeLoc))
                .then(Commands.argument("destination", EntityArgument.entity()).executes(executeEnt));
            dispatcher.register(tpNode);

            var teleportNode = Commands.literal("teleport")
                .requires(source -> true)
                .then(Commands.argument("location", Vec3Argument.vec3()).executes(executeLoc))
                .then(Commands.argument("destination", EntityArgument.entity()).executes(executeEnt));
            dispatcher.register(teleportNode);
        });

        // 最底层的聊天/命令拦截器
        // 用来捕获 Xaero 小地图发送的带有复杂参数的 /execute 命令或者其他小地图直接发送的字符
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register((message, source, params) -> {
            ServerPlayer player = source.getPlayer();
            if (player == null || player.server.getPlayerList().isOp(player.getGameProfile())) {
                return true;
            }

            String cmd = message.signedContent();
            
            // 检查跨维度传送
            Matcher execMatcher = EXECUTE_TP_PATTERN.matcher(cmd);
            if (execMatcher.matches()) {
                if (!checkAndConsumeTp(source)) return false;
                
                String dimensionId = execMatcher.group(1);
                try {
                    double x = Double.parseDouble(execMatcher.group(2));
                    double y = Double.parseDouble(execMatcher.group(3));
                    double z = Double.parseDouble(execMatcher.group(4));

                    net.minecraft.resources.ResourceLocation dimLoc = net.minecraft.resources.ResourceLocation.parse(dimensionId);
                    net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> targetDim = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimLoc);
                    net.minecraft.server.level.ServerLevel targetLevel = player.server.getLevel(targetDim);
                    
                    if (targetLevel != null) {
                        player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
                    }
                } catch (Exception ignored) {}
                return false; // 拦截掉原生 /execute 的解析，防止报没有权限
            }

            // 检查同维度的快捷传送（防范极端情况下客户端仍然红字的问题）
            Matcher normalMatcher = NORMAL_TP_PATTERN.matcher(cmd);
            if (normalMatcher.matches()) {
                if (!checkAndConsumeTp(source)) return false;

                try {
                    double x = Double.parseDouble(normalMatcher.group(1));
                    double y = Double.parseDouble(normalMatcher.group(2));
                    double z = Double.parseDouble(normalMatcher.group(3));
                    player.teleportTo(player.serverLevel(), x, y, z, player.getYRot(), player.getXRot());
                } catch (Exception ignored) {}
                return false;
            }

            return true;
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

    private boolean checkAndConsumeTp(net.minecraft.commands.CommandSourceStack source) {
        if (!source.hasPermission(2) && source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            if (player == null) return true;
            
            UUID uuid = player.getUUID();
            int currentTpCount = tpCounts.getOrDefault(uuid, 0);

            if (currentTpCount >= FuckFriendsConfig.getInstance().maxTpCount) {
                player.sendSystemMessage(Component.literal(FuckFriendsConfig.getInstance().messageTpLimitReached));
                return false;
            }
            tpCounts.put(uuid, currentTpCount + 1);
        }
        return true;
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
                    // Minecraft 1.21.1 返回类型不再是单一的 Optional<Vec3> 而是包含更多信息的 Record。
                    // 实际上最稳妥的方式是直接告诉 ServerPlayer 去执行 respawn 的核心逻辑，或者简单化：
                    // 因为无法直接使用 findRespawnPositionAndUseSpawnBlock，
                    // 我们可以直接把玩家传送到那个方块（床/重生锚）的坐标上方
                    
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
