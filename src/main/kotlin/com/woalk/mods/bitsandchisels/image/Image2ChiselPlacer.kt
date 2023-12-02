package com.woalk.mods.bitsandchisels.image

import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.BitsBlockEntity
import com.woalk.mods.bitsandchisels.api.BitUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.*

fun IChiseledImage.canPlace(player: PlayerEntity, startingPosInput: BlockPos, direction: Direction): Boolean {
    val startingPos = when (direction) {
        Direction.NORTH, Direction.WEST, Direction.DOWN, Direction.SOUTH -> startingPosInput
        Direction.EAST -> startingPosInput.add(0, 0, 1 - blockWidth)
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

fun IChiseledImage.place(
    world: World,
    startingPosInput: BlockPos,
    zRange: IntRange,
    direction: Direction,
    palette: PaletteIdentifier,
    setAir: Boolean
) {
    val startingPos = when (direction) {
        Direction.NORTH, Direction.WEST, Direction.DOWN, Direction.SOUTH -> startingPosInput
        Direction.EAST -> startingPosInput.add(0, 0, 1 - blockWidth)
        Direction.UP -> startingPosInput.add(blockWidth - 1, 0, 0)
    }
    for (blockYOffset in 0 until blockHeight) {
        for (blockXOffset in 0 until blockWidth) {
            val offsetPos = when (direction) {
                Direction.SOUTH -> startingPos.add(blockXOffset, blockYOffset, 0)
                Direction.NORTH -> startingPos.add(-blockXOffset, blockYOffset, 0)
                Direction.WEST  -> startingPos.add(0, blockYOffset, blockXOffset)
                Direction.EAST  -> startingPos.add(0, blockYOffset, blockXOffset)
                Direction.DOWN  -> startingPos.add(blockXOffset, 0, blockYOffset)
                Direction.UP    -> startingPos.add(-blockXOffset, 0, blockYOffset)
            }
            placeInBlock(
                world,
                offsetPos,
                if (direction == Direction.EAST) { blockWidth - blockXOffset - 1 } else { blockXOffset } * 16,
                blockYOffset * 16,
                zRange,
                direction,
                palette,
                setAir
            )
        }
    }
}

private fun IChiseledImage.placeInBlock(
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
                val block = getStateForPixel(x + xOffset, y + yOffset, palette)
                if (!setAir && block.isAir) {
                    continue
                }
                val (bx, by, bz) = when(direction) {
                    Direction.NORTH -> Triple(15 - x, y, z)
                    Direction.SOUTH -> Triple(x, y, 15 - z)
                    Direction.WEST -> Triple(z, y, x)
                    Direction.EAST -> Triple(15 - z, y, 15 - x)
                    Direction.DOWN -> Triple(x, z, y)
                    Direction.UP -> Triple(15 - x, 15 - z, y)
                }
                BitUtils.setBit(
                    world,
                    blockPos,
                    bx, by, bz,
                    block,
                    palette.bitBlockState
                )
            }
        }
    }
    if (palette.blockWildcard.contains("glass")) {
        world.setBlockState(blockPos, world.getBlockState(blockPos).with(BitsBlock.SHADER_TYPE, 3))
    }
    getImageName()?.let { imageName ->
        val byIndex = imageName.indexOf(" by ")
        (world.getBlockEntity(blockPos) as? BitsBlockEntity)?.apply {
            val loreText = LinkedList<Text>()
            if (byIndex > -1) {
                val name = imageName.substring(0, byIndex).ifEmpty { "Chiseled Image Block" }
                val author = imageName.substring(byIndex + 4)
                customName = Text.literal(name)
                loreText.add(Text.literal("Created by $author"))
            } else {
                customName = Text.literal(imageName).styled {
                    style -> style.withItalic(false)
                }
            }
            loreText.add(Text.literal("(${xOffset / 16}, ${yOffset / 16})").formatted(Formatting.DARK_GRAY))
            setLoreText(loreText)
        }
    }
    BitUtils.update(world, blockPos)
}
