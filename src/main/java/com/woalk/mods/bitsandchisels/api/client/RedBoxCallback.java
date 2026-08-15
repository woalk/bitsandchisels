package com.woalk.mods.bitsandchisels.api.client;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;

@SuppressWarnings("all")
public interface RedBoxCallback {
    Event<RedBoxCallback> EVENT = EventFactory.createArrayBacked(RedBoxCallback.class,
        listeners -> (redBoxDrawer, matrixStack, vertexConsumer, worldoffsetx, worldoffsety, worldoffsetz) -> {
            for (RedBoxCallback listener : listeners) {
                listener.drawBoxes(redBoxDrawer, matrixStack, vertexConsumer, worldoffsetx, worldoffsety, worldoffsetz);
            }
        }
    );

    public void drawBoxes(IRedBoxDrawer redBoxDrawer, PoseStack matrixStack, VertexConsumer vertexConsumer, double worldoffsetx, double worldoffsety, double worldoffsetz);
    
    public static interface IRedBoxDrawer {
        public void drawRedBox(PoseStack matrixStack, VertexConsumer vertexConsumer, BlockPos block, int bitx, int bity, int bitz, int bitx2, int bity2, int bitz2, double worldoffsetx, double worldoffsety, double worldoffsetz);
    }
}
