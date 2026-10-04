package com.dotflegacy.entity.ai;

import com.dotflegacy.entity.EntityPodInfector;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

/**
 * Port of {@code PodInfectorEntity.PodInfectorPackMoveGoal} (itself based on the
 * vanilla patrol goal): a patrol leader walks towards a far-away patrol target and
 * hands that target over to all pods within 16 blocks so the pack moves together.
 */
public class EntityAIPodPackMove extends EntityAIBase {

    private final EntityPodInfector mob;
    private final double speedModifier;
    private final double leaderSpeedModifier;
    private long cooldownUntil = -1L;

    public EntityAIPodPackMove(EntityPodInfector mob, double speedModifier, double leaderSpeedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.leaderSpeedModifier = leaderSpeedModifier;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        boolean onCooldown = this.mob.world.getTotalWorldTime() < this.cooldownUntil;
        return this.mob.isPatrolling()
                && this.mob.getAttackTarget() == null
                && !this.mob.isBeingRidden()
                && !this.mob.isRiding()
                && this.mob.hasPatrolTarget()
                && !onCooldown;
    }

    @Override
    public void updateTask() {
        boolean leader = this.mob.isPatrolLeader();
        PathNavigate navigator = this.mob.getNavigator();
        if (!navigator.noPath()) {
            return;
        }
        List<EntityPodInfector> companions = this.findPatrolCompanions();
        BlockPos patrolTarget = this.mob.getPatrolTarget();
        if (this.mob.isPatrolling() && companions.isEmpty()) {
            this.mob.setPatrolling(false);
        } else if (patrolTarget == null) {
            this.mob.findPatrolTarget();
        } else if (leader && patrolTarget.distanceSq(this.mob.posX, this.mob.posY, this.mob.posZ) < 10.0D * 10.0D) {
            this.mob.findPatrolTarget();
        } else {
            Vec3d targetVec = new Vec3d(patrolTarget.getX() + 0.5D, patrolTarget.getY() + 0.5D, patrolTarget.getZ() + 0.5D);
            Vec3d position = this.mob.getPositionVector();
            Vec3d offset = position.subtract(targetVec);
            targetVec = offset.rotateYaw((float) (Math.PI / 2.0D)).scale(0.4D).add(targetVec);
            Vec3d step = targetVec.subtract(position).normalize().scale(10.0D).add(position);
            BlockPos blockpos = this.mob.world.getHeight(new BlockPos(step));
            if (!navigator.tryMoveToXYZ(blockpos.getX(), blockpos.getY(), blockpos.getZ(), leader ? this.leaderSpeedModifier : this.speedModifier)) {
                this.moveRandomly();
                this.cooldownUntil = this.mob.world.getTotalWorldTime() + 200L;
            } else if (leader) {
                for (EntityPodInfector companion : companions) {
                    companion.setPatrolTarget(blockpos);
                }
            }
        }
    }

    private List<EntityPodInfector> findPatrolCompanions() {
        return this.mob.world.getEntitiesWithinAABB(EntityPodInfector.class, this.mob.getEntityBoundingBox().grow(16.0D),
                pod -> pod != null && pod.canJoinPatrol() && !pod.isEntityEqual(this.mob));
    }

    private boolean moveRandomly() {
        Random random = this.mob.getRNG();
        BlockPos blockpos = this.mob.world.getHeight(this.mob.getPosition().add(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));
        return this.mob.getNavigator().tryMoveToXYZ(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speedModifier);
    }
}
