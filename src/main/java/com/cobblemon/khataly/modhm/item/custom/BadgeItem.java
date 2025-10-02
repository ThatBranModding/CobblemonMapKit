package com.cobblemon.khataly.modhm.item.custom;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.networking.handlers.BadgeBoxHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Set;

public class BadgeItem extends Item {

    public BadgeItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack badgeStack = user.getStackInHand(hand);

        if (world.isClient) return TypedActionResult.success(badgeStack, true);
        if (!(user instanceof net.minecraft.server.network.ServerPlayerEntity sp)) {
            return TypedActionResult.pass(badgeStack);
        }

        // Trova un Badge Case OVUNQUE nell'inventario (mani incluse)
        ItemStack caseStack = BadgeInventory.findCaseStack(sp);
        if (caseStack.isEmpty() || !(caseStack.getItem() instanceof BadgeCaseItem)) {
            sp.sendMessage(Text.translatable("msg." + HMMod.MOD_ID + ".badge_case.missing"), true);
            return TypedActionResult.pass(badgeStack);
        }

        // Inserisci la medaglia nel case
        Identifier badgeId = Registries.ITEM.getId(badgeStack.getItem());
        Set<Identifier> stored = BadgeCaseItem.readBadges(caseStack);
        boolean added = stored.add(badgeId);

        if (added) {
            badgeStack.decrement(1);                 // consuma 1
            BadgeCaseItem.writeBadges(caseStack, stored);
        }

        int total = Registries.ITEM.getEntryList(BadgeCaseItem.BADGE_TAG)
                .map(RegistryEntryList::size)
                .orElse(stored.size());

        // Scegli una mano "di comodo" per la GUI: se il case è in mano usa quella, altrimenti MAIN_HAND
        Hand caseHand = BadgeInventory.handIfHeld(sp);
        if (caseHand == null) caseHand = Hand.MAIN_HAND;

        // Apri GUI con stato aggiornato
        BadgeBoxHandler.sendOpen(sp, caseHand, stored, total);

        return TypedActionResult.success(badgeStack, false);
    }

    /** Utility per cercare il case nell'inventario/mani */
    static final class BadgeInventory {
        static ItemStack findCaseStack(net.minecraft.server.network.ServerPlayerEntity sp) {
            // Main-hand
            ItemStack main = sp.getMainHandStack();
            if (main.getItem() instanceof BadgeCaseItem) return main;

            // Off-hand
            ItemStack off = sp.getOffHandStack();
            if (off.getItem() instanceof BadgeCaseItem) return off;

            // Tutto l'inventario (main+armor+offhand)
            for (int i = 0; i < sp.getInventory().size(); i++) {
                ItemStack s = sp.getInventory().getStack(i);
                if (!s.isEmpty() && s.getItem() instanceof BadgeCaseItem) return s;
            }
            return ItemStack.EMPTY;
        }

        static Hand handIfHeld(net.minecraft.server.network.ServerPlayerEntity sp) {
            if (sp.getMainHandStack().getItem() instanceof BadgeCaseItem) return Hand.MAIN_HAND;
            if (sp.getOffHandStack().getItem() instanceof BadgeCaseItem) return Hand.OFF_HAND;
            return null;
        }
    }
}
