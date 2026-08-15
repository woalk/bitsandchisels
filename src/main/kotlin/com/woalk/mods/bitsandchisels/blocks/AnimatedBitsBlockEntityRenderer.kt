package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.registry.ModBlocks
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack

class AnimatedBitsBlockEntityRenderer(private val ctx: BlockEntityRendererProvider.Context): BlockEntityRenderer<AnimatedBitsBlockEntity> {
    private val model = ctx.blockRenderDispatcher.getBlockModel(ModBlocks.animatedBitsBlock.defaultBlockState())

    override fun render(
        entity: AnimatedBitsBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        // requires Indium renderer to look pretty
        ctx.blockRenderDispatcher.modelRenderer.tesselateBlock(
            entity.level,
            model,
            entity.state,
            entity.blockPos,
            matrices,
            vertexConsumers.getBuffer(ItemBlockRenderTypes.getChunkRenderType(entity.state)),
            false,
            entity.level?.random,
            0,
            overlay,
        )
    }
}