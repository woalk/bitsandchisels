package com.woalk.mods.bitsandchisels.compat

import com.sk89q.jnbt.CompoundTag
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.extent.Extent
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.math.transform.Transform
import com.sk89q.worldedit.world.block.BlockStateHolder
import com.woalk.mods.bitsandchisels.BitsAndChisels
import org.enginehub.linbus.tree.LinCompoundTag

class ChiselExtent(extent: Extent, private val transform: Transform) : AbstractDelegateExtent(extent) {
    override fun <T : BlockStateHolder<T>> setBlock(location: BlockVector3, block: T): Boolean {
        if (block.blockType.id == "${BitsAndChisels.MODID}:bits_block") {
            if (transform is AffineTransform) {
                val baseBlock = block.toBaseBlock()
                val nbt = baseBlock.nbt
                if (nbt != null) {
                    return super.setBlock(location, baseBlock.toBaseBlock(apply(transform, nbt)))
                }
            }
        }
        return super.setBlock(location, block)
    }

    companion object {
        fun apply(transform: Transform, tag: LinCompoundTag): LinCompoundTag {
            val bitsIn = BitCompoundUtil.Block3D()
            val bitsOut = BitCompoundUtil.Block3D()
            BitCompoundUtil.read3DBitArray(tag, bitsIn)
            apply(transform, bitsIn, bitsOut)
            return BitCompoundUtil.write3DBitArray(bitsOut, tag)
        }

        private fun apply(transform: Transform, bitsIn: Block3D, bitsOut: Block3D) {
            for (i in 0..15) {
                for (j in 0..15) {
                    for (k in 0..15) {
                        val target =
                            transform.apply(Vector3.at(i.toDouble(), j.toDouble(), k.toDouble())).toBlockPoint()
                                .add(onlyNegatives(transform.apply(Vector3.at(15.0, 15.0, 15.0))).toBlockPoint())
                        if (target.x() > 15 || target.y() > 15 || target.z() > 15) {
                            continue
                        }
                        val x = if (target.x() < 0) {
                            16 + target.x()
                        } else target.x()
                        val y = if (target.y() < 0) {
                            16 + target.y()
                        } else target.y()
                        val z = if (target.z() < 0) {
                            16 + target.z()
                        } else target.z()
                        bitsOut[x][y][z] = bitsIn[i][j][k]
                    }
                }
            }
        }

        private fun onlyNegatives(vec: Vector3): Vector3 = Vector3.at(
            if (vec.x() < 0) -vec.x() else 0.0,
            if (vec.y() < 0) -vec.y() else 0.0,
            if (vec.z() < 0) -vec.z() else 0.0
        )
    }
}