package com.woalk.mods.bitsandchisels.mixin;

import com.woalk.mods.bitsandchisels.api.client.RedBoxCallback;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.shapes.Shapes;
import org.spongepowered.asm.mixin.Final;
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
public abstract class RedSelectionBoxMixin implements RedBoxCallback.IRedBoxDrawer {
    @Final
    @Shadow
    private GameRenderer gameRenderer;

    @Inject(at = @At("TAIL"), method = "submitBlockOutline")
    private void drawBlockOutlineAddon(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        RedBoxCallback.EVENT.invoker().drawBoxes(this, poseStack, submitNodeCollector, 0, 0, 0);
    }

    public void drawRedBox(PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, BlockPos block, int bitx, int bity, int bitz, int bitx2, int bity2, int bitz2, double worldoffsetx, double worldoffsety, double worldoffsetz) {
        double xoffset = block.getX() - worldoffsetx;
        double yoffset = block.getY() - worldoffsety;
        double zoffset = block.getZ() - worldoffsetz;

        double x = ((double)bitx) * 0.0625d;
        double y = ((double)bity) * 0.0625d;
        double z = ((double)bitz) * 0.0625d;

        double x1 = ((double)bitx2) * 0.0625d;
        double y1 = ((double)bity2) * 0.0625d;
        double z1 = ((double)bitz2) * 0.0625d;

        final var shape = Shapes.create(x, y, z, x1, y1, z1);
        submitNodeCollector.submitShapeOutline(matrixStack, shape, RenderTypes.lines(), ARGB.colorFromFloat(1f, 0f, 0f, 0.4f), gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth, true);
    }
}
