package com.woalk.mods.bitsandchisels.image

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.Raster
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class ChiseledImage(imageFilePath: String): IChiseledImage {
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

    private val hasTransparency: Boolean
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
        hasTransparency = image.hasTransparentPixel()
        val icm = ConcreteColorModel()
        ditheredImage = if (hasTransparency) {
            val raster = Raster.createWritableRaster(icm.createCompatibleSampleModel(image.width, image.height), null)
            BufferedImage(icm, raster, false, null)
        } else {
            BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_INDEXED, icm)
        }
        val g2d = ditheredImage.createGraphics()
        g2d.drawImage(image, 0, 0, null)
        g2d.dispose()
    }

    override val width by ditheredImage::width
    override val height by ditheredImage::height

    override val blockWidth = width.ceilDiv(16)
    override val blockHeight = height.ceilDiv(16)

    private val dataBuffer = ditheredImage.data.dataBuffer as DataBufferByte

    private fun dataIndex(x: Int, y: Int) = dataBuffer.data.size - (x + y * width) - 1

    fun getColorForPixel(x: Int, y: Int): ConcreteColorModel.Color {
        return ConcreteColorModel.Color.values()[
                dataBuffer.data[dataIndex(x, y)
                ].toUByte().toInt()]
    }

    private fun getBlockForPixel(x: Int, y: Int, palette: PaletteIdentifier = PaletteIdentifier()): Block {
        return getColorForPixel(x, y).toBlock(palette, hasTransparency)
    }

    override fun getStateForPixel(x: Int, y: Int, palette: PaletteIdentifier?): BlockState =
        getBlockForPixel(x, y, palette!!).defaultState
}