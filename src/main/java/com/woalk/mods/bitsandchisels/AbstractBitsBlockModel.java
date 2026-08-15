package com.woalk.mods.bitsandchisels;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;

import com.woalk.mods.bitsandchisels.blocks.AnimatedBitsBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;

public abstract class AbstractBitsBlockModel implements UnbakedModel, BakedModel, FabricBakedModel {
    public static final Material SPRITE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("bitsandchisels", "block/null"));
    static boolean frapiwarn = false;

    // *Important Stuff

    protected static final LRUCache<CompoundTag, Mesh> cache = new LRUCache<>(200); //Should be enough (tm)

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        Object mesh = ((RenderAttachedBlockView) blockView).getBlockEntityRenderAttachment(pos);
        if (mesh instanceof Mesh) context.meshConsumer().accept((Mesh) mesh);
        else if (mesh instanceof kotlin.Pair) {
            //noinspection unchecked
            kotlin.Pair<Integer, List<Mesh>> pair = (kotlin.Pair<Integer, List<Mesh>>) mesh;
            if (pair.component2().size() > pair.component1()) {
                context.meshConsumer().accept(pair.component2().get(pair.component1()));
            }
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        Mesh mesh = cache.computeIfAbsent(beTag, discard -> {
            CompoundTag tag = beTag;
            if (tag != null) {
                BlockState[][][] bits = new BlockState[16][16][16];
                if (tag.contains(AnimatedBitsBlockEntity.FRAMES_KEY)) {
                    tag = tag.getList(AnimatedBitsBlockEntity.FRAMES_KEY, Tag.TAG_COMPOUND).getCompound(0);
                }
                if (BitNbtUtil.read3DBitArray(tag, bits)) {
                    return BitMeshes.createMesh(bits, null, null, null);
                }
            }
            return null;
        });

        if (mesh != null) context.meshConsumer().accept(mesh);
    }

    //*Stubs

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> unbakedModelGetter,
                                             Set<Pair<String, String>> unresolvedTextureReferences) {
        return Collections.singletonList(SPRITE);
    }

    @Override
    public BakedModel bake(ModelBakery loader, Function<Material, TextureAtlasSprite> textureGetter,
                           ModelState rotationContainer, ResourceLocation modelId) {
        return this;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        if (!frapiwarn) {
            BitsAndChisels.LOGGER.warn("BakedModel.getQuads was just called, this likely means your renderer doesn't support Fabric rendering API. In that case get one that does, I recommend canvas or indigo.");
            frapiwarn = true;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return SPRITE.sprite();
    }

    @Override
    public ItemTransforms getTransforms() {
        return ModelHelper.MODEL_TRANSFORM_BLOCK;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }
}
