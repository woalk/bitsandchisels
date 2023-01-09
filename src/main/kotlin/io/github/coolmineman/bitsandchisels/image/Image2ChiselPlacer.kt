package io.github.coolmineman.bitsandchisels.image

import io.github.coolmineman.bitsandchisels.api.BitUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

fun ChiseledImage.canPlace(player: PlayerEntity, startingPos: BlockPos, direction: Direction): Boolean {
    for (x in 0 until blockWidth) {
        for (y in 0 until blockHeight) {
            if (!player.world.canSetBlock(startingPos.add(x, y, 0))) return false
        }
    }
    return true
}

fun ChiseledImage.place(
    world: World,
    startingPos: BlockPos,
    zRange: IntRange,
    direction: Direction,
    palette: PaletteIdentifier
) {
    for (blockXOffset in 0 until blockWidth) {
        for (blockYOffset in 0 until blockHeight) {
            placeInBlock(
                world,
                startingPos.add(blockXOffset, blockYOffset, 0),
                blockXOffset * 16,
                blockYOffset * 16,
                zRange,
                direction,
                palette
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
    palette: PaletteIdentifier
) {
    for (z in zRange) {
        for (x in 0 until 16) {
            for (y in 0 until 16) {
                BitUtils.setBit(
                    world,
                    blockPos,
                    x, y, z,
                    getBlockForPixel(x + xOffset, y + yOffset, palette).defaultState
                )
            }
        }
    }
    BitUtils.update(world, blockPos)
}
