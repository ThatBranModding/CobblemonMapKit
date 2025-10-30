package com.cobblemon.khataly.mapkit.item.custom;

import com.cobblemon.khataly.mapkit.entity.BicycleEntity;
import com.cobblemon.khataly.mapkit.sound.ModSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BicycleItem extends Item {
    public BicycleItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);

        if (player.isSwimming() || player.isFallFlying()) {
            player.sendMessage(Text.translatable("item.mapkit.bicycle.cannot_use_now"), true);
            return TypedActionResult.fail(stack);
        }

        BlockHitResult hit = (BlockHitResult) player.raycast(3.5D, 0.0F, false);
        BlockPos spawnPos = (hit.getType() == HitResult.Type.BLOCK)
                ? hit.getBlockPos().offset(hit.getSide())
                : player.getBlockPos().offset(player.getHorizontalFacing());

        if (!world.getBlockState(spawnPos).getCollisionShape(world, spawnPos).isEmpty()) {
            spawnPos = spawnPos.up();
        }

        ((ServerWorld) world).playSound(
                null,                           // null = udibile da tutti i giocatori nelle vicinanze
                player.getX(), player.getY(), player.getZ(),
                ModSounds.BIKE_BELL,    // usa .value() se BIKE_BELL è RegistryEntry<SoundEvent>
                net.minecraft.sound.SoundCategory.PLAYERS,
                1f, 1f
        );
        BicycleEntity bike = BicycleEntity.spawn((ServerWorld) world, spawnPos, player);
        if (bike != null) {
            player.startRiding(bike, true);
            player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), 0.8f, 1.2f);
            player.sendMessage(Text.translatable("item.mapkit.bicycle.enabled"), true);

            // ✅ consuma l’item se non in creative
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }

            // (opzionale) piccolo cooldown per evitare doppio spawn da doppio click
            player.getItemCooldownManager().set(this, 10);

            return TypedActionResult.consume(stack);
        }

        return TypedActionResult.fail(stack);
    }
}
