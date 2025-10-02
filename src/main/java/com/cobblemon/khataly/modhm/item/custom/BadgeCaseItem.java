package com.cobblemon.khataly.modhm.item.custom;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.networking.handlers.BadgeBoxHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.*;

public class BadgeCaseItem extends Item {

    /** data/<modid>/tags/item/badges.json */
    public static final TagKey<Item> BADGE_TAG =
            TagKey.of(RegistryKeys.ITEM, Identifier.of(HMMod.MOD_ID, "badges"));

    /** Badges: lista di compound { id, shine, last } */
    private static final String NBT_BADGES = "Badges";
    private static final String KEY_ID   = "id";
    private static final String KEY_SH   = "shine";
    private static final String KEY_LAST = "last";

    /** Decadimento offline: 20 minuti 100→0 */
    private static final long  FULL_DECAY_MS = 20L * 60L * 1000L;
    private static final float DECAY_PER_SEC = 100f / (FULL_DECAY_MS / 1000f);

    public record BadgeData(Identifier id, int shine, long last) {}

    public BadgeCaseItem(Settings settings) { super(settings); }

    /** Click destro → apri GUI (server → client) */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack caseStack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            // Applica decadimento offline e invia lo stato
            var data  = readBadgesDataAndDecay(caseStack);
            int total = totalCountOrDefault(data.size());
            BadgeBoxHandler.sendOpen(sp, hand, data, total, Optional.empty());
        }
        return TypedActionResult.success(caseStack, world.isClient);
    }

    /** Tooltip: mostra count e (in advanced) lo shine % */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var data  = readBadgesDataAndDecay(stack); // decadi anche qui
        int total = Registries.ITEM.getEntryList(BADGE_TAG).map(RegistryEntryList::size).orElse(8);

        long collected = data.stream().filter(d -> d.id() != null).count();
        tooltip.add(Text.translatable("tooltip." + HMMod.MOD_ID + ".badge_case.count", collected, total));

        if (type == TooltipType.ADVANCED) {
            for (BadgeData d : data) {
                if (d.id() == null) continue;
                Item item = Registries.ITEM.get(d.id());
                MutableText name = new ItemStack(item).getName().copy();
                tooltip.add(Text.literal("• ").append(name).append(Text.literal("  (" + d.shine() + "%)")));
            }
        } else {
            tooltip.add(Text.translatable("tooltip." + HMMod.MOD_ID + ".badge_case.hint"));
        }
    }

    /* ========= API usata da handler/BadgeItem ========= */

    /** Inserisci badge se non già presente (shine=0, last=now). */
    public static boolean addBadge(ItemStack caseStack, Identifier id) {
        long now = System.currentTimeMillis();
        var data = readBadgesDataAndDecay(caseStack);
        boolean already = data.stream().anyMatch(b -> id.equals(b.id()));
        if (already) return false;
        data.add(new BadgeData(id, 0, now));
        writeBadgesData(caseStack, data);
        return true;
    }

    /** Lucida: +amount (max 100), aggiorna last=now. */
    public static boolean polish(ItemStack caseStack, Identifier id, int amount) {
        long now = System.currentTimeMillis();
        var data = readBadgesDataAndDecay(caseStack);
        boolean changed = false;
        for (int i = 0; i < data.size(); i++) {
            var b = data.get(i);
            if (id.equals(b.id())) {
                int newSh = Math.min(100, b.shine() + Math.max(1, amount));
                if (newSh != b.shine()) changed = true;
                data.set(i, new BadgeData(b.id(), newSh, now));
                break;
            }
        }
        if (changed) writeBadgesData(caseStack, data);
        return changed;
    }

    /** Rimuovi badge (se presente). Ritorna true se rimosso. */
    public static boolean remove(ItemStack caseStack, Identifier id) {
        var data = readBadgesDataAndDecay(caseStack);
        boolean removed = data.removeIf(b -> id.equals(b.id()));
        if (removed) writeBadgesData(caseStack, data);
        return removed;
    }

    /** Legge e applica decadimento offline. */
    public static List<BadgeData> readBadgesDataAndDecay(ItemStack stack) {
        var list = readBadgesDataRaw(stack);
        long now = System.currentTimeMillis();
        boolean changed = false;
        var out = new ArrayList<BadgeData>(list.size());

        for (var b : list) {
            if (b.id() == null || b.shine() <= 0) { out.add(b); continue; }
            long elapsedMs = Math.max(0, now - Math.max(0, b.last()));
            if (elapsedMs <= 0) { out.add(b); continue; }
            int drop = Math.max(0, Math.round((elapsedMs / 1000f) * DECAY_PER_SEC));
            int newSh = Math.max(0, b.shine() - drop);
            if (newSh != b.shine()) changed = true;
            out.add(new BadgeData(b.id(), newSh, now)); // advance last=now
        }
        if (changed) writeBadgesData(stack, out);
        return out;
    }

    /* ========= NBT low-level ========= */

    private static List<BadgeData> readBadgesDataRaw(ItemStack stack) {
        var tag = readCustomTag(stack);
        var out = new ArrayList<BadgeData>();
        if (!tag.contains(NBT_BADGES, NbtElement.LIST_TYPE)) return out;

        NbtList list = tag.getList(NBT_BADGES, NbtElement.COMPOUND_TYPE);

        // compat col vecchio formato (lista di stringhe)
        if (list.isEmpty() || list.getType() == NbtElement.STRING_TYPE) {
            long now = System.currentTimeMillis();
            var newList = new NbtList();
            for (int i = 0; i < list.size(); i++) {
                Identifier id = Identifier.tryParse(list.getString(i));
                if (id == null) continue;
                out.add(new BadgeData(id, 0, now));
                var c = new NbtCompound();
                c.putString(KEY_ID, id.toString());
                c.putInt(KEY_SH, 0);
                c.putLong(KEY_LAST, now);
                newList.add(c);
            }
            tag.put(NBT_BADGES, newList);
            writeCustomTag(stack, tag);
            return out;
        }

        for (int i = 0; i < list.size(); i++) {
            var c = list.getCompound(i);
            Identifier id = c.contains(KEY_ID) ? Identifier.tryParse(c.getString(KEY_ID)) : null;
            int sh  = c.contains(KEY_SH)   ? Math.max(0, Math.min(100, c.getInt(KEY_SH))) : 0;
            long ls = c.contains(KEY_LAST) ? c.getLong(KEY_LAST) : System.currentTimeMillis();
            if (id != null) out.add(new BadgeData(id, sh, ls));
        }
        return out;
    }

    public static void writeBadgesData(ItemStack stack, List<BadgeData> data) {
        var tag = readCustomTag(stack);
        var list = new NbtList();
        for (var b : data) {
            if (b.id() == null) continue;
            var c = new NbtCompound();
            c.putString(KEY_ID, b.id().toString());
            c.putInt(KEY_SH, Math.max(0, Math.min(100, b.shine())));
            c.putLong(KEY_LAST, b.last());
            list.add(c);
        }
        tag.put(NBT_BADGES, list);
        writeCustomTag(stack, tag);
    }

    public static int totalCountOrDefault(int fallback) {
        return Registries.ITEM.getEntryList(BADGE_TAG).map(RegistryEntryList::size).orElse(fallback);
    }

    private static NbtCompound readCustomTag(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }
    private static void writeCustomTag(ItemStack stack, NbtCompound newTag) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, tag -> tag.copyFrom(newTag));
    }
}
