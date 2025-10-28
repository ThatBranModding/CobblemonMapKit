package com.cobblemon.khataly.mapkit.command.custom;

import com.cobblemon.khataly.mapkit.util.TeleportPairRegistry;
import com.cobblemon.khataly.mapkit.util.TeleportPairRegistry.TeleportLocation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;



import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TeleportBlockCommands {

    /** Suggeritore dinamico per gli ID delle coppie di TeleportBlock. */
    private static final SuggestionProvider<ServerCommandSource> SUGGEST_IDS = TeleportBlockCommands::suggestIds;

    private static CompletableFuture<Suggestions> suggestIds(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        ServerWorld world = ctx.getSource().getWorld();
        TeleportPairRegistry reg = TeleportPairRegistry.get(world);

        // Raccogliamo sia gli ID completi che quelli abbreviati (8 char), senza duplicati e mantenendo ordine d’inserimento
        Set<String> candidates = new LinkedHashSet<>();
        reg.getPairs().keySet().forEach(full -> {
            candidates.add(full);
            candidates.add(full.substring(0, Math.min(8, full.length())));
        });

        // Lasciamo a Brigadier il filtraggio rispetto al testo già digitato
        return CommandSource.suggestMatching(candidates, builder);
    }

    /**
     * /teleportblock
     *   ├─ list
     *   ├─ cleanup
     *   └─ remove <id|prefix>   (con suggestions)
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("teleportblock")
                .requires(src -> src.hasPermissionLevel(2))

                // /teleportblock list
                .then(CommandManager.literal("list")
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            ServerWorld world = source.getWorld();

                            TeleportPairRegistry reg = TeleportPairRegistry.get(world);
                            var pairs = reg.getPairs();

                            if (pairs.isEmpty()) {
                                source.sendMessage(Text.literal("§eNo teleport pairs registered."));
                                return 1;
                            }

                            source.sendMessage(Text.literal("§6-- Registered Teleport Pairs --"));
                            pairs.forEach((id, pair) -> {
                                String a = formatPos(pair.a());
                                String b = (pair.b() != null) ? formatPos(pair.b()) : "--";
                                source.sendMessage(Text.literal("§7" + id.substring(0, 8) + " §f: §b" + a + " §7<-> §b" + b));
                            });

                            return 1;
                        })
                )

                // /teleportblock cleanup
                .then(CommandManager.literal("cleanup")
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            ServerWorld world = source.getWorld();

                            TeleportPairRegistry reg = TeleportPairRegistry.get(world);
                            int removed = reg.cleanup(world);

                            source.sendMessage(Text.literal("§aCleaned up " + removed + " incomplete teleport pairs."));
                            return removed;
                        })
                )

                // /teleportblock remove <id|prefix>  (con suggestions sugli ID)
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(SUGGEST_IDS)
                                .executes(ctx -> {
                                    ServerCommandSource source = ctx.getSource();
                                    ServerWorld world = source.getWorld();
                                    String input = StringArgumentType.getString(ctx, "id");

                                    TeleportPairRegistry reg = TeleportPairRegistry.get(world);

                                    // Match: prima prova esatto, altrimenti prefisso
                                    Optional<String> fullId = reg.getPairs().keySet().stream()
                                            .filter(k -> k.equals(input) || k.startsWith(input))
                                            .findFirst();

                                    if (fullId.isEmpty()) {
                                        source.sendMessage(Text.literal("§cTeleport pair ID not found."));
                                        return 0;
                                    }

                                    // Rimuoviamo direttamente dalla mappa e salviamo
                                    reg.getPairs().remove(fullId.get());
                                    reg.markDirty();

                                    source.sendMessage(Text.literal("§aTeleport pair " + fullId.get().substring(0, 8) + " removed."));
                                    return 1;
                                })
                        )
                )
        );
    }

    /** "(x, y, z) in namespace:dimension" */
    private static String formatPos(TeleportLocation loc) {
        return String.format("(%d, %d, %d) in %s",
                loc.pos().getX(),
                loc.pos().getY(),
                loc.pos().getZ(),
                loc.dimension().getValue().toString()
        );
    }
}
