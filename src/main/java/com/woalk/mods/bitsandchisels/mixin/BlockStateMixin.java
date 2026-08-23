package com.woalk.mods.bitsandchisels.mixin;

import java.util.ArrayList;
import java.util.List;

import com.woalk.mods.bitsandchisels.utils.CubeRenderStuff;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

@Mixin(BlockState.class)
public class BlockStateMixin implements CubeRenderStuff {
    @Unique
    BlockStateModel oldModel = null;
    @Unique
    BakedQuad[][] quads = null;

    @Unique
    private void recomputeIfNeeded() {
        BlockState thiz = (BlockState)(Object)this;
        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        BlockStateModel newModel = modelSet.get(thiz);
        if (oldModel == null || oldModel != newModel) {
            oldModel = newModel;
            quads = new BakedQuad[7][];
            List<BlockStateModelPart> parts = new ArrayList<>();
            newModel.collectParts(new LegacyRandomSource(0), parts);
            for (Direction d : Direction.values()) {
                List<BakedQuad> dQuads = new ArrayList<>();
                for (BlockStateModelPart part : parts) {
                    List<BakedQuad> partQuads = part.getQuads(d);
                    if (partQuads != null) {
                        dQuads.addAll(partQuads);
                    }
                }
                if (dQuads.isEmpty()) {
                    // fallback to missing model
                    BlockStateModel missingModel = modelSet.missingModel();
                    List<BlockStateModelPart> missingParts = new ArrayList<>();
                    missingModel.collectParts(new LegacyRandomSource(0), missingParts);
                    for (BlockStateModelPart part : missingParts) {
                        List<BakedQuad> partQuads = part.getQuads(d);
                        if (partQuads != null) {
                            dQuads.addAll(partQuads);
                        }
                    }
                }
                quads[d.ordinal()] = dQuads.toArray(new BakedQuad[0]);
            }
        }
    }

    @Override
    public BakedQuad[] getQuads(int direction) {
        recomputeIfNeeded();
        return quads[direction];
    }
    
}
