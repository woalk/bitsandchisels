package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.registry.ModBlocks
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack

class AnimatedBitsBlockEntityRenderer(private val ctx: BlockEntityRendererFactory.Context): BlockEntityRenderer<AnimatedBitsBlockEntity> {
    private val model = ctx.renderManager.getModel(ModBlocks.animatedBitsBlock.defaultState)

    override fun render(
        entity: AnimatedBitsBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        // requires Indium renderer to look pretty
        ctx.renderManager.modelRenderer.render(
            entity.world,
            model,
            entity.state,
            entity.pos,
            matrices,
            vertexConsumers.getBuffer(RenderLayers.getBlockLayer(entity.state)),
            false,
            entity.world?.random,
            0,
            overlay,
        )
    }
}