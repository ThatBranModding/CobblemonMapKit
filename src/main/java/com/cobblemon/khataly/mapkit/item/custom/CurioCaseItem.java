package com.cobblemon.khataly.mapkit.item.custom;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.networking.handlers.CurioCaseHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.*;

/**
 * Curio Case saves list of curios with shine (0..100) and last updated timestamp.
 * NBT (CUSTOM_DATA):
 * Curios: List of Compound { id:String, shine:int(0..100), last:long(ms) }
 */
public class CurioCaseItem extends Item {

    /** data/<modid>/tags/item/curios.json */
    public static final TagKey<Item> CURIO_TAG =
            TagKey.of(RegistryKeys.ITEM, Identifier.of(CobblemonMapKitMod.MOD_ID, "curios"));

    /** Fixed capacity */
    public static final int HARD_MAX_SLOTS = 8;

    private static final String NBT_CURIOS = "Curios";
    private static final String KEY_ID   = "id";
    private static final String KEY_SH   = "shine";
    private static final String KEY_LAST = "last";

    /** Offline decay: ~20 minutes from 100 -> 0 */
    private static final long  FULL_DECAY_MS = 20L * 60L * 1000L;
    private static final float DECAY_PER_SEC = 100f / (FULL_DECAY_MS / 1000f);

    public record CurioData(Identifier id, int shine, long last) {}

    public CurioCaseItem(Settings settings) { super(settings); }

    /** Always 8 */
    public static int getMaxSlots() { return HARD_MAX_SLOTS; }

