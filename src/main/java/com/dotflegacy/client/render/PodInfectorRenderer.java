package com.dotflegacy.client.render;

import com.dotflegacy.client.model.PodInfectorModel;
import com.dotflegacy.entity.EntityPodInfector;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

@SideOnly(Side.CLIENT)
public class PodInfectorRenderer extends GeoEntityRenderer<EntityPodInfector> {

    public PodInfectorRenderer(RenderManager renderManager) {
        super(renderManager, new PodInfectorModel());
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntityPodInfector entity, double x, double y, double z, float entityYaw, float partialTicks) {
        float scale = entity.getRenderScale();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.scale(scale, scale, scale);
        super.doRender(entity, 0.0D, 0.0D, 0.0D, entityYaw, partialTicks);
        GlStateManager.popMatrix();
    }
}
