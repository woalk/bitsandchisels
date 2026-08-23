package com.woalk.mods.bitsandchisels.render

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.util.ARGB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object RedBoxPipeline {
    private val COLOR = ARGB.colorFromFloat(0.4f, 1f, 0f, 0.0f)
    private val COLOR_HC = ARGB.colorFromFloat(1f, 1f, 0f, 0.0f)

    private var registrationNumber = 0
    private var state = MutableList<State?>(5) { null }

    @JvmStatic
    fun registerExtraction(handler: (LevelExtractionContext, HitResult?) -> State?): Int {
        val number = registrationNumber++
        LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register { context, hitResult ->
            state[number] = handler(context, hitResult)
        }
        return number
    }

    object Shape {
        @JvmStatic
        @JvmOverloads
        fun bitPosition(hit: BlockHitResult, placing: Boolean = false): BitVec {
            val direction = hit.direction
            val pos = hit.blockPos
            val x = floor(((hit.getLocation().x() - pos.x) * 16) + (direction.stepX * -0.5)).toInt() + if (placing) direction.stepX else 0
            val y = floor(((hit.getLocation().y() - pos.y) * 16) + (direction.stepY * -0.5)).toInt() + if (placing) direction.stepY else 0
            val z = floor(((hit.getLocation().z() - pos.z) * 16) + (direction.stepZ * -0.5)).toInt() + if (placing) direction.stepZ else 0
            return BitVec(x, y, z, placing)
        }

        private fun bitShape(
            bitX: Double, bitY: Double, bitZ: Double,
            bitX1: Double, bitY1: Double, bitY2: Double,
        ): VoxelShape {
            val x = bitX * 0.0625
            val y = bitY * 0.0625
            val z = bitZ * 0.0625

            val x1 = bitX1 * 0.0625
            val y1 = bitY1 * 0.0625
            val z1 = bitY2 * 0.0625

            return Shapes.create(x, y, z, x1, y1, z1)
        }

        @JvmStatic
        @JvmOverloads
        fun singleBit(hit: HitResult, placing: Boolean = false): AnchoredShape? = if (hit.type == HitResult.Type.BLOCK) {
            val pos = bitPosition(hit as BlockHitResult, placing)
            val shape = bitShape(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)
            return AnchoredShape(pos.overflow?.let { hit.blockPos.offset(it) } ?: hit.blockPos, shape)
        } else null

        @JvmStatic
        @JvmOverloads
        fun multiBit(hit: HitResult, bitSelection: BitSelection?, placing: Boolean = false): AnchoredShape? {
            if (hit.type == HitResult.Type.BLOCK) {
                var pos = (hit as BlockHitResult).blockPos
                if (bitSelection != null) {
                        if (pos.distSqr(bitSelection.anchor) > 9) {
                            return null
                        }
                        val bitPos = bitPosition(hit, placing)
                        if (placing && bitPos.overflow != null) {
                            pos = pos.offset(bitPos.overflow)
                        }

                        val blockXOffset: Int = pos.x - bitSelection.anchor.x
                        val blockYOffset: Int = pos.y - bitSelection.anchor.y
                        val blockZOffset: Int = pos.z - bitSelection.anchor.z

                        val minx = min(bitSelection.bit.x.toDouble(), blockXOffset * 16.0 + bitPos.x)
                        val miny = min(bitSelection.bit.y.toDouble(), blockYOffset * 16.0 + bitPos.y)
                        val minz = min(bitSelection.bit.z.toDouble(), blockZOffset * 16.0 + bitPos.z)

                        val maxx = max(bitSelection.bit.x.toDouble(), blockXOffset * 16.0 + bitPos.x)
                        val maxy = max(bitSelection.bit.y.toDouble(), blockYOffset * 16.0 + bitPos.y)
                        val maxz = max(bitSelection.bit.z.toDouble(), blockZOffset * 16.0 + bitPos.z)

                        return AnchoredShape(bitSelection.anchor, bitShape(minx, miny, minz, maxx + 1, maxy + 1, maxz + 1))
                } else {
                    return singleBit(hit, placing)
                }
            }
            return null
        }

        data class BitSelection(val anchor: BlockPos, val bit: Vec3i) {
            constructor(anchor: BlockPos, bitX: Int, bitY: Int, bitZ: Int) : this(anchor, Vec3i(bitX, bitY, bitZ))
        }

        class BitVec(
            x: Int, y: Int, z: Int,
            withOverflow: Boolean = false,
            val overflow: Vec3i? = if (withOverflow && (x > 15 || y > 15 || z > 15 || x < 0 || y < 0 || z < 0)) Vec3i(
                if (x > 15) 1 else if (x < 0) -1 else 0,
                if (y > 15) 1 else if (y < 0) -1 else 0,
                if (z > 15) 1 else if (z < 0) -1 else 0,
            ) else null
        ) : Vec3i(
            if (!withOverflow) x else if (x > 15) x - 16 else if (x < 0) x + 16 else x,
            if (!withOverflow) y else if (y > 15) y - 16 else if (y < 0) y + 16 else y,
            if (!withOverflow) z else if (z > 15) z - 16 else if (z < 0) z + 16 else z,
        )
        data class AnchoredShape(val pos: BlockPos, val shape: VoxelShape)
    }

    private fun render(context: LevelRenderContext, outlineRenderState: BlockOutlineRenderState): Boolean {
        val cameraPos: Vec3 = context.levelState().cameraRenderState.pos
        val lineWidth = context.gameRenderer().gameRenderState().windowRenderState.appropriateLineWidth

        for (s in state) {
            if (s == null) continue

            val pos: BlockPos = s.pos
            context.poseStack().pushPose()
            context.poseStack().translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z)

            context.submitNodeCollector().submitShapeOutline(
                context.poseStack(),
                s.shape,
                RenderTypes.lines(),
                if (outlineRenderState.highContrast) COLOR_HC else COLOR,
                lineWidth,
                true
            )

            context.poseStack().popPose()
        }
        return true
    }

    fun registerClient() {
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(::render)
    }

    data class State(val pos: BlockPos, val shape: VoxelShape) {
        constructor(multiBitResult: Shape.AnchoredShape) : this(multiBitResult.pos, multiBitResult.shape)
    }
}
