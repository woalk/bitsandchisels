package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.BitMeshes
import com.woalk.mods.bitsandchisels.BitNbtUtil
import com.woalk.mods.bitsandchisels.AbstractBitsBlockModel
import com.woalk.mods.bitsandchisels.LRUCache
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem.Companion.NBT_BIT_BLOCK_TAGS
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem.Companion.OFFSETS
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.util.RandomSource
import org.joml.Vector3f
import java.util.function.Supplier
import kotlin.math.max

class BitStructureModel: AbstractBitsBlockModel() {
    companion object {
        private val cache = LRUCache<CompoundTag, Pair<Mesh, Int>>(100)
    }

    override fun emitItemQuads(stack: ItemStack, randomSupplier: Supplier<RandomSource?>?, context: RenderContext) {
        val fullTag = stack.components.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return
        val mesh = cache.computeIfAbsent(fullTag) { _ ->
            val builder = RendererAccess.INSTANCE.renderer!!.meshBuilder()
            val emitter = builder.emitter
            val tmp = Vector3f()
            var maxX = 0
            var maxY = 0
            var maxZ = 0
            for ((index, nbtElement) in fullTag.getList(NBT_BIT_BLOCK_TAGS, Tag.TAG_COMPOUND.toInt()).withIndex()) {
                if (index >= OFFSETS.size) break // too many blocks to render
                val tag = nbtElement as CompoundTag
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
            val pos = Vector3f()
            for (i in 0 until 4) {
                it.pos(i, it.copyPos(i, pos).apply { mul(0.75f / mesh.second) })
            }
            true
        }
        if (mesh != null) context.meshConsumer().accept(mesh.first)
        context.popTransform()
    }
}