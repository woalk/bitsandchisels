package com.woalk.mods.bitsandchisels.blocks;

import java.util.List;
import java.util.function.Predicate;

import com.woalk.mods.bitsandchisels.render.LRUCache;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3f;

public abstract class AbstractBitsBlockModel implements BlockStateModel, FabricBlockStateModel {
    private static final Identifier SPRITE_ID = Identifier.fromNamespaceAndPath("bitsandchisels", "block/null");

    protected static final LRUCache<CompoundTag, Mesh> cache = new LRUCache<>(200);

    private static final ThreadLocal<Vector3f> TMP_POS = ThreadLocal.withInitial(Vector3f::new);

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        BlockEntity be = blockView.getBlockEntity(pos);
        if (be instanceof BitsBlockEntity bitsEntity) {
            Object renderData = bitsEntity.getRenderData();
            if (renderData instanceof Mesh mesh) {
                emitMesh(mesh, emitter);
            } else if (renderData instanceof kotlin.Pair) {
                //noinspection unchecked
                kotlin.Pair<Integer, List<Mesh>> pair = (kotlin.Pair<Integer, List<Mesh>>) renderData;
                if (pair.component2().size() > pair.component1()) {
                    emitMesh(pair.component2().get(pair.component1()), emitter);
                }
            }
        }
    }

    /**
     * Emits mesh quads with cleared cull faces and a microscopic vertex offset
     * to prevent optimization mods like Sodium from aggressively culling partial
     * block faces or causing pipeline crashes when block face culling is enabled.
     */
    private void emitMesh(Mesh mesh, QuadEmitter emitter) {
        Vector3f tmp = TMP_POS.get();
        mesh.forEach(quad -> {
            emitter.copyFrom(quad);
            emitter.cullFace(null);
            quad.copyPos(0, tmp);
            tmp.add(0.0001f, 0.0001f, 0.0001f);
            emitter.pos(0, tmp);
            emitter.emit();
        });
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
