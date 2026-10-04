package com.dotflegacy.client.model;

import com.dotflegacy.Tags;
import com.dotflegacy.entity.EntityPodInfector;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.model.AnimatedGeoModel;

@SideOnly(Side.CLIENT)
public class PodInfectorModel extends AnimatedGeoModel<EntityPodInfector> {

    private static final ResourceLocation MODEL = new ResourceLocation(Tags.MOD_ID, "geo/pod_infector.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/pod_infector.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(Tags.MOD_ID, "animations/pod_infector.animation.json");

    @Override
    public ResourceLocation getModelLocation(EntityPodInfector entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityPodInfector entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntityPodInfector entity) {
        return ANIMATION;
    }
}
