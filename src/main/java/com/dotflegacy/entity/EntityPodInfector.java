package com.dotflegacy.entity;

import com.dotflegacy.entity.ai.EntityAIPodLeap;
import com.dotflegacy.entity.ai.EntityAIPodPackMove;
import com.dotflegacy.entity.ai.EntityAIPodRideTarget;
import com.dotflegacy.init.ModLootTables;
import com.dotflegacy.init.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketSetPassengers;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateClimber;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.List;

/**
 * 1.12.2 port of the Pod Infector from Dawn of the Flood.
 * <p>
 * Behaviour summary (mirrors the original {@code PodInfectorEntity}):
 * <ul>
 *     <li>Small, fast hostile mob that climbs walls and takes no fall damage.</li>
 *     <li>Moves in packs: a "patrol leader" picks a far-away patrol target and the
 *     pods around it follow; nearby pods also share attack targets.</li>
 *     <li>Leaps at its target when it is 2.5 - 16 blocks away.</li>
 *     <li>When close enough it latches onto the target (rides it), periodically
 *     damages it and finally bursts into a poison cloud.</li>
 *     <li>On death it pops with a sound and particles (no damage) and makes nearby
 *     pods with the same target pop too.</li>
 * </ul>
 */
public class EntityPodInfector extends EntityMob implements IAnimatable {

