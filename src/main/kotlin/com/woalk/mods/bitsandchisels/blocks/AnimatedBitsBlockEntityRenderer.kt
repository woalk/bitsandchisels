package com.woalk.mods.bitsandchisels.blocks

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricOrderedSubmitNodeCollector
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.chunk.ChunkSectionLayer
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.world.phys.Vec3
import java.util.function.Function

class AnimatedBitsRenderState : BlockEntityRenderState() {
    var currentMesh: Mesh? = null
}

class AnimatedBitsBlockEntityRenderer(private val ctx: BlockEntityRendererProvider.Context): BlockEntityRenderer<AnimatedBitsBlockEntity, AnimatedBitsRenderState> {

    override fun createRenderState(): AnimatedBitsRenderState {
        return AnimatedBitsRenderState()
    }

    override fun extractRenderState(
        entity: AnimatedBitsBlockEntity,
        state: AnimatedBitsRenderState,
        partialTick: Float,
        cameraPos: Vec3,
        crumbling: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(entity, state, crumbling)
        val renderData = entity.renderData
        if (renderData is Pair<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val pair = renderData as Pair<Int, List<Mesh>>
            val frameIndex = pair.first
            val meshes = pair.second
            if (meshes.size > frameIndex) {
                state.currentMesh = meshes[frameIndex]
            } else {
                state.currentMesh = null
            }
        } else {
            state.currentMesh = null
        }
    }

    override fun submit(
        state: AnimatedBitsRenderState,
        matrices: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val mesh = state.currentMesh ?: return
        val layerFunction = Function<ChunkSectionLayer, RenderType> { layer -> RenderTypes.solidMovingBlock() }
        (collector as FabricOrderedSubmitNodeCollector).submitBlockModel(
            matrices,
            layerFunction,
            true,
            emptyList(),
            mesh,
            intArrayOf(),
            state.lightCoords,
            0,
            0
        )
    }
}
