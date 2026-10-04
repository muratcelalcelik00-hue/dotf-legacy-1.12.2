package com.dotflegacy.entity.ai;

import com.dotflegacy.entity.EntityPodInfector;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

/**
 * Port of {@code PodInfectorEntity.PodRidesTargetGoal}: once the pod is within two
 * blocks of its target it latches onto it by riding it. The actual damage/burst
 * logic lives in {@link EntityPodInfector#onUpdate()}.
 */
public class EntityAIPodRideTarget extends EntityAIBase {

    private final EntityPodInfector pod;

    public EntityAIPodRideTarget(EntityPodInfector pod) {
        this.pod = pod;
    }

    private boolean isValidTarget() {
        EntityLivingBase target = this.pod.getAttackTarget();
        return target != null && target.isEntityAlive() && this.pod.getDistance(target) < 2.0F;
    }

    @Override
    public boolean shouldExecute() {
        return this.pod.getAttackTarget() != null
                && this.pod.isEntityAlive()
                && !this.pod.isRiding()
                && !this.pod.isBeingRidden()
                && this.pod.getRNG().nextInt(7) == 0;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return false;
    }

    @Override
    public void startExecuting() {
        EntityLivingBase target = this.pod.getAttackTarget();
        if (target != null && this.isValidTarget()) {
            this.pod.grabTarget(target);
        }
    }
}
