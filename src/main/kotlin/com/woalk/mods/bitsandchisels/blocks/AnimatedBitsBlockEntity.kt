package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.BitMeshes
import com.woalk.mods.bitsandchisels.BitNbtUtil
import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlockEntity
import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.BlockPos
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

    override fun writeBitNbt(tag: NbtCompound) {
        if (nbtCache == null) {
            BitsAndChisels.LOGGER.error("NbtCache should not be null!")
            writeFrames(tag)
        } else tag.copyFrom(nbtCache)
        tag.putInt(FRAME_TIME_KEY, frametime)
    }

    override fun writeNbt(tag: NbtCompound) {
        super.writeNbt(tag)
        tag.putInt(FRAME_TIME_KEY, frametime)
    }

    override fun rebuildNbtCache() {
        val c = NbtCompound()
        writeFrames(c)
        nbtCache = c
    }

    private fun writeFrames(tag: NbtCompound) {
        val list = NbtList()
        for (states in frames) {
            val frameTag = NbtCompound()
            BitNbtUtil.write3DBitArray(frameTag, states)
            list.add(frameTag)
        }
        tag.put(FRAMES_KEY, list)
    }

    override fun readNbt(tag: NbtCompound) {
        super.readNbt(tag)
        frametime = tag.getInt(FRAME_TIME_KEY).coerceAtLeast(1)
    }

    override fun readBitNbt(tag: NbtCompound) {
        val list = tag.getList(FRAMES_KEY, NbtElement.COMPOUND_TYPE.toInt())
        frames.clear()
        val air = Blocks.AIR.defaultState
        for (frameTag in list) {
            val states = Array(16) { Array(16) { Array(16) { air } } }
            BitNbtUtil.read3DBitArray(frameTag as NbtCompound, states)
            frames.add(states)
        }
        frames.firstOrNull()?.let { states = it }
    }

    override fun rebuildMesh() {
        meshes.clear()
        for (states in frames) {
            meshes.add(BitMeshes.createMesh(states, world, pos))
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
