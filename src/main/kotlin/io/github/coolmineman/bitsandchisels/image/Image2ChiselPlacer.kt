package io.github.coolmineman.bitsandchisels.image

import io.github.coolmineman.bitsandchisels.api.BitUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

fun ChiseledImage.canPlace(player: PlayerEntity, startingPosInput: BlockPos, direction: Direction): Boolean {
    val startingPos = when (direction) {
        Direction.SOUTH, Direction.WEST, Direction.DOWN -> startingPosInput
        Direction.NORTH -> startingPosInput.add(blockWidth - 1, 0, 0)
        Direction.EAST -> startingPosInput.add(0, 0, blockWidth - 1)
        Direction.UP -> startingPosInput.add(blockWidth - 1, 0, 0)
    }
    for (blockXOffset in 0 until blockWidth) {
        for (blockYOffset in 0 until blockHeight) {
            val offsetPos = when (direction) {
                Direction.SOUTH -> startingPos.add(blockXOffset, blockYOffset, 0)
                Direction.NORTH -> startingPos.add(-blockXOffset, blockYOffset, 0)
                Direction.WEST  -> startingPos.add(0, blockYOffset, blockXOffset)
                Direction.EAST  -> startingPos.add(0, blockYOffset, -blockXOffset)
                Direction.DOWN  -> startingPos.add(blockXOffset, 0, blockYOffset)
                Direction.UP    -> startingPos.add(-blockXOffset, 0, blockYOffset)
            }
            if (!player.world.canSetBlock(offsetPos)) return false
        }
    }
    return true
}

fun ChiseledImage.place(
    world: World,
    startingPosInput: BlockPos,
    zRange: IntRange,
    direction: Direction,
    palette: PaletteIdentifier,
    setAir: Boolean
) {
    val startingPos = when (direction) {
        Direction.SOUTH, Direction.WEST, Direction.DOWN -> startingPosInput
        Direction.NORTH -> startingPosInput.add(blockWidth - 1, 0, 0)
        Direction.EAST -> startingPosInput.add(0, 0, blockWidth - 1)
        Direction.UP -> startingPosInput.add(blockWidth - 1, 0, 0)
    }
    for (blockYOffset in 0 until blockHeight) {
        for (blockXOffset in 0 until blockWidth) {
            val offsetPos = when (direction) {
                Direction.SOUTH -> startingPos.add(blockXOffset, blockYOffset, 0)
                Direction.NORTH -> startingPos.add(-blockXOffset, blockYOffset, 0)
                Direction.WEST  -> startingPos.add(0, blockYOffset, blockXOffset)
                Direction.EAST  -> startingPos.add(0, blockYOffset, -blockXOffset)
                Direction.DOWN  -> startingPos.add(blockXOffset, 0, blockYOffset)
                Direction.UP    -> startingPos.add(-blockXOffset, 0, blockYOffset)
            }
            placeInBlock(
                world,
                offsetPos,
                blockXOffset * 16,
                blockYOffset * 16,
                zRange,
                direction,
                palette,
                setAir
            )
        }
    }
}

private fun ChiseledImage.placeInBlock(
    world: World,
    blockPos: BlockPos,
    xOffset: Int,
    yOffset: Int,
    zRange: IntRange,
    direction: Direction,
    palette: PaletteIdentifier,
    setAir: Boolean
) {
    for (z in zRange) {
        for (y in 0 until minOf(16, height - yOffset)) {
            for (x in 0 until minOf(16, width - xOffset)) {
                val block = getBlockForPixel(x + xOffset, y + yOffset, palette).defaultState
                if (!setAir && block.isAir) {
                    continue
                }
                val (bx, by, bz) = when(direction) {
                    Direction.SOUTH -> Triple(x, y, z)
                    Direction.NORTH -> Triple(15 - x, y, 15 - z)
                    Direction.WEST -> Triple(z, y, x)
                    Direction.EAST -> Triple(15 - z, y, 15 - x)
                    Direction.DOWN -> Triple(x, z, y)
                    Direction.UP -> Triple(15 - x, 15 - z, y)
                }
                BitUtils.setBit(
                    world,
                    blockPos,
                    bx, by, bz,
                    block
                )
            }
        }
    }
    BitUtils.update(world, blockPos)
}
