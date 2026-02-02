package com.cobblemon.khataly.mapkit.command.custom;

import com.cobblemon.khataly.mapkit.config.FlyTargetConfig;
import com.cobblemon.khataly.mapkit.config.PlayerFlyProgress;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FlyTargetCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("flytarget")
                .requires(source -> source.hasPermissionLevel(2))

                // /flytarget list
                .then(CommandManager.literal("list")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            Map<String, FlyTargetConfig.TargetInfo> targets = FlyTargetConfig.getAllTargets();

                            if (targets.isEmpty()) {
                                source.sendMessage(Text.literal("§eNo fly targets registered."));
                                return 1;
                            }

                            source.sendMessage(Text.literal("§6-- Registered Fly Targets --"));
                            for (var entry : targets.entrySet()) {
                                String name = entry.getKey();
                                var pos = entry.getValue().pos;
                                source.sendMessage(Text.literal("§7" + name + " §f@ (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"));
                            }

                            return 1;
                        })
                )

                // /flytarget create <name...>
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerPlayerEntity player = source.getPlayer();
                                    if (player == null) {
                                        source.sendMessage(Text.literal("§cThis command must be executed by a player."));
                                        return 0;
                                    }

                                    String name = StringArgumentType.getString(context, "name").trim();
                                    if (name.isEmpty()) {
                                        source.sendMessage(Text.literal("§cName cannot be empty."));
                                        return 0;
                                    }

                                    BlockPos pos = player.getBlockPos();
                                    boolean success = FlyTargetConfig.addTarget(name, player.getServerWorld().getRegistryKey(), pos);

                                    if (success) {
                                        source.sendMessage(Text.literal("§aFly target '" + name + "' created at position " +
                                                pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
                                        return 1;
                                    } else {
                                        source.sendMessage(Text.literal("§cFailed to create fly target '" + name + "'."));
                                        return 0;
                                    }
                                })
                        )
                )

                // /flytarget remove <name...>
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .suggests(FlyTargetCommand::suggestTargets)
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    String input = StringArgumentType.getString(context, "name").trim();

                                    String actualKey = resolveTargetKey(input);
                                    if (actualKey == null) {
                                        source.sendMessage(Text.literal("§cFly target '" + input + "' not found."));
                                        return 0;
                                    }

                                    boolean removed = FlyTargetConfig.removeTarget(actualKey);
                                    if (removed) {
                                        source.sendMessage(Text.literal("§aFly target '" + actualKey + "' removed."));
                                        return 1;
                                    } else {
                                        source.sendMessage(Text.literal("§cFly target '" + actualKey + "' not found."));
                                        return 0;
                                    }
                                })
                        )
                )

                // /flytarget unlock <player> <name...>
                .then(CommandManager.literal("unlock")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .suggests(FlyTargetCommand::suggestTargets)
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                            String input = StringArgumentType.getString(context, "name").trim();

                                            String actualKey = resolveTargetKey(input);
                                            if (actualKey == null) {
                                                source.sendMessage(Text.literal("§cFly target '" + input + "' does not exist."));
                                                return 0;
                                            }

                                            String keyLower = actualKey.toLowerCase(Locale.ROOT);
                                            boolean added = PlayerFlyProgress.unlock(targetPlayer.getUuid(), keyLower);

                                            if (added) {
                                                source.sendMessage(Text.literal("§aUnlocked fly target '§e" + actualKey + "§a' for §b" + targetPlayer.getName().getString() + "§a."));
                                                targetPlayer.sendMessage(Text.literal("§7[Admin] §fNow you can fly to " + pretty(actualKey)), false);
                                                return 1;
                                            } else {
                                                source.sendMessage(Text.literal("§ePlayer already had fly target '§e" + actualKey + "§e' unlocked."));
                                                return 1;
                                            }
                                        })
                                )
                        )
                )

                // /flytarget clear <player>
                .then(CommandManager.literal("clear")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");

                                    PlayerFlyProgress.clearAll(targetPlayer.getUuid());
                                    source.sendMessage(Text.literal("§aCleared all unlocked fly targets for §b" + targetPlayer.getName().getString() + "§a."));
                                    targetPlayer.sendMessage(Text.literal("§7[Admin] §fYour unlocked fly targets have been cleared."), false);
                                    return 1;
                                })
                        )
                )

                // /flytarget reload
                .then(CommandManager.literal("reload")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            FlyTargetConfig.reload();
                            source.sendMessage(Text.literal("§aFly targets reloaded from file."));
                            return 1;
                        })
                )

                // /flytarget tp <name...>
                // /flytarget tp <player> <name...>
                //
                // NOTE: player-first branch exists because greedyString cannot have args after it.
                .then(CommandManager.literal("tp")
                        // tp <player> <name...>
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .suggests(FlyTargetCommand::suggestTargets)
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                            String input = StringArgumentType.getString(context, "name").trim();
                                            return teleportToTarget(source, targetPlayer, input);
                                        })
                                )
                        )
                        // tp <name...> (self)
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .suggests(FlyTargetCommand::suggestTargets)
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerPlayerEntity player = source.getPlayer();
                                    if (player == null) {
                                        source.sendMessage(Text.literal("§cThis command must be executed by a player."));
                                        return 0;
                                    }
                                    String input = StringArgumentType.getString(context, "name").trim();
                                    return teleportToTarget(source, player, input);
                                })
                        )
                )
        );
    }

    // ======== Suggestions & utils ========

    private static CompletableFuture<Suggestions> suggestTargets(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder
    ) {
        return CommandSource.suggestMatching(FlyTargetConfig.getAllTargets().keySet(), builder);
    }

    /**
     * Resolve a target name case-insensitively.
     * Returns the actual stored key (original casing) or null if not found.
     */
    private static String resolveTargetKey(String input) {
        if (input == null) return null;
        String needle = input.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return null;

        for (String key : FlyTargetConfig.getAllTargets().keySet()) {
            if (key.equalsIgnoreCase(needle) || key.toLowerCase(Locale.ROOT).equals(needle)) {
                return key;
            }
        }
        return null;
    }

    private static String pretty(String name) {
        String[] parts = name.replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    private static int teleportToTarget(ServerCommandSource source, ServerPlayerEntity player, String inputName) {
        String actualKey = resolveTargetKey(inputName);
        if (actualKey == null) {
            source.sendMessage(Text.literal("§cFly target '" + inputName + "' doesn't exist."));
            return 0;
        }

        Map<String, FlyTargetConfig.TargetInfo> all = FlyTargetConfig.getAllTargets();
        FlyTargetConfig.TargetInfo info = all.get(actualKey);
        if (info == null) {
            source.sendMessage(Text.literal("§cFly target '" + actualKey + "' doesn't exist."));
            return 0;
        }

        var server = source.getServer();
        var world = server.getWorld(info.worldKey);
        if (world == null) {
            source.sendMessage(Text.literal("§cDimension for the target '" + actualKey + "' not available."));
            return 0;
        }

        BlockPos pos = info.pos;
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        player.teleport(world, x, y, z, yaw, pitch);

        if (source.getEntity() != player) {
            source.sendMessage(Text.literal("§aYou teleported §b" + player.getName().getString() +
                    " §ato the target §e" + pretty(actualKey) + "§a."));
        }
        player.sendMessage(Text.literal("§7[FlyTarget] §fTeleported to §e" + pretty(actualKey) + "§f..."), false);
        return 1;
    }
}
