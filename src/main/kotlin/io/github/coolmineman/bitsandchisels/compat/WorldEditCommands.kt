package io.github.coolmineman.bitsandchisels.compat

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.context.CommandContext
import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.fabric.FabricAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BaseBlock
import com.woalk.mods.roleplay.registry.IModRegister
import io.github.coolmineman.bitsandchisels.BitsAndChisels
import io.github.coolmineman.bitsandchisels.registry.ModPermissions
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.block.BlockRenderType
import net.minecraft.block.Blocks
import net.minecraft.block.LeavesBlock
import net.minecraft.block.WallMountedBlock
import net.minecraft.server.command.ServerCommandSource
import kotlin.math.ceil

object WorldEditCommands : IModRegister {

    private fun minify(ctx: CommandContext<ServerCommandSource>, onlyFullBlocks: Boolean = true): Int = try {
        val actor = FabricAdapter.adaptPlayer(ctx.source.player)
        val localSession = WorldEdit.getInstance().sessionManager.get(actor)
        val clipboard = localSession.clipboard.clipboard

        val size = clipboard.dimensions
        val bitsSize = BlockVector3.at(
            ceil(size.x.toDouble() / 16).toInt(),
            ceil(size.y.toDouble() / 16).toInt(),
            ceil(size.z.toDouble() / 16).toInt()
        )

        val newClipboard = BlockArrayClipboard(
            CuboidRegion(
                clipboard.minimumPoint,
                clipboard.minimumPoint.add(bitsSize.subtract(BlockVector3.ONE))
            )
        )
        for (x in 0 until bitsSize.x) {
            for (y in 0 until bitsSize.y) {
                for (z in 0 until bitsSize.z) {
                    val vec = BlockVector3.at(x, y, z)
                    val blockMinPos = clipboard.minimumPoint.add(vec.multiply(16))
                    val bitsOut = BitCompoundUtil.Block3D()
                    for (i in 0..15) {
                        for (j in 0..15) {
                            for (k in 0..15) {
                                val block = FabricAdapter.adapt(
                                    clipboard.getBlock(blockMinPos.add(BlockVector3.at(i, j, k)))
                                )
                                bitsOut[i][j][k] = if (onlyFullBlocks) if (
                                    block.renderType != BlockRenderType.MODEL ||
                                    block.block.hasDynamicBounds() ||
                                    block.hasSidedTransparency() ||
                                    block.block is WallMountedBlock ||
                                    block.block is LeavesBlock ||
                                    !block.isFullCube(ctx.source.world, ctx.source.player!!.blockPos)
                                ) Blocks.AIR.defaultState else block else block
                            }
                        }
                    }
                    newClipboard.setBlock(
                        clipboard.minimumPoint.add(vec),
                        FabricAdapter.adapt(BitsAndChisels.BITS_BLOCK.defaultState)
                            .toBaseBlock(BitCompoundUtil.write3DBitArray(bitsOut))
                    )
                }
            }
        }

        localSession.clipboard = ClipboardHolder(newClipboard)
        actor.print("Minified clipboard (new size is ${bitsSize.x}, ${bitsSize.y}, ${bitsSize.z})")

        SINGLE_SUCCESS
    } catch (e: EmptyClipboardException) {
        -1
    } catch (e: Exception) {
        e.printStackTrace()
        -2
    }

    override fun registerAll() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal<ServerCommandSource>("/minify")
                    .requires(Permissions.require(ModPermissions.COMMAND_WORLDEDIT_MINIFY, 4))
                    .executes(::minify)
                    .then(
                        literal<ServerCommandSource>("ignoreLimits")
                            .executes { ctx ->
                                minify(ctx, false)
                            }
                    )
            )
        }
    }
}