package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.BitMeshes
import com.woalk.mods.bitsandchisels.BitNbtUtil
import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlockEntity
import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.nbt.ListTag
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import java.util.*

class AnimatedBitsBlockEntity(pos: BlockPos, val state: BlockState) : BitsBlockEntity(
    ModBlockEntities.animatedBitsBlockEntity, pos, state
) {
    companion object {
        const val FRAMES_KEY = "frames"
        const val FRAME_TIME_KEY = "frametime"
    }

    var frametime: Int = 1
    private val frames: MutableList<Array<Array<Array<BlockState>>>> = LinkedList()
    private val meshes: MutableList<Mesh> = LinkedList()

    private var frameCounter = 0

    override fun writeBitNbt(tag: CompoundTag) {
        if (nbtCache == null) {
            BitsAndChisels.LOGGER.error("NbtCache should not be null!")
            writeFrames(tag)
        } else tag.merge(nbtCache)
        tag.putInt(FRAME_TIME_KEY, frametime)
    }

    override fun saveAdditional(tag: CompoundTag, provider: HolderLookup.Provider) {
        super.saveAdditional(tag, provider)
        tag.putInt(FRAME_TIME_KEY, frametime)
    }

    override fun rebuildNbtCache() {
        val c = CompoundTag()
        writeFrames(c)
        nbtCache = c
    }

    private fun writeFrames(tag: CompoundTag) {
        val list = ListTag()
        for (states in frames) {
            val frameTag = CompoundTag()
            BitNbtUtil.write3DBitArray(frameTag, states)
            list.add(frameTag)
        }
        tag.put(FRAMES_KEY, list)
    }

    override fun loadAdditional(tag: CompoundTag, provider: HolderLookup.Provider) {
        super.loadAdditional(tag, provider)
        load(tag)
    }

    fun load(tag: CompoundTag) {
        frametime = tag.getInt(FRAME_TIME_KEY).coerceAtLeast(1)
    }

    override fun readBitNbt(tag: CompoundTag) {
        val list = tag.getList(FRAMES_KEY, Tag.TAG_COMPOUND.toInt())
        frames.clear()
        val air = Blocks.AIR.defaultBlockState()
        for (frameTag in list) {
            val states = Array(16) { Array(16) { Array(16) { air } } }
            BitNbtUtil.read3DBitArray(frameTag as CompoundTag, states)
            frames.add(states)
        }
        frames.firstOrNull()?.let { states = it }
    }

    override fun rebuildMesh() {
        meshes.clear()
        for (states in frames) {
            meshes.add(BitMeshes.createMesh(states, level, worldPosition, null))
        }
        mesh = meshes.firstOrNull()
    }

    fun tick() {
        frameCounter++
        if (frameCounter >= frames.size * frametime) {
            frameCounter = 0
        }
    }

    override fun getRenderAttachmentData() = Pair(frameCounter / frametime, meshes)
}
