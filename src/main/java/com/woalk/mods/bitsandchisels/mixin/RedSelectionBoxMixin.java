package com.woalk.mods.bitsandchisels.mixin;

import com.woalk.mods.bitsandchisels.api.client.RedBoxCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(LevelRenderer.class)
public class RedSelectionBoxMixin implements RedBoxCallback.IRedBoxDrawer {
    @Inject(at = @At("HEAD"), method = "renderHitOutline")
    private void drawBlockOutlineAddon(PoseStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState, CallbackInfo bruh) {
        RedBoxCallback.EVENT.invoker().drawBoxes(this, matrixStack, vertexConsumer, d, e, f);
    }

    public void drawRedBox(PoseStack matrixStack, VertexConsumer vertexConsumer, BlockPos block, int bitx, int bity, int bitz, int bitx2, int bity2, int bitz2, double worldoffsetx, double worldoffsety, double worldoffsetz) {
        double xoffset = block.getX() - worldoffsetx;
        double yoffset = block.getY() - worldoffsety;
        double zoffset = block.getZ() - worldoffsetz;

        double x = ((double)bitx) * 0.0625d;
        double y = ((double)bity) * 0.0625d;
        double z = ((double)bitz) * 0.0625d;

        double x1 = ((double)bitx2) * 0.0625d;
        double y1 = ((double)bity2) * 0.0625d;
        double z1 = ((double)bitz2) * 0.0625d;

        LevelRenderer.renderLineBox(matrixStack, vertexConsumer, x + xoffset, y + yoffset, z + zoffset, x1 + xoffset, y1 + yoffset, z1 + zoffset, 1f, 0f, 0f, 0.4f);
    }
}
