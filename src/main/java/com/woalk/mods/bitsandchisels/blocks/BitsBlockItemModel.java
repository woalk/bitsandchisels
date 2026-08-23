package com.woalk.mods.bitsandchisels.blocks;

import com.woalk.mods.bitsandchisels.utils.BitNbtUtil;
import com.woalk.mods.bitsandchisels.render.BitMeshes;
import com.woalk.mods.bitsandchisels.render.LRUCache;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricLayerRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BitsBlockItemModel implements ItemModel {

    private static final LRUCache<CompoundTag, Mesh> cache = new LRUCache<>(200);

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        final var data = stack.getComponents().get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag beTag = data != null ? data.copyTagWithoutId() : null;
        Mesh mesh = cache.computeIfAbsent(beTag, discard -> {
            CompoundTag tag = beTag;
            if (tag != null) {
                BlockState[][][] bits = new BlockState[16][16][16];
                if (tag.contains(AnimatedBitsBlockEntity.FRAMES_KEY)) {
                    tag = tag.getList(AnimatedBitsBlockEntity.FRAMES_KEY).orElseThrow().getCompound(0).get();
                }
                if (BitNbtUtil.read3DBitArray(tag, bits)) {
                    return BitMeshes.createMesh(bits, null, null, null);
                }
            }
            return null;
        });

        if (mesh != null) {
            ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
            layer.setUsesBlockLight(true);
            QuadEmitter emitter = ((FabricLayerRenderState) layer).emitter();
            mesh.outputTo(emitter);
        }
    }
}
