package com.woalk.mods.bitsandchisels.duck;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;

public interface CubeRenderStuff {
    BakedQuad[] getQuads(int direction);

    RenderMaterial[] getMaterials(int direction);

    public static CubeRenderStuff of(BlockState state) {
        return (CubeRenderStuff)state;
    }
}
