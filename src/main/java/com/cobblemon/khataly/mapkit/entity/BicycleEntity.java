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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Bicycle entity controllata dal giocatore.
 * Supporta marce, impennata e posizione dinamica del rider.
 */
public class BicycleEntity extends PathAwareEntity {

    public final AnimationState goingAnimation = new AnimationState();

    private boolean fastGear = true;       // true = marcia veloce ⚡, false = lenta 🐢
    private boolean wheelieActive = false; // true se SPACE è tenuto premuto
    private int wheelieTicks = 0;          // contatore per animazione
    private float wheelieOffset = 0;       // traslazione per renderer

    public BicycleEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.noClip = false;
        this.setPersistent();
        this.setInvulnerable(true);
    }

    /** Attributi base della bici. */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 0.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.85D);
    }

    @Override protected void initGoals() {}
    @Override protected void initDataTracker(DataTracker.Builder builder) { super.initDataTracker(builder); }

    // ==============================
    // ⚙️ Tick logico
    // ==============================
    @Override
    public void tick() {
        super.tick();
        this.setFireTicks(0);

        if (this.getWorld().isClient) {
            updateClientAnimations();
        } else if (this.isTouchingWater() || !this.isOnGround()) {
            this.setVelocity(this.getVelocity().multiply(0.85, 1.0, 0.85));
        }

        updateWheelieMotion();
    }

    /** Aggiorna animazioni lato client. */
    private void updateClientAnimations() {
        double speedSq = this.getVelocity().horizontalLengthSquared();
        boolean moving = speedSq > 1.0E-4;
        if (moving) this.goingAnimation.startIfNotRunning(this.age);
        else this.goingAnimation.stop();
    }

    /** Logica di impennata/saltello e aggiornamento rider. */
    private void updateWheelieMotion() {
        if (!wheelieActive) {
            if (wheelieTicks > 0) {
                wheelieTicks = 0;
                wheelieOffset = 0;
                this.setPitch(0);

                // reset posizione rider
                if (!this.getPassengerList().isEmpty()) {
                    Entity rider = this.getFirstPassenger();
                    if (rider != null) {
                        Vec3d basePos = getPassengerBasePos();
                        rider.setPosition(basePos.x, basePos.y, basePos.z);
                        rider.setPitch(0);
                    }
                }
            }
            return;
        }

        wheelieTicks++;

        // Pitch negativo = ruota anteriore si alza
        double hop = Math.sin(wheelieTicks / 3.0) * 0.08;
        float targetPitch = (float) (-25.0 - hop * 10.0);
        if (targetPitch < -40f) targetPitch = -40f;
        this.setPitch(targetPitch);

        // offset per renderer
        this.wheelieOffset = (float) Math.min(0.4f, Math.abs(Math.sin(wheelieTicks / 6.0)) * 0.4f);

        // piccolo saltello
        if (hop > 0 && this.isOnGround()) {
            Vec3d v = this.getVelocity();
            this.setVelocity(v.x, 0.25, v.z);
            this.velocityDirty = true;
        }

        // particelle e suono
        if (wheelieTicks % 8 == 0 && this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.3, this.getZ(),
                    4, 0.12, 0.0, 0.12, 0.01);
            this.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.35f, 1.2f);
        }

        // aggiorna posizione del rider in tempo reale
        if (!this.getPassengerList().isEmpty()) {
            Entity rider = this.getFirstPassenger();
            if (rider != null) {
                Vec3d pos = getPassengerWheeliePos();
                rider.setPosition(pos.x, pos.y, pos.z);
                rider.setPitch(this.getPitch() / 1.5f);
            }
        }
    }

    /** Posizione base del rider (seduto normale). */
    private Vec3d getPassengerBasePos() {
        double offsetY = 1.1;
        double offsetZ = -0.6;
        float yawRad = (float) Math.toRadians(this.getYaw());
        double x = this.getX() - Math.sin(yawRad) * offsetZ;
        double y = this.getY() + offsetY;
        double z = this.getZ() + Math.cos(yawRad) * offsetZ;
        return new Vec3d(x, y, z);
    }

    /** Posizione del rider durante l’impennata (più indietro e più basso). */
    private Vec3d getPassengerWheeliePos() {
        double offsetY = 0.65;
        double offsetZ = -1.9;
        float yawRad = (float) Math.toRadians(this.getYaw());
        double x = this.getX() - Math.sin(yawRad) * offsetZ;
        double y = this.getY() + offsetY;
        double z = this.getZ() + Math.cos(yawRad) * offsetZ;
        return new Vec3d(x, y, z);
    }

    // ==============================
    // 🚲 Movimento controllato dal giocatore
    // ==============================
    @Override
    public void travel(Vec3d movementInput) {
        if (this.hasPassengers()) {
            Entity rider = this.getFirstPassenger();
            if (rider instanceof PlayerEntity player) {
                this.prevYaw = this.getYaw();
                this.setYaw(player.getYaw());
                this.bodyYaw = this.getYaw();
                this.headYaw = this.getYaw();

                float forward = player.forwardSpeed;
                float strafe  = player.sidewaysSpeed;
                if (forward < 0) forward *= 0.5f;

                double baseSpeed = this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                double speed = baseSpeed * (this.fastGear ? 1.0 : 0.5);
                this.setMovementSpeed((float) speed);

                super.travel(new Vec3d(strafe, 0, forward));
                return;
            }
        }
        super.travel(movementInput);
    }

    // ==============================
    // 🧍‍♀️ Gestione passeggero / interazioni
    // ==============================
    @Override
    public boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().isEmpty() && passenger instanceof PlayerEntity;
    }

    @Override public float getStepHeight() { return 1.1F; }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player.isSneaking() && player.getStackInHand(hand).isEmpty()) {
            if (!this.getWorld().isClient) {
                this.removeAllPassengers();
                if (!player.getAbilities().creativeMode) {
                    ItemStack bike = new ItemStack(ModItems.BICYCLE);
                    if (!player.getInventory().insertStack(bike)) player.dropItem(bike, false);
                }
                this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.8f, 1.1f);
                this.discard();
            }
            return ActionResult.SUCCESS;
        }

        if (!this.getWorld().isClient) {
            if (player.hasVehicle()) player.stopRiding();
            if (this.getPassengerList().isEmpty()) player.startRiding(this, true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public Vec3d getPassengerRidingPos(Entity passenger) {
        // Posizione di fallback base
        return getPassengerBasePos();
    }

    // ==============================
    // ⚙️ Marcia e wheelie
    // ==============================
    public void toggleGear(@Nullable PlayerEntity toggler) {
        this.fastGear = !this.fastGear;
        float pitch = this.fastGear ? 1.3f : 0.7f;

        this.getWorld().playSound(null, this.getBlockPos(),
                SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 1.0f, pitch);

        if (toggler != null)
            toggler.sendMessage(Text.literal(this.fastGear ? "Fast Gear ⚡" : "Slow Gear 🐢"), true);

        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(this.fastGear ? ParticleTypes.FLAME : ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.4, this.getZ(),
                    10, 0.2, 0.1, 0.2, 0.01);
        }
    }

    /** Attiva o disattiva l’impennata (SPACE premuto/rilasciato). */
    public void setWheelie(boolean active) {
        if (this.wheelieActive == active) return;
        this.wheelieActive = active;
        this.wheelieTicks = 0;
        this.wheelieOffset = 0;

        if (active) {
            this.playSound(SoundEvents.BLOCK_PISTON_EXTEND, 0.7f, 1.4f);
            if (this.getWorld() instanceof ServerWorld sw)
                sw.spawnParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(),
                        6, 0.2, 0.0, 0.2, 0.01);
        } else {
            this.playSound(SoundEvents.BLOCK_PISTON_CONTRACT, 0.5f, 1.0f);
            this.setPitch(0);
        }
    }

    public float getWheelieOffset() {
        return this.wheelieOffset;
    }

    // ==============================
    // 🚫 Invulnerabilità / fisica
    // ==============================
    @Override public boolean isPushable() { return false; }
    @Override protected void pushAway(Entity entity) {}
    @Override public boolean isPushedByFluids() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean canBeHitByProjectile() { return false; }
    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) { return false; }
    @Override protected int getBurningDuration() { return 0; }

    // ==============================
    // 💾 NBT
    // ==============================
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("FastGear", this.fastGear);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.fastGear = nbt.getBoolean("FastGear");
    }

    // ==============================
    // 🧩 Spawn helper
    // ==============================
    @Nullable
    public static BicycleEntity spawn(ServerWorld world, BlockPos start, @Nullable PlayerEntity owner) {
        BlockPos pos = start;
        while (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty() && pos.getY() < world.getTopY())
            pos = pos.up();

        BicycleEntity e = ModEntities.BICYCLE.create(world);
        if (e == null) return null;

        e.refreshPositionAndAngles(
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                owner != null ? owner.getYaw() : 0f, 0f
        );
        world.spawnEntity(e);
        return e;
    }
}
