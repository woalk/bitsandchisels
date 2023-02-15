package io.github.coolmineman.bitsandchisels.compat

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.context.CommandContext
import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.fabric.FabricAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.session.ClipboardHolder
import com.woalk.mods.roleplay.registry.IModRegister
import io.github.coolmineman.bitsandchisels.BitNbtUtil
import io.github.coolmineman.bitsandchisels.BitsAndChisels
import io.github.coolmineman.bitsandchisels.BitsBlock
import io.github.coolmineman.bitsandchisels.BitsBlockEntity
import io.github.coolmineman.bitsandchisels.registry.ModPermissions
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.block.*
import net.minecraft.command.argument.BlockStateArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.BlockPos
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

    private fun replace(ctx: CommandContext<ServerCommandSource>, from: BlockState, to: BlockState): Int = try {
        val actor = FabricAdapter.adaptPlayer(ctx.source.player)
        val world = FabricAdapter.adapt(ctx.source.world)
        val localSession = WorldEdit.getInstance().sessionManager.get(actor)

        val selection = localSession.getSelection(world)
        val changes = localSession.createEditSession(actor).use { editSession ->
            for (x in 0 until selection.width) {
                for (y in 0 until selection.height) {
                    for (z in 0 until selection.length) {
                        val vec = selection.minimumPoint.add(BlockVector3.at(x, y, z))
                        val blockPos = BlockPos(vec.x, vec.y, vec.z)
                        if (ctx.source.world.getBlockState(blockPos).block == BitsAndChisels.BITS_BLOCK) {
                            val entity = ctx.source.world.getBlockEntity(blockPos) as? BitsBlockEntity ?: continue
                            val bits: Block3D = entity.states
                            var hasChanged = false
                            for (i in 0..15) {
                                for (j in 0..15) {
                                    for (k in 0..15) {
                                        if (bits[i][j][k] == from) {
                                            bits[i][j][k] = to
                                            hasChanged = true
                                        }
                                    }
                                }
                            }
                            if (hasChanged) {
                                val block = editSession.getBlock(vec).toBaseBlock()
                                editSession.setBlock(vec, block.toBaseBlock(BitCompoundUtil.write3DBitArray(bits)))
                            }
                        }
                    }
                }
            }
            localSession.remember(editSession)
            editSession.blockChangeCount
        }

        actor.print("Changed $changes blocks.")
        SINGLE_SUCCESS
    } catch (e: IncompleteRegionException) {
        -1
    } catch (e: Exception) {
        e.printStackTrace()
        -2
    }

    override fun registerAll() {
        CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, _ ->
            dispatcher.register(
                literal("/minify")
                    .requires(Permissions.require(ModPermissions.COMMAND_WORLDEDIT_MINIFY, 4))
                    .executes(::minify)
                    .then(
                        literal("ignoreLimits")
                            .executes { ctx ->
                                minify(ctx, false)
                            }
                    )
            )
            dispatcher.register(
                literal("/bitreplace")
                    .requires(Permissions.require(ModPermissions.COMMAND_WORLDEDIT_REPLACE, 4))
                    .then(
                        argument("from", BlockStateArgumentType.blockState(commandRegistryAccess))
                            .then(
                                argument("to", BlockStateArgumentType.blockState(commandRegistryAccess))
                                    .executes { ctx ->
                                        replace(
                                            ctx,
                                            BlockStateArgumentType.getBlockState(ctx, "from").blockState,
                                            BlockStateArgumentType.getBlockState(ctx, "to").blockState
                                        )
                                    }
                            )
                    )
            )
        }
    }
}