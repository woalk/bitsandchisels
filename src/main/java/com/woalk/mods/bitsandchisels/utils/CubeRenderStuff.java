package com.woalk.mods.bitsandchisels.utils;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;

public interface CubeRenderStuff {
    BakedQuad[] getQuads(int direction);

    public static CubeRenderStuff of(BlockState state) {
        return (CubeRenderStuff)state;
    }
}
