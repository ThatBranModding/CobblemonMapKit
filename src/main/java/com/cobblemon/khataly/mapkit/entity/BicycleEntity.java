package com.cobblemon.khataly.mapkit.entity;

import com.cobblemon.khataly.mapkit.item.ModItems;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BicycleEntity extends PathAwareEntity {
    public final AnimationState goingAnimation = new AnimationState();
    public BicycleEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.noClip = false;
        this.setPersistent();
        this.setInvulnerable(true);
    }

    /** Attributi base della bici (registrali nel main con FabricDefaultAttributeRegistry). */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35D);
    }

    /** Nessuna AI. */
    @Override
    protected void initGoals() {
        // no AI
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
    }

    /** Un pochino di “sicurezza” fisica. */
    @Override
    public void tick() {
        super.tick();
        this.setFireTicks(0);

        // --- AGGIUNTA: aggiorna lo stato dell'animazione SOLO lato client ---
        if (this.getWorld().isClient) {
            updateClientAnimations();
        }

        if (!this.getWorld().isClient) {
            if (this.isTouchingWater() || !this.isOnGround()) {
                this.setVelocity(this.getVelocity().multiply(0.85, 1.0, 0.85));
            }
        }
    }
    // BicycleEntity.java (client-side)
    private void updateClientAnimations() {
        if (!this.getWorld().isClient) return;

        double speedSq = this.getVelocity().horizontalLengthSquared();
        boolean moving = speedSq > 1.0E-4;

        if (moving) {
            // avvia una volta e lascia correre
            this.goingAnimation.startIfNotRunning(this.age);
        } else {
            this.goingAnimation.stop();
        }
    }


    /** Guida controllata dal giocatore che monta. */
    @Override
    public void travel(Vec3d movementInput) {
        if (this.hasPassengers()) {
            Entity rider = this.getFirstPassenger();
            if (rider instanceof PlayerEntity player) {
                // orientamento = quello del giocatore
                this.prevYaw = this.getYaw();
                this.setYaw(player.getYaw());
                this.bodyYaw = this.getYaw();
                this.headYaw = this.getYaw();
                this.setPitch(0);

                // input del giocatore
                float forward = player.forwardSpeed;   // W/S
                float strafe  = player.sidewaysSpeed;  // A/D
                if (forward < 0) forward *= 0.5f;      // retro = più lenta

                this.setMovementSpeed((float) this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
                super.travel(new Vec3d(strafe, 0, forward));
                return;
            }
        }
        super.travel(movementInput);
    }

    /** 1 solo passeggero (il player). */
    @Override
    public boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().isEmpty() && passenger instanceof PlayerEntity;
    }

    /** Step-up di 1 blocco pieno quando guidi. */
    @Override
    public float getStepHeight() {
        return 1.1F;
    }

    /** Interazione: sneak+mano vuota → raccogli; altrimenti prova a montare. */
    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player.isSneaking() && player.getStackInHand(hand).isEmpty()) {
            if (!this.getWorld().isClient) {
                // smonta eventuali passeggeri
                this.removeAllPassengers();

                // ridai l'item se non in creative
                if (!player.getAbilities().creativeMode) {
                    ItemStack bike = new ItemStack(ModItems.BICYCLE);
                    if (!player.getInventory().insertStack(bike)) {
                        player.dropItem(bike, false);
                    }
                }

                this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.8f, 1.1f);
                this.discard();
            }
            return ActionResult.SUCCESS;
        }

        // monta
        if (!this.getWorld().isClient) {
            if (player.hasVehicle()) player.stopRiding();
            if (this.getPassengerList().isEmpty()) {
                player.startRiding(this, true);
            }
        }
        return ActionResult.SUCCESS;
    }

    /** Non spingibile / niente collision pushes. */
    @Override public boolean isPushable() { return false; }
    @Override protected void pushAway(Entity entity) { /* no-op */ }
    @Override public boolean isPushedByFluids() { return false; }

    /** Non attaccabile / non danneggiabile. */
    @Override public boolean isAttackable() { return false; }
    @Override public boolean canBeHitByProjectile() { return false; }
    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) { return false; }
    @Override protected int getBurningDuration() { return 0; }

    /** NBT (nessun dato custom per ora). */
    @Override public void readCustomDataFromNbt(NbtCompound nbt) { super.readCustomDataFromNbt(nbt); }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) { super.writeCustomDataToNbt(nbt); }

    /** Helper di spawn: risolve l’altezza (sopra il terreno/fluido) e piazza un po’ più in alto. */
    @Nullable
    public static BicycleEntity spawn(ServerWorld world, BlockPos start, @Nullable PlayerEntity owner) {
        // sali finché il blocco non è “libero”
        BlockPos pos = start;
        while (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty() && pos.getY() < world.getTopY()) {
            pos = pos.up();
        }

        BicycleEntity e = ModEntities.BICYCLE.create(world);
        if (e == null) return null;

        // piazza sopra di ~1.2 blocchi per evitare “affondamenti” nel terreno/fluido
        e.refreshPositionAndAngles(
                pos.getX() + 0.5,
                pos.getY() + 1.2,
                pos.getZ() + 0.5,
                owner != null ? owner.getYaw() : 0f,
                0f
        );
        world.spawnEntity(e);
        return e;
    }
}
