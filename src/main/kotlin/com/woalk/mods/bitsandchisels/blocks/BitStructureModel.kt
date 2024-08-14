package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.BitMeshes
import com.woalk.mods.bitsandchisels.BitNbtUtil
import com.woalk.mods.bitsandchisels.BitsBlockModel
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.util.math.Vec3f
import net.minecraft.util.math.Vec3i
import net.minecraft.util.math.random.Random
import java.util.function.Supplier

class BitStructureModel: BitsBlockModel() {
    companion object {
        const val NBT_BIT_BLOCK_TAGS = "BitBlockTags"
        private const val MAX_DIAMETER = 4
        val OFFSETS = Array(MAX_DIAMETER * MAX_DIAMETER * MAX_DIAMETER) { i ->
            Vec3i(i % MAX_DIAMETER, (i / MAX_DIAMETER) % MAX_DIAMETER, (i / MAX_DIAMETER) / MAX_DIAMETER)
        }
    }

    override fun emitItemQuads(stack: ItemStack, randomSupplier: Supplier<Random?>?, context: RenderContext) {
        val fullTag = stack.nbt ?: return
        val mesh = cache.computeIfAbsent(fullTag) { _ ->
            val builder = RendererAccess.INSTANCE.renderer!!.meshBuilder()
            val emitter = builder.emitter
            val tmp = Vec3f()
            for ((index, nbtElement) in fullTag.getList(NBT_BIT_BLOCK_TAGS, NbtElement.COMPOUND_TYPE.toInt()).withIndex()) {
                if (index >= OFFSETS.size) break // too many blocks to render
                val tag = nbtElement as NbtCompound
                if (tag.isEmpty) continue
                val bits = Array(16) { Array(16) { arrayOfNulls<BlockState>(16) } }
                if (BitNbtUtil.read3DBitArray(tag, bits)) {
                    BitMeshes.prepareMesh(bits, null, null, emitter, tmp, OFFSETS[index])
                }
            }
            return@computeIfAbsent builder.build()
        }

        if (mesh != null) context.meshConsumer().accept(mesh)
    }}