package com.woalk.mods.bitsandchisels.blocks;

import java.util.ArrayList;
import java.util.List;

import com.woalk.mods.bitsandchisels.utils.BitNbtUtil;
import com.woalk.mods.bitsandchisels.utils.ColorPolice;
import com.woalk.mods.bitsandchisels.render.LRUCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.jetbrains.annotations.Nullable;

public class BitItemModel implements ItemModel {

    private static final LRUCache<CompoundTag, BlockState> cache = new LRUCache<>(200);

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        final var data = stack.getComponents().get(DataComponents.CUSTOM_DATA);
        final var bitNbt = data != null ? data.copyTag().getCompound("bit").orElse(null) : null;
        if (bitNbt == null) return;

        BlockState blockState = cache.computeIfAbsent(bitNbt, BitNbtUtil::toBlockState);
        if (blockState == null) return;

        BlockState renderBlockState = blockState instanceof ColoredBlockState cbs ? cbs.getWrapped() : blockState;

        // Get the block model's quads
        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        BlockStateModel model = modelSet.get(renderBlockState);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(new LegacyRandomSource(seed), parts);

        ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
        List<BakedQuad> quadList = layer.prepareQuadList();

        for (BlockStateModelPart part : parts) {
            for (Direction d : Direction.values()) {
                List<BakedQuad> dirQuads = part.getQuads(d);
                if (dirQuads != null) {
                    for (BakedQuad quad : dirQuads) {
                        // Apply tinting
                        if (blockState instanceof ColoredBlockState cbs) {
                            int color = 0xFF000000 | cbs.getColor();
                            // We can't modify BakedQuad colors directly here, but the tint layer system handles it
                            quadList.add(quad);
                        } else {
                            quadList.add(quad);
                        }
                    }
                }
            }
            // Also add null-face quads (unculled)
            List<BakedQuad> nullQuads = part.getQuads(null);
            if (nullQuads != null) {
                quadList.addAll(nullQuads);
            }
        }

        // Apply color tinting via tint layers
        if (blockState instanceof ColoredBlockState cbs) {
            int color = 0xFF000000 | cbs.getColor();
            layer.tintLayers().add(color);
        } else {
            // Use the block's color provider tint
            int color = 0xFF000000 | ColorPolice.getBlockColor(blockState, null, null, 0);
            layer.tintLayers().add(color);
        }

        layer.setParticleMaterial(model.particleMaterial());
        layer.setUsesBlockLight(true);
    }
}
