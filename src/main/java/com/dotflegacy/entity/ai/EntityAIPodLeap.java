package com.dotflegacy.entity.ai;

import com.dotflegacy.entity.EntityPodInfector;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Port of {@code PodInfectorEntity.PodLeapGoal}: when the target is between 2.5 and 16
 * blocks away and the pod is on the ground, hurl the pod towards it.
 */
public class EntityAIPodLeap extends EntityAIBase {

    private final EntityPodInfector mob;
    private final float yd;
    private EntityLivingBase target;

    public EntityAIPodLeap(EntityPodInfector mob, float yd) {
        this.mob = mob;
        this.yd = yd;
        this.setMutexBits(5); // movement + jumping, like EntityAILeapAtTarget
    }

    @Override
    public boolean shouldExecute() {
        this.target = this.mob.getAttackTarget();
        if (this.target == null || this.mob.getLeapCooldown() > 0 || this.mob.isInWater()) {
            return false;
        }
        double distance = this.mob.getDistance(this.target);
        if (distance >= 2.5D && distance < 16.0D) {
            return this.mob.onGround && !this.mob.isRiding();
        }
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.mob.onGround && this.mob.getAttackTarget() != null && !this.mob.isRiding();
    }

    @Override
    public void startExecuting() {
        this.mob.setLeapCooldown(20 + this.mob.getRNG().nextInt(20));
        this.mob.setLeaping(true);
        Vec3d motion = new Vec3d(this.mob.motionX, this.mob.motionY, this.mob.motionZ);
        Vec3d toTarget = new Vec3d(this.target.posX - this.mob.posX, this.target.posY - this.mob.posY, this.target.posZ - this.mob.posZ);
        if (toTarget.lengthSquared() > 1.0E-7D) {
            toTarget = toTarget.normalize().scale(2.0D).add(motion.scale(1.5D));
        }
        this.mob.motionX = toTarget.x + this.yd * 0.65F;
        this.mob.motionY = toTarget.y + this.yd;
        this.mob.motionZ = toTarget.z + this.yd * 0.65F;
        this.mob.isAirBorne = true;
    }

    @Override
    public void updateTask() {
        if (this.target != null) {
            double dx = this.target.posX - this.mob.posX;
            double dz = this.target.posZ - this.mob.posZ;
            this.mob.rotationYaw = -((float) MathHelper.atan2(dx, dz)) * (180F / (float) Math.PI);
            this.mob.renderYawOffset = this.mob.rotationYaw;
            this.mob.setLeaping(true);
        }
    }

    @Override
    public void resetTask() {
        Vec3d pos = RandomPositionGenerator.findRandomTarget(this.mob, 5, 4);
        if (pos != null) {
            this.mob.getNavigator().tryMoveToXYZ(pos.x, pos.y, pos.z, this.yd * 3.0F);
        }
        this.mob.setLeaping(false);
        this.target = null;
    }
}