    private static final DataParameter<Integer> BURROW_TIMER = EntityDataManager.createKey(EntityPodInfector.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> GRABBED_TIMER = EntityDataManager.createKey(EntityPodInfector.class, DataSerializers.VARINT);
    private static final DataParameter<Float> STACK_SCALE = EntityDataManager.createKey(EntityPodInfector.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> IS_LEAPING = EntityDataManager.createKey(EntityPodInfector.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_AGGRESSIVE = EntityDataManager.createKey(EntityPodInfector.class, DataSerializers.BOOLEAN);

    /** Ticks the pod stays latched onto a victim before it bursts (original: config, default 100). */
    public static final int TICKS_BEFORE_EXPLODING = 100;
    /** Length of the burrow/latch animation in ticks. */
    public static final int BURROW_ANIM_TIME = 35;
    /** Interval (ticks) between bites while latched onto a host. */
    public static final int LATCH_DAMAGE_INTERVAL = 20;

    private static final float BASE_WIDTH = 0.7F;
    private static final float BASE_HEIGHT = 0.6F;

    private final AnimationFactory factory = new AnimationFactory(this);

    private boolean playedBiteSound;
    private int leapCooldown;
    @Nullable
    private BlockPos patrolTarget;
    private boolean patrolLeader;
    private boolean patrolling;

    public EntityPodInfector(World world) {
        super(world);
        this.setSize(BASE_WIDTH, BASE_HEIGHT);
        this.stepHeight = 0.5F;
        this.experienceValue = 0;
        // Stay away from fire, as the original does
        this.setPathPriority(net.minecraft.pathfinding.PathNodeType.DANGER_FIRE, 16.0F);
        this.setPathPriority(net.minecraft.pathfinding.PathNodeType.DAMAGE_FIRE, -1.0F);
    }

    // ------------------------------------------------------------------ setup

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(BURROW_TIMER, 0);
        this.dataManager.register(GRABBED_TIMER, 0);
        this.dataManager.register(STACK_SCALE, 0.0F);
        this.dataManager.register(IS_LEAPING, false);
        this.dataManager.register(IS_AGGRESSIVE, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(6.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(2.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(0.5D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2.0D);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(0, new EntityAIAttackMelee(this, 1.2D, false) {
            @Override
            protected double getAttackReachSqr(EntityLivingBase attackTarget) {
                return 1.5D + (double) (attackTarget.width * attackTarget.width);
            }
        });
        this.tasks.addTask(1, new EntityAIPodLeap(this, 0.65F));
        this.tasks.addTask(4, new EntityAIPodPackMove(this, 1.1D, 0.9D));
        this.tasks.addTask(4, new EntityAIWanderAvoidWater(this, 0.8D) {
            @Override
            public boolean shouldExecute() {
                return super.shouldExecute() && EntityPodInfector.this.getAttackTarget() == null;
            }

            @Override
            public boolean shouldContinueExecuting() {
                return super.shouldContinueExecuting() && EntityPodInfector.this.getAttackTarget() == null;
            }
        });
        this.tasks.addTask(6, new EntityAIPodRideTarget(this));
        this.tasks.addTask(8, new EntityAILookIdle(this));

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, EntityPodInfector.class));
        this.targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityLivingBase.class, 10, true, false, EntityPodInfector::isValidTarget));
    }

    /**
     * Target filter: everything living that is not a pod, a hostile mob, a squid, a bat
     * or an armor stand (the original excluded flood forms, fish, squids, bats and stands).
     */
    public static boolean isValidTarget(@Nullable EntityLivingBase living) {
        return living != null
                && !(living instanceof EntityPodInfector)
                && !(living instanceof IMob)
                && !(living instanceof EntitySquid)
                && !(living instanceof EntityBat)
                && !(living instanceof EntityArmorStand);
    }

    @Override
    protected PathNavigate createNavigator(World worldIn) {
        return new PathNavigateClimber(this, worldIn);
    }

    // ------------------------------------------------------------- synced data

    public int getBurrowTimer() {
        return this.dataManager.get(BURROW_TIMER);
    }

    public void setBurrowTimer(int ticks) {
        this.dataManager.set(BURROW_TIMER, ticks);
    }

    public int getGrabbedTimer() {
        return this.dataManager.get(GRABBED_TIMER);
    }

    public void setGrabbedTimer(int ticks) {
        this.dataManager.set(GRABBED_TIMER, ticks);
    }

    public boolean isLeaping() {
        return this.dataManager.get(IS_LEAPING);
    }

    public void setLeaping(boolean leaping) {
        this.dataManager.set(IS_LEAPING, leaping);
    }

    public boolean isAggressive() {
        return this.dataManager.get(IS_AGGRESSIVE);
    }

    public void setAggressive(boolean aggressive) {
        this.dataManager.set(IS_AGGRESSIVE, aggressive);
    }

    public float getStackScale() {
        return this.dataManager.get(STACK_SCALE);
    }

    public void setStackScale(float scale) {
        this.dataManager.set(STACK_SCALE, scale);
    }

    /** Render/hitbox scale: (1 + stackScale) * 0.75, exactly as in the original renderer. */
    public float getRenderScale() {
        return (1.0F + this.getStackScale()) * 0.75F;
    }

    public int getLeapCooldown() {
        return this.leapCooldown;
    }

    public void setLeapCooldown(int leapCooldown) {
        this.leapCooldown = leapCooldown;
    }

    public void randomizeScale(float factor) {
        this.setStackScale(this.getStackScale() + this.rand.nextFloat() / factor);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (STACK_SCALE.equals(key)) {
            float scale = this.getRenderScale();
            this.setSize(BASE_WIDTH * scale, BASE_HEIGHT * scale);
        }
    }

    // ------------------------------------------------------------------- nbt

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("burrowTimer", this.getBurrowTimer());
        compound.setInteger("grabbedTimer", this.getGrabbedTimer());
        compound.setFloat("stackScale", this.getStackScale());
        compound.setBoolean("isLeaping", this.isLeaping());
        compound.setBoolean("patrolLeader", this.patrolLeader);
        compound.setBoolean("patrolling", this.patrolling);
        if (this.patrolTarget != null) {
            compound.setTag("PatrolTarget", NBTUtil.createPosTag(this.patrolTarget));
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setBurrowTimer(compound.getInteger("burrowTimer"));
        this.setGrabbedTimer(compound.getInteger("grabbedTimer"));
        this.setStackScale(compound.getFloat("stackScale"));
        this.setLeaping(compound.getBoolean("isLeaping"));
        this.patrolLeader = compound.getBoolean("patrolLeader");
        this.patrolling = compound.getBoolean("patrolling");
        if (compound.hasKey("PatrolTarget", 10)) {
            this.patrolTarget = NBTUtil.getPosFromTag(compound.getCompoundTag("PatrolTarget"));
        }
    }

    // ------------------------------------------------------------ vanilla hooks

    @Override
    public void fall(float distance, float damageMultiplier) {
        // Pods never take fall damage
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public int getTalkInterval() {
        return 15 + this.rand.nextInt(15);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.POD_INFECTOR_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.POD_INFECTOR_IDLE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.POD_INFECTOR_IDLE;
    }

    @Override
    protected float getSoundVolume() {
        return 1.85F;
    }

    @Override
    protected float getSoundPitch() {
        return 0.95F;
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return ModLootTables.POD_INFECTOR;
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.8F;
    }

    @Override
    public boolean isOnLadder() {
        // PathNavigateClimber + collidedHorizontally == spider-like wall climbing
        return this.collidedHorizontally;
    }

    @Override
    public boolean isPotionApplicable(PotionEffect potioneffectIn) {
        return potioneffectIn.getPotion() != MobEffects.POISON && super.isPotionApplicable(potioneffectIn);
    }

    @Override
    public boolean canDespawn() {
        return !this.patrolling;
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
        if (this.rand.nextInt(25) == 0) {
            this.setPatrolLeader(true);
            this.findPatrolTarget();
        }
        this.randomizeScale(2.85F);
        return super.onInitialSpawn(difficulty, livingdata);
    }

    // --------------------------------------------------------------- ticking

    @Override
    protected void updateAITasks() {
        if (this.getBurrowTimer() > 0) {
            this.setBurrowTimer(this.getBurrowTimer() - 1);
        }
        super.updateAITasks();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.leapCooldown > 0) {
            --this.leapCooldown;
        }
        if (this.world.isRemote) {
            return;
        }

        this.removePotionEffect(MobEffects.POISON);

        EntityLivingBase target = this.getAttackTarget();
        this.setAggressive(target != null);
        if (target != null && !target.isEntityAlive()) {
            this.setAttackTarget(null);
            target = null;
        }

        if (target != null && target.isPassenger(this)) {
            this.tickLatched(target);
        } else {
            this.playedBiteSound = false;
            if (this.getGrabbedTimer() > 0) {
                this.setGrabbedTimer(0);
            }
        }

        if (this.isEntityAlive() && target != null) {
            this.chainedWeakBurst(target);
        }

        // Pack behaviour: share the attack target with pods nearby every second
        if (target != null && this.ticksExisted % 20 == 0) {
            this.shareTargetWithPack(target);
        }

        // Slow regeneration when not burning (original: pod_form_regeneration config)
        if (!this.isBurning() && this.rand.nextInt(500) == 0 && this.getHealth() < this.getMaxHealth() - 0.5F) {
            this.heal(0.5F);
        }
    }

    /** Called every tick while riding (latched onto) the current target. */
    private void tickLatched(EntityLivingBase host) {
        this.setGrabbedTimer(this.getGrabbedTimer() + 1);

        if (!this.playedBiteSound) {
            this.world.playSound(null, this.getPosition(), ModSounds.POD_INFECTOR_BITE, SoundCategory.HOSTILE,
                    this.getSoundVolume() + 0.15F, this.getSoundPitch());
            this.playedBiteSound = true;
        }
        if (this.getBurrowTimer() == 0 && this.getGrabbedTimer() == 1) {
            this.setBurrowTimer(BURROW_ANIM_TIME);
        }

        // Periodic bite damage while attached
        if (this.getGrabbedTimer() % LATCH_DAMAGE_INTERVAL == 0) {
            float damage = (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            host.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
            this.heal(0.5F);
            if (host instanceof EntityPlayer) {
                ((EntityPlayer) host).addExhaustion(1.5F);
            }
            if (host.isBurning()) {
                this.setFire(5);
            }
            this.spawnBloodParticles(3);
        }

        if (this.getGrabbedTimer() > TICKS_BEFORE_EXPLODING + this.rand.nextInt(15)) {
            this.burst();
        }
    }

    private void shareTargetWithPack(EntityLivingBase target) {
        if (!isValidTarget(target) && !(target instanceof EntityPlayer)) {
            return;
        }
        List<EntityPodInfector> pack = this.world.getEntitiesWithinAABB(EntityPodInfector.class,
                this.getEntityBoundingBox().grow(12.0D), pod -> pod != this && pod.isEntityAlive() && pod.getAttackTarget() == null);
        for (EntityPodInfector pod : pack) {
            pod.setAttackTarget(target);
        }
    }

    // ------------------------------------------------------------ grab / ride

    public boolean isAttachedToHost() {
        return this.getRidingEntity() instanceof EntityLivingBase;
    }

    public void grabTarget(EntityLivingBase target) {
        if (this.world.isRemote || !target.isEntityAlive()) {
            return;
        }
        if (!this.startRiding(target, true)) {
            return;
        }
        this.getNavigator().clearPath();
        this.setGrabbedTimer(0);
        this.setBurrowTimer(BURROW_ANIM_TIME);
        this.setLeaping(false);
        this.renderYawOffset = target.renderYawOffset;
        this.syncPassengers(target);
    }

    public void detachFromHost() {
        Entity vehicle = this.getRidingEntity();
        this.dismountRidingEntity();
        this.setGrabbedTimer(0);
        if (vehicle != null) {
            this.syncPassengers(vehicle);
        }
    }

    /**
     * The entity tracker never sends passenger packets of an entity to that entity's own
     * client, so a player would not see the pod attached to themselves without this.
     */
    private void syncPassengers(Entity vehicle) {
        if (vehicle instanceof EntityPlayerMP) {
            ((EntityPlayerMP) vehicle).connection.sendPacket(new SPacketSetPassengers(vehicle));
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isAttachedToHost() && !this.world.isRemote && this.rand.nextBoolean()) {
            this.detachFromHost();
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return false;
    }

    @Override
    public double getYOffset() {
        return this.isAttachedToHost() ? -0.1D : super.getYOffset();
    }

    // --------------------------------------------------------------- bursting

    public static DamageSource podPopDamage(EntityPodInfector pod) {
        return new EntityDamageSource("dotflegacy.pod_pop", pod).setExplosion();
    }

    /** Full burst: damages nearby non-pod entities, leaves a poison cloud and kills the pod. */
    private void burst() {
        if (this.world.isRemote) {
            return;
        }
        double attack = this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        List<EntityLivingBase> victims = this.world.getEntitiesWithinAABB(EntityLivingBase.class,
                this.getEntityBoundingBox().grow(3.0D), living -> living != null && !(living instanceof EntityPodInfector));
        for (EntityLivingBase victim : victims) {
            float dist = Math.max(0.5F, this.getDistance(victim));
            double damage = attack * 2.5D * ((float) Math.PI / dist);
            if (victim instanceof EntityPlayer) {
                damage /= 1.75D;
            }
            victim.attackEntityFrom(podPopDamage(this), (float) damage);
        }
        this.playPopSound();
        this.spawnLingeringCloud();
        this.spawnPopParticles(17);
        this.popThis();
    }

    /** Harmless burst used on death: just sound and particles. */
    private void dieBurst() {
        if (this.world.isRemote) {
            return;
        }
        this.playPopSound();
        this.spawnPopParticles(13);
        this.popThis();
    }

    private void weakBurst() {
        this.dieBurst();
    }

    private void chainedBurst(@Nullable EntityLivingBase target) {
        if (target == null) {
            return;
        }
        for (EntityPodInfector pod : this.world.getEntitiesWithinAABB(EntityPodInfector.class, this.getEntityBoundingBox().grow(2.0D))) {
            if (pod != this && pod.isEntityAlive() && pod.getAttackTarget() == target) {
                pod.dieBurst();
            }
        }
    }

    private void chainedWeakBurst(EntityLivingBase target) {
        if (this.getDistance(target) >= 1.9F) {
            return;
        }
        for (EntityPodInfector pod : this.world.getEntitiesWithinAABB(EntityPodInfector.class, this.getEntityBoundingBox().grow(10.0D))) {
            if (pod != this && pod.isEntityAlive() && pod.getAttackTarget() == target && pod.getDistance(target) < 2.5F) {
                pod.weakBurst();
            }
        }
    }

    private void playPopSound() {
        this.world.playSound(null, this.getPosition(), ModSounds.POD_INFECTOR_POP, SoundCategory.HOSTILE,
                this.getSoundVolume(), this.getSoundPitch());
    }

    private void spawnLingeringCloud() {
        EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(this.world, this.posX, this.posY, this.posZ);
        cloud.setRadius(1.5F);
        cloud.setRadiusOnUse(-0.5F);
        cloud.setWaitTime(6);
        cloud.setDuration(cloud.getDuration() / 3 * 6 / 5);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());
        cloud.addEffect(new PotionEffect(MobEffects.POISON, 100, 0));
        this.world.spawnEntity(cloud);
    }

    private void spawnPopParticles(int count) {
        if (this.world instanceof WorldServer) {
            WorldServer server = (WorldServer) this.world;
            server.spawnParticle(EnumParticleTypes.SLIME, this.posX, this.posY + 0.35D, this.posZ, count, 0.3D, 0.2D, 0.3D, 0.05D);
            server.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, this.posX, this.posY + 0.25D, this.posZ, 3, 0.1D, 0.2D, 0.1D, 0.01D);
            server.spawnParticle(EnumParticleTypes.CLOUD, this.posX, this.posY + 0.25D, this.posZ, 4, 0.2D, 0.2D, 0.2D, 0.02D);
        }
    }

    private void spawnBloodParticles(int count) {
        if (this.world instanceof WorldServer) {
            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.CRIT_MAGIC, this.posX, this.posY + 0.25D, this.posZ,
                    count, 0.1D, 0.2D, 0.1D, 0.05D);
        }
    }

