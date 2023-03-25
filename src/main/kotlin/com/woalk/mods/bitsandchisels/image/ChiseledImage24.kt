package com.woalk.mods.bitsandchisels.image

import com.woalk.mods.bitsandchisels.blocks.ColoredBlockState
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.BlockState
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class ChiseledImage24(imageFilePath: String): IChiseledImage {
    companion object {
        private const val IMAGES_DIR = "images"
        private const val MAX_PIXELS = (16 * 16) * (16 * 16) // 256x256

        private val imagesDir = File(FabricLoader.getInstance().gameDir.toFile(), IMAGES_DIR)
        private val enabled = if (!imagesDir.exists()) {
            imagesDir.mkdir()
        } else if (!imagesDir.isDirectory) {
            System.err.println("'images' is not a directory, image2chisel disabled")
            false
        } else {
            true
        }

        private fun isFileAllowed(name: String): Boolean = !name.contains("../") && name.endsWith(".png")
    }

    private val image: BufferedImage

    init {
        if (!enabled) {
            throw IllegalStateException("image2chisel is disabled")
        }
        if (!isFileAllowed(imageFilePath)) {
            throw IOException("Reading this file was prevented for security reasons")
        }
        image = ImageIO.read(File(imagesDir, imageFilePath))
        if (image.width * image.height > MAX_PIXELS) {
            throw IllegalArgumentException("Image is too large, maximum is ${MAX_PIXELS}px")
        }
    }

    override val width by image::width
    override val height by image::height

    override val blockWidth = width.ceilDiv(16)
    override val blockHeight = height.ceilDiv(16)

    fun getColorForPixel(x: Int, y: Int): Int {
        return image.getRGB(width - x - 1, height - y - 1)
    }

    override fun getStateForPixel(x: Int, y: Int, palette: PaletteIdentifier?): BlockState =
        ColoredBlockState(getColorForPixel(x, y))
}