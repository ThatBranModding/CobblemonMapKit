package com.cobblemon.khataly.mapkit.item.custom;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public class RunningShoesItem extends ArmorItem {
    private static final Identifier SPEED_ID = Identifier.of(CobblemonMapKitMod.MOD_ID, "running_shoes_speed");
    private final double speedBonus;

    public RunningShoesItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings, double speedBonusPercent) {
        super(material, type, settings.maxCount(1)); // item non impilabile
        this.speedBonus = speedBonusPercent;
    }

    @Override
    public AttributeModifiersComponent getAttributeModifiers() {
        // Modificatori base (armor/toughness/knockback) costruiti da ArmorItem
        AttributeModifiersComponent base = super.getAttributeModifiers();

        // Aggiunge il bonus velocità SOLO ai piedi.
        // NB: usa Identifier come id del modifier, coerente con ArmorItem.
        return base.with(
                EntityAttributes.GENERIC_MOVEMENT_SPEED,
                new EntityAttributeModifier(
                        SPEED_ID,
                        this.speedBonus,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ),
                AttributeModifierSlot.FEET
        );
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.mapkit.running_shoes").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
