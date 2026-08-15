package com.woalk.mods.bitsandchisels;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;

import com.woalk.mods.bitsandchisels.blocks.ColoredBlockState;
import com.woalk.mods.bitsandchisels.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import net.minecraft.client.resources.model.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class BitItemModel implements UnbakedModel, BakedModel, FabricBakedModel {

    //Copied from fapi

    private static ItemTransform makeTransform(float rotationX, float rotationY, float rotationZ, float translationX, float translationY, float translationZ, float scaleX, float scaleY, float scaleZ) {
		Vector3f translation = new Vector3f(translationX, translationY, translationZ);
		translation.mul(0.0625f);
		translation = new Vector3f(Math.clamp(translation.x, -5.0F, 5.0F), Math.clamp(translation.y, -5.0F, 5.0F), Math.clamp(translation.z, -5.0F, 5.0F));
		return new ItemTransform(new Vector3f(rotationX, rotationY, rotationZ), translation, new Vector3f(scaleX, scaleY, scaleZ));
	}

    public static final ItemTransform TRANSFORM_BLOCK_GUI = makeTransform(30, 225, 0, 0, 0, 0, 0.625f * 0.5f, 0.625f * 0.5f, 0.625f * 0.5f);
	public static final ItemTransform TRANSFORM_BLOCK_GROUND = makeTransform(0, 0, 0, 0, 3, 0, 0.25f * 0.5f, 0.25f * 0.5f, 0.25f * 0.5f);
	public static final ItemTransform TRANSFORM_BLOCK_FIXED = makeTransform(0, 0, 0, 0, 0, 0, 0.5f * 0.5f, 0.5f * 0.5f, 0.5f * 0.5f);
	public static final ItemTransform TRANSFORM_BLOCK_3RD_PERSON_RIGHT = makeTransform(75, 45, 0, 0, 2.5f, 0, 0.375f * 0.5f, 0.375f * 0.5f, 0.375f * 0.5f);
	public static final ItemTransform TRANSFORM_BLOCK_1ST_PERSON_RIGHT = makeTransform(0, 45, 0, 0, 0, 0, 0.4f * 0.5f, 0.4f * 0.5f, 0.4f * 0.5f);
    public static final ItemTransform TRANSFORM_BLOCK_1ST_PERSON_LEFT = makeTransform(0, 225, 0, 0, 0, 0, 0.4f * 0.5f, 0.4f * 0.5f, 0.4f * 0.5f);
    
    private static BlockState transform_state;
    private static final QuadTransform TRANSFORM = quad -> {
        BlockColor colorthing = ColorProviderRegistry.BLOCK.get(transform_state.getBlock());
        if (transform_state instanceof ColoredBlockState) {
            int color = 0xFF000000 | ((ColoredBlockState) transform_state).getColor();
            quad.spriteColor(0, color, color, color, color);
        } else if (colorthing != null) {
            int color = 0xFF000000 | ColorPolice.getBlockColor(transform_state, null, null, quad.colorIndex());
            quad.spriteColor(0, color, color, color, color);
        }
        return true;
    };

	/**
	 * Mimics the vanilla model transformation * 0.5 used for most vanilla blocks,
	 */
    public static final ItemTransforms MODEL_TRANSFORM_BLOCK = new ItemTransforms(TRANSFORM_BLOCK_3RD_PERSON_RIGHT, TRANSFORM_BLOCK_3RD_PERSON_RIGHT, TRANSFORM_BLOCK_1ST_PERSON_LEFT, TRANSFORM_BLOCK_1ST_PERSON_RIGHT, ItemTransform.NO_TRANSFORM, TRANSFORM_BLOCK_GUI, TRANSFORM_BLOCK_GROUND, TRANSFORM_BLOCK_FIXED);
    
    private static LRUCache<CompoundTag, BlockState> cache = new LRUCache<>(200);

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        if (!stack.hasTag() || stack.getTagElement("bit") == null) return;
        transform_state = cache.computeIfAbsent(stack.getTag(), tag -> BitNbtUtil.toBlockState(stack.getTagElement("bit")));
        context.pushTransform(TRANSFORM);
        context.fallbackConsumer().accept(Minecraft.getInstance().getBlockRenderer().getBlockModel(
                transform_state instanceof ColoredBlockState ?
                        ((ColoredBlockState) transform_state).getWrapped() :
                        transform_state));
        context.popTransform();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        BitsAndChisels.LOGGER.warn("BakedModel.getQuads was just called, this likely means your renderer doesn't support Fabric rendering API. In that case get one that does, I recommend canvas or indigo.");
        return Collections.emptyList();
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> function) {
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
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
        return null;
    }

    @Override
    public ItemTransforms getTransforms() {
        return MODEL_TRANSFORM_BLOCK;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable BakedModel bake(ModelBaker modelBaker, Function<Material, TextureAtlasSprite> function, ModelState modelState, ResourceLocation resourceLocation) {
        return this;
    }
    
}