    private void popThis() {
        if (!this.world.isRemote) {
            this.dead = true;
            this.setDead();
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        boolean wasDead = this.dead;
        EntityLivingBase target = this.getAttackTarget();
        super.onDeath(cause); // handles loot, experience and the death event
        if (!wasDead && !this.world.isRemote) {
            this.dieBurst();
            this.chainedBurst(target);
        }
    }

    // ------------------------------------------------------------- patrolling

    public void setPatrolTarget(BlockPos pos) {
        this.patrolTarget = pos;
        this.patrolling = true;
    }

    @Nullable
    public BlockPos getPatrolTarget() {
        return this.patrolTarget;
    }

    public boolean hasPatrolTarget() {
        return this.patrolTarget != null;
    }

    public void setPatrolLeader(boolean leader) {
        this.patrolLeader = leader;
        this.patrolling = true;
    }

    public boolean isPatrolLeader() {
        return this.patrolLeader;
    }

    public boolean canJoinPatrol() {
        return true;
    }

    public void findPatrolTarget() {
        this.patrolTarget = this.getPosition().add(-500 + this.rand.nextInt(1000), 0, -500 + this.rand.nextInt(1000));
        this.patrolling = true;
    }

    public boolean isPatrolling() {
        return this.patrolling;
    }

    public void setPatrolling(boolean patrolling) {
        this.patrolling = patrolling;
    }

    // --------------------------------------------------------------- geckolib

    private PlayState animationPredicate(AnimationEvent<EntityPodInfector> event) {
        AnimationController<?> controller = event.getController();
        controller.setAnimationSpeed(1.0D);

        if (this.isAttachedToHost() || this.getBurrowTimer() > 0) {
            controller.setAnimation(new AnimationBuilder().addAnimation("pod.burrow", ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }
        if (this.isLeaping() && !this.onGround) {
            controller.setAnimation(new AnimationBuilder().addAnimation("pod.air", ILoopType.EDefaultLoopTypes.LOOP));
            return PlayState.CONTINUE;
        }
        if (this.isInWater() && !this.onGround) {
            controller.setAnimation(new AnimationBuilder().addAnimation("pod.swim", ILoopType.EDefaultLoopTypes.LOOP));
            return PlayState.CONTINUE;
        }
        if (event.isMoving()) {
            String anim = this.isAggressive() ? "pod.target" : "pod.walk";
            controller.setAnimation(new AnimationBuilder().addAnimation(anim, ILoopType.EDefaultLoopTypes.LOOP));
            return PlayState.CONTINUE;
        }
        controller.setAnimationSpeed(1.2D);
        controller.setAnimation(new AnimationBuilder().addAnimation("pod.idle", ILoopType.EDefaultLoopTypes.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controllerV", 7, this::animationPredicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    // ------------------------------------------------------------------ misc

    /** Entities of the given class with a bounding-box test, helper used by the AI goals. */
    public List<EntityPodInfector> findNearbyPods(double range) {
        AxisAlignedBB box = this.getEntityBoundingBox().grow(range);
        return this.world.getEntitiesWithinAABB(EntityPodInfector.class, box, pod -> pod != null && pod != this && pod.isEntityAlive());
    }
}
