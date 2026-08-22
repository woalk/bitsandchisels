package com.woalk.mods.bitsandchisels;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.woalk.mods.bitsandchisels.blocks.AnimatedBitsBlockEntity;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractBitsBlockModel implements BlockStateModel, FabricBlockStateModel {
    private static final Identifier SPRITE_ID = Identifier.fromNamespaceAndPath("bitsandchisels", "block/null");

    protected static final LRUCache<net.minecraft.nbt.CompoundTag, Mesh> cache = new LRUCache<>(200);

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        BlockEntity be = blockView.getBlockEntity(pos);
        if (be instanceof BitsBlockEntity bitsEntity) {
            Object renderData = bitsEntity.getRenderData();
            if (renderData instanceof Mesh mesh) {
                mesh.outputTo(emitter);
            } else if (renderData instanceof kotlin.Pair) {
                //noinspection unchecked
                kotlin.Pair<Integer, List<Mesh>> pair = (kotlin.Pair<Integer, List<Mesh>>) renderData;
                if (pair.component2().size() > pair.component1()) {
                    pair.component2().get(pair.component1()).outputTo(emitter);
                }
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        // No vanilla parts - we emit quads via FabricBlockStateModel
    }

    @Override
    public Material.Baked particleMaterial() {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(QuadAtlas.BLOCK.getId())
                .getSprite(SPRITE_ID);
        return new Material.Baked(sprite, false);
    }

    @Override
    public int materialFlags() {
        return 0;
    }
}
