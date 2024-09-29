package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.BitMeshes
import com.woalk.mods.bitsandchisels.BitNbtUtil
import com.woalk.mods.bitsandchisels.BitsBlockModel
import com.woalk.mods.bitsandchisels.LRUCache
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.block.BlockState
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.render.model.json.Transformation
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.util.math.Vec3f
import net.minecraft.util.math.Vec3i
import net.minecraft.util.math.random.Random
import java.util.function.Supplier
import kotlin.math.max

class BitStructureModel: BitsBlockModel() {
    companion object {
        const val NBT_BIT_BLOCK_TAGS = "BitBlockTags"
        const val MAX_DIAMETER = 4
        val OFFSETS = createOffsets(MAX_DIAMETER)
        private val cache = LRUCache<NbtCompound, Pair<Mesh, Int>>(100)

        fun createOffsets(diameter: Int, offset: Int = 0) = Array(diameter * diameter * diameter) { i ->
            Vec3i(i % diameter + offset, (i / diameter) % diameter + offset, (i / diameter) / diameter + offset)
        }
    }

    override fun emitItemQuads(stack: ItemStack, randomSupplier: Supplier<Random?>?, context: RenderContext) {
        val fullTag = stack.nbt ?: return
        val mesh = cache.computeIfAbsent(fullTag) { _ ->
            val builder = RendererAccess.INSTANCE.renderer!!.meshBuilder()
            val emitter = builder.emitter
            val tmp = Vec3f()
            var maxX = 0
            var maxY = 0
            var maxZ = 0
            for ((index, nbtElement) in fullTag.getList(NBT_BIT_BLOCK_TAGS, NbtElement.COMPOUND_TYPE.toInt()).withIndex()) {
                if (index >= OFFSETS.size) break // too many blocks to render
                val tag = nbtElement as NbtCompound
                if (tag.isEmpty) continue
                val bits = Array(16) { Array(16) { arrayOfNulls<BlockState>(16) } }
                if (BitNbtUtil.read3DBitArray(tag, bits)) {
                    val offset = OFFSETS[index]
                    BitMeshes.prepareMesh(bits, null, null, emitter, tmp, offset)
                    if (offset.x > maxX) maxX = offset.x
                    if (offset.y > maxY) maxY = offset.y
                    if (offset.z > maxZ) maxZ = offset.z
                }
            }
            return@computeIfAbsent Pair(builder.build(), max(max(maxX, maxY), maxZ))
        }

        context.pushTransform {
            val pos = Vec3f()
            for (i in 0 until 4) {
                it.pos(i, it.copyPos(i, pos).apply { scale(0.75f / mesh.second) })
            }
            true
        }
        if (mesh != null) context.meshConsumer().accept(mesh.first)
    }
}