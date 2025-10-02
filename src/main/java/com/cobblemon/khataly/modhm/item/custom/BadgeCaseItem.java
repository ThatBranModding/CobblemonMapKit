package com.cobblemon.khataly.modhm.item.custom;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.networking.handlers.BadgeBoxHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BadgeCaseItem extends Item {

    /** Tag: data/<modid>/tags/item/badges.json */
    public static final TagKey<Item> BADGE_TAG =
            TagKey.of(RegistryKeys.ITEM, Identifier.of(HMMod.MOD_ID, "badges"));

    /** Chiave dentro al CUSTOM_DATA */
    private static final String NBT_BADGES = "Badges"; // NbtList<String> con gli ID delle medaglie

    public BadgeCaseItem(Settings settings) {
        super(settings);
    }

    /** CLICK DESTRO → apre la GUI (server → client) */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack caseStack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            Set<Identifier> stored = readBadges(caseStack);
            int total = Registries.ITEM.getEntryList(BADGE_TAG)
                    .map(RegistryEntryList::size)
                    .orElse(stored.size());
            // Invia il pacchetto S2C per aprire la GUI
            BadgeBoxHandler.sendOpen(sp, hand, stored, total);
        }
        return TypedActionResult.success(caseStack, world.isClient);
    }

    /* ================== Tooltip ================== */

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        Set<Identifier> stored = readBadges(stack);
        int total = Registries.ITEM.getEntryList(BADGE_TAG).map(RegistryEntryList::size).orElse(8);

        tooltip.add(Text.translatable("tooltip." + HMMod.MOD_ID + ".badge_case.count", stored.size(), total));

        // ISTRUZIONI NUOVE
        tooltip.add(Text.translatable("tooltip." + HMMod.MOD_ID + ".badge_case.open_hint"));   // Apri GUI
        tooltip.add(Text.translatable("tooltip." + HMMod.MOD_ID + ".badge_case.insert_hint")); // Inserisci medaglia

        if (type == TooltipType.ADVANCED) {
            for (Identifier id : stored) {
                Item item = Registries.ITEM.get(id);
                MutableText name = new ItemStack(item).getName().copy();
                tooltip.add(Text.literal("• ").append(name));
            }
        }
    }


    /* ================== Helpers Data Components ================== */

    private static NbtCompound readCustomTag(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private static void writeCustomTag(ItemStack stack, NbtCompound newTag) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, tag -> tag.copyFrom(newTag));
    }

    /* ================== Stato contenuto (usati anche dagli handler) ================== */

    public static Set<Identifier> readBadges(ItemStack stack) {
        Set<Identifier> out = new LinkedHashSet<>();
        NbtCompound tag = readCustomTag(stack);
        if (tag.contains(NBT_BADGES, NbtElement.LIST_TYPE)) {
            NbtList list = tag.getList(NBT_BADGES, NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                Identifier id = Identifier.tryParse(list.getString(i));
                if (id != null) out.add(id);
            }
        }
        return out;
    }

    public static void writeBadges(ItemStack stack, Set<Identifier> ids) {
        NbtCompound tag = readCustomTag(stack);
        NbtList list = new NbtList();
        for (Identifier id : ids) list.add(NbtString.of(id.toString()));
        tag.put(NBT_BADGES, list);
        writeCustomTag(stack, tag);
    }
}
