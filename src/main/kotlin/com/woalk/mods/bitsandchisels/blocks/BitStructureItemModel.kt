package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.render.BitMeshes
import com.woalk.mods.bitsandchisels.utils.BitNbtUtil
import com.woalk.mods.bitsandchisels.render.LRUCache
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem.Companion.NBT_BIT_BLOCK_TAGS
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem.Companion.OFFSETS
import net.fabricmc.fabric.api.client.renderer.v1.Renderer
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricLayerRenderState
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.ItemModel
import net.minecraft.client.renderer.item.ItemModelResolver
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.entity.ItemOwner
import net.minecraft.nbt.CompoundTag
import org.joml.Vector3f
import kotlin.math.max

class BitStructureItemModel: ItemModel {
    companion object {
        private val cache = LRUCache<CompoundTag, Pair<Mesh, Int>>(100)
    }

    override fun update(
        renderState: ItemStackRenderState,
        stack: ItemStack,
        resolver: ItemModelResolver,
        displayContext: ItemDisplayContext,
        level: ClientLevel?,
        owner: ItemOwner?,
        seed: Int
    ) {
        val fullTag = stack.components.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return
        val mesh = cache.computeIfAbsent(fullTag) { _ ->
            val builder = Renderer.get()!!.mutableMesh()
            builder.clear()
            val emitter = builder.emitter()
            val tmp = Vector3f()
            var maxX = 0
            var maxY = 0
            var maxZ = 0
            for ((index, nbtElement) in fullTag.getListOrEmpty(NBT_BIT_BLOCK_TAGS).withIndex()) {
                if (index >= OFFSETS.size) break // too many blocks to render
                val tag = nbtElement.asCompound().orElse(null) ?: continue
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
            return@computeIfAbsent Pair(builder.immutableCopy(), max(max(maxX, maxY), maxZ))
        }

        if (mesh != null) {
            val layer = renderState.newLayer()
            layer.setUsesBlockLight(true)
            val layerEmitter = (layer as FabricLayerRenderState).emitter()

            // Scale transform: output each quad vertex position scaled by 0.75/mesh.second
            val scaleFactor = 0.75f / mesh.second
            val pos = Vector3f()
            mesh.first.forEach { quad ->
                layerEmitter.copyFrom(quad)
                for (i in 0 until 4) {
                    layerEmitter.copyPos(i, pos)
                    pos.mul(scaleFactor)
                    layerEmitter.pos(i, pos)
                }
                layerEmitter.emit()
            }
        }
    }
}
