package io.github.coolmineman.bitsandchisels.image

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.Block
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class ChiseledImage(imageFilePath: String) {
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

    private val ditheredImage: BufferedImage

    init {
        if (!enabled) {
            throw IllegalStateException("image2chisel is disabled")
        }
        if (!isFileAllowed(imageFilePath)) {
            throw IOException("Reading this file was prevented for security reasons")
        }
        val image = ImageIO.read(File(imagesDir, imageFilePath))
        if (image.width * image.height > MAX_PIXELS) {
            throw IllegalArgumentException("Image is too large, maximum is ${MAX_PIXELS}px")
        }
        ditheredImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_INDEXED, ConcreteColorModel())
        val g2d = ditheredImage.createGraphics()
        g2d.drawImage(image, 0, 0, null)
        g2d.dispose()
    }

    val width by ditheredImage::width
    val height by ditheredImage::height

    val blockWidth = width.ceilDiv(16)
    val blockHeight = height.ceilDiv(16)

    private val dataBuffer = ditheredImage.data.dataBuffer as DataBufferByte

    private fun dataIndex(x: Int, y: Int) = dataBuffer.size - (x + y * width) - 1

    fun getColorForPixel(x: Int, y: Int): ConcreteColorModel.Color {
        return ConcreteColorModel.Color.values()[
                dataBuffer.data[dataIndex(x, y)
                ].toUByte().toInt()]
    }

    fun getBlockForPixel(x: Int, y: Int, palette: PaletteIdentifier = PaletteIdentifier()): Block {
        return getColorForPixel(x, y).toBlock(palette)
    }
}