    /** Right click => open GUI */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack caseStack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            List<CurioData> data = readCuriosDataAndDecay(caseStack); // trims
            CurioCaseHandler.sendOpen(sp, hand, data, getMaxSlots(), Optional.empty());
        }
        return TypedActionResult.success(caseStack, world.isClient);
    }

    /** Tooltip: count + (ADVANCED) list with shine% */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        List<CurioData> data  = readCuriosDataAndDecay(stack);
        int total = getMaxSlots();
        long collected = data.stream().filter(d -> d.id() != null).count();

        tooltip.add(Text.translatable("tooltip." + CobblemonMapKitMod.MOD_ID + ".curio_case.count", collected, total));

        if (type == TooltipType.ADVANCED) {
            for (CurioData d : data) {
                if (d.id() == null) continue;
                Item item = Registries.ITEM.get(d.id());
                MutableText name = new ItemStack(item).getName().copy();
                tooltip.add(Text.literal("• ").append(name).append(Text.literal("  (" + d.shine() + "%)")));
            }
        } else {
            tooltip.add(Text.translatable("tooltip." + CobblemonMapKitMod.MOD_ID + ".curio_case.hint"));
        }
    }

    /* ===================== API used by handlers / utilities ===================== */

    public static boolean isFull(ItemStack caseStack) {
        int max = getMaxSlots();
        int count = 0;
        for (CurioData b : readCuriosData(caseStack)) {
            if (b.id() != null) count++;
            if (count >= max) return true;
        }
        return false;
    }

    public static boolean addCurio(ItemStack caseStack, Identifier id) {
        if (id == null) return false;

        List<CurioData> data = readCuriosDataAndDecay(caseStack);

        // already?
        boolean already = data.stream().anyMatch(b -> id.equals(b.id()));
        if (already) return false;

        // full?
        int filled = 0;
        for (CurioData b : data) if (b.id() != null) filled++;
        if (filled >= getMaxSlots()) return false;

        long now = System.currentTimeMillis();
        data.add(new CurioData(id, 0, now));

        if (data.size() > getMaxSlots()) {
            data = new ArrayList<>(data.subList(0, getMaxSlots()));
        }

        writeCuriosData(caseStack, data);
        return true;
    }

    public static boolean polish(ItemStack caseStack, Identifier id, int amount) {
        long now = System.currentTimeMillis();
        List<CurioData> data = readCuriosDataAndDecay(caseStack);
        boolean changed = false;

        for (int i = 0; i < data.size(); i++) {
            CurioData b = data.get(i);
            if (id.equals(b.id())) {
                int newSh = Math.min(100, Math.max(0, b.shine() + amount));
                if (newSh != b.shine()) changed = true;
                data.set(i, new CurioData(b.id(), newSh, now));
                break;
            }
        }
        if (changed) writeCuriosData(caseStack, data);
        return changed;
    }

    public static boolean remove(ItemStack caseStack, Identifier id) {
        List<CurioData> data = readCuriosDataAndDecay(caseStack);
        boolean removed = data.removeIf(b -> id.equals(b.id()));
        if (removed) writeCuriosData(caseStack, data);
        return removed;
    }

    /** Read data applying decay, and TRIMS to 8 (writes back if changes). */
    public static List<CurioData> readCuriosDataAndDecay(ItemStack stack) {
        int max = getMaxSlots();
        List<CurioData> list = readCuriosDataRaw(stack);

        long now = System.currentTimeMillis();
        boolean changed = false;

        ArrayList<CurioData> out = new ArrayList<>(list.size());

        for (CurioData b : list) {
            if (b.id() == null || b.shine() <= 0) {
                out.add(b);
                continue;
            }
            long elapsedMs = Math.max(0, now - Math.max(0, b.last()));
            if (elapsedMs <= 0) {
                out.add(b);
                continue;
            }
            int drop = Math.max(0, Math.round((elapsedMs / 1000f) * DECAY_PER_SEC));
            int newSh = Math.max(0, b.shine() - drop);
            if (newSh != b.shine()) changed = true;
            out.add(new CurioData(b.id(), newSh, now));
        }

        if (out.size() > max) {
            out.subList(max, out.size()).clear();
            changed = true;
        }

        if (changed) writeCuriosData(stack, out);
        return out;
    }

    /** Read-only copy without decay; still clamps to 8 (no write). */
    public static List<CurioData> readCuriosData(ItemStack stack) {
        int max = getMaxSlots();
        List<CurioData> raw = readCuriosDataRaw(stack);
        if (raw.size() <= max) return new ArrayList<>(raw);
        return new ArrayList<>(raw.subList(0, max));
    }

    /* ===================== NBT (low-level) ===================== */

    private static List<CurioData> readCuriosDataRaw(ItemStack stack) {
        NbtCompound tag = readCustomTag(stack);
        ArrayList<CurioData> out = new ArrayList<>();
        if (!tag.contains(NBT_CURIOS, NbtElement.LIST_TYPE)) return out;

        NbtList list = tag.getList(NBT_CURIOS, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            Identifier id = c.contains(KEY_ID) ? Identifier.tryParse(c.getString(KEY_ID)) : null;
            int sh  = c.contains(KEY_SH)   ? Math.max(0, Math.min(100, c.getInt(KEY_SH))) : 0;
            long ls = c.contains(KEY_LAST) ? c.getLong(KEY_LAST) : System.currentTimeMillis();
            if (id != null) out.add(new CurioData(id, sh, ls));
        }
        return out;
    }

    public static void writeCuriosData(ItemStack stack, List<CurioData> data) {
        int max = getMaxSlots();
        NbtCompound tag = readCustomTag(stack);
        NbtList list = new NbtList();

        int n = Math.min(max, data.size());
        for (int i = 0; i < n; i++) {
            CurioData b = data.get(i);
            if (b.id() == null) continue;
            NbtCompound c = new NbtCompound();
            c.putString(KEY_ID, b.id().toString());
            c.putInt(KEY_SH, Math.max(0, Math.min(100, b.shine())));
            c.putLong(KEY_LAST, b.last());
            list.add(c);
        }

        tag.put(NBT_CURIOS, list);
        writeCustomTag(stack, tag);
    }

    private static NbtCompound readCustomTag(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private static void writeCustomTag(ItemStack stack, NbtCompound newTag) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, tag -> tag.copyFrom(newTag));
    }
}
