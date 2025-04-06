package com.woalk.mods.bitsandchisels.compat

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.fabric.FabricAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.session.ClipboardHolder
import com.woalk.mods.bitsandchisels.registry.IModRegister
import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlockEntity
import com.woalk.mods.bitsandchisels.api.BitUtils
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem
import com.woalk.mods.bitsandchisels.registry.ModPermissions
import com.woalk.mods.bitsandchisels.utils.DirectionArgumentType
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.block.*
import net.minecraft.command.argument.BlockStateArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i
import net.minecraft.util.registry.Registry
import java.util.*
import kotlin.math.ceil

object WorldEditCommands : IModRegister {

    private fun minify(
        ctx: CommandContext<ServerCommandSource>,
        onlyFullBlocks: Boolean = true,
        fullBlockHeuristic: Boolean = true
    ): Int = try {
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
                                val blockId = block.registryEntry.key.get().value
                                val fullBlock = findBlock(block, blockId, fullBlockHeuristic)
                                bitsOut[i][j][k] = if (onlyFullBlocks) if (
                                    fullBlock.renderType != BlockRenderType.MODEL ||
                                    fullBlock.block.hasDynamicBounds() ||
                                    fullBlock.hasSidedTransparency() ||
                                    fullBlock.block is WallMountedBlock ||
                                    fullBlock.block is LeavesBlock ||
                                    !fullBlock.isFullCube(ctx.source.world, ctx.source.player!!.blockPos)
                                ) Blocks.AIR.defaultState else fullBlock else block
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

    private fun findBlock(block: BlockState, id: Identifier, fullBlockHeuristic: Boolean): BlockState {
        val blockId = if (id.namespace == "minekea") {
            Identifier.of(
                if (id.path.startsWith("betterend") || id.path.startsWith("betternether") || id.path.startsWith("byg")) {
                    id.path.split("/").first()
                } else id.namespace,
                id.path.replace(Regex("building/.+/bookshelves"), "furniture/bookshelves"),
            )!!
        } else id
        return if (fullBlockHeuristic) {
            if (blockId.path.endsWith("_stairs")) {
                findBlock(blockId, blockId.path.substring(0, blockId.path.length - 7)) ?:
                findBlock(blockId, blockId.path.substring(0, blockId.path.length - 7) + "s") ?:
                findBlock(blockId, blockId.path.substring(0, blockId.path.length - 6) + "planks")
            } else if (blockId.path.endsWith("_slab")) {
                findBlock(blockId, blockId.path.substring(0, blockId.path.length - 5)) ?:
                findBlock(blockId, blockId.path.substring(0, blockId.path.length - 5) + "s") ?:
                findBlock(blockId, blockId.path.substring(0, blockId.path.length - 4) + "planks")
            } else if (blockId.path.endsWith("_wall")) {
                findBlock(blockId, blockId.path.substring(0, blockId.path.length - 5)) ?:
                findBlock(blockId, blockId.path.substring(0, blockId.path.length - 5) + "s")
            } else if (blockId.path.contains("/stairs/vertical/")) {
                findBlock(blockId, blockId.path.replace("/stairs/vertical/", "/dyed/")) ?:
                findBlock(blockId, blockId.path.replace("/stairs/vertical/", "/dyed/").replace("brick", "bricks")) ?:
                findBlock("minecraft", blockId.path.split("/").last()) ?:
                findBlock("minecraft", blockId.path.split("/").last() + "s") ?:
                findBlock("minecraft", blockId.path.split("/").last() + "_planks")
            } else if (blockId.path.contains("/stairs/")) {
                findBlock(blockId, blockId.path.replace("/stairs/", "/dyed/")) ?:
                findBlock("minecraft", blockId.path.split("/").last()) ?:
                findBlock("minecraft", blockId.path.split("/").last() + "s") ?:
                findBlock("minecraft", blockId.path.split("/").last() + "_planks")
            } else if (blockId.path.contains("/slabs/")) {
                findBlock(blockId, blockId.path.replace("/slabs/", "/dyed/")) ?:
                findBlock("minecraft", blockId.path.split("/").last()) ?:
                findBlock("minecraft", blockId.path.split("/").last() + "s") ?:
                findBlock("minecraft", blockId.path.split("/").last() + "_planks") ?:
                findBlock("minecraft", blockId.path.split("/").last() + "_block")
            } else if (blockId.path.contains("/walls/")) {
                findBlock(blockId, blockId.path.replace("/walls/", "/dyed/")) ?:
                findBlock("minecraft", blockId.path.split("/").last()) ?:
                findBlock("minecraft", blockId.path.split("/").last() + "s") ?:
                findBlock("minecraft", blockId.path.split("/").last() + "_planks") ?:
                findBlock("minecraft", blockId.path.split("/").last() + "_block")
            } else if (blockId.path.contains("/bookshelves/storage/")) {
                findBlock(blockId, blockId.path.replace("furniture/bookshelves/storage/", "building/dyed/")) ?:
                findBlock("minecraft", blockId.path.split("/").last()) ?:
                findBlock("minecraft", blockId.path.split("/").last() + "s") ?:
                findBlock("minecraft", blockId.path.split("/").last() + "_block")
            } else if (blockId.path.contains("/bookshelves/")) {
                findBlock(blockId, blockId.path.replace("furniture/bookshelves/", "building/dyed/")) ?:
                findBlock("minecraft", blockId.path.split("/").last()) ?:
                findBlock("minecraft", blockId.path.split("/").last() + "s") ?:
                findBlock("minecraft", blockId.path.split("/").last() + "_block") ?:
                Blocks.BOOKSHELF.defaultState
            } else if (blockId.path.startsWith("containers/")) {
                findBlock("minecraft", blockId.path.split("/").last() + "_planks")
            } else if (blockId.path.endsWith("leaves") || blockId.path.endsWith("_crop")) {
                Blocks.MOSS_BLOCK.defaultState
            } else block
        } else { block } ?: block
    }
    private fun findBlock(ns: Identifier, path: String): BlockState? = findBlock(ns.namespace, path)
    private fun findBlock(ns: String, path: String): BlockState? =
        Registry.BLOCK.get(Identifier(ns, path)).defaultState.let { if (it.isAir) null else it }

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

    private fun shift(
        ctx: CommandContext<ServerCommandSource>,
        direction: Direction = Direction.getEntityFacingOrder(ctx.source.entityOrThrow).first(),
        amount: Int = 1,
        merge: Boolean = false,
    ): Int = try {
        val actor = FabricAdapter.adaptPlayer(ctx.source.player)
        val world = FabricAdapter.adapt(ctx.source.world)
        val localSession = WorldEdit.getInstance().sessionManager.get(actor)

        val directionVectorInverted = direction.vector.multiply(-1)
        val translateBy = direction.vector.multiply(amount)
        val translateByInverted = translateBy.multiply(-1)
        val selection = localSession.getSelection(world)
        val changes = localSession.createEditSession(actor).use { editSession ->
            val (from, to) = if (direction.vector.x < 0 || direction.vector.y < 0 || direction.vector.z < 0) {
                Pair(direction.vector, Vec3i(selection.width, selection.height, selection.length))
            } else Pair(Vec3i.ZERO, direction.vector.add(selection.width, selection.height, selection.length))
            for (x in from.x until to.x) {
                for (y in from.y until to.y) {
                    for (z in from.z until to.z) {
                        val vec = selection.minimumPoint.add(BlockVector3.at(x, y, z))
                        val newBlock = FabricAdapter.adapt(BitsAndChisels.BITS_BLOCK.defaultState).toBaseBlock()
                        val bits = BitCompoundUtil.Block3D()
                        var hasChanged = false
                        for (i in 0..15) {
                            for (j in 0..15) {
                                for (k in 0..15) {
                                    val sourceBitPos = translateByInverted.add(i, j, k)
                                    val bit = if (sourceBitPos.x < 0 || sourceBitPos.x > 15 ||
                                        sourceBitPos.y < 0 || sourceBitPos.y > 15 ||
                                        sourceBitPos.z < 0 || sourceBitPos.z > 15
                                    ) {
                                        val neighborPos = directionVectorInverted.add(vec.x, vec.y, vec.z)
                                        if (!merge && !selection.contains(BlockVector3.at(neighborPos.x, neighborPos.y, neighborPos.z))) {
                                            null
                                        } else if (ctx.source.world.getBlockState(BlockPos(neighborPos)).block == BitsAndChisels.BITS_BLOCK) {
                                            val entity = ctx.source.world.getBlockEntity(BlockPos(neighborPos)) as? BitsBlockEntity
                                            val oldBits = entity?.states
                                            val oldBitPos = BlockPos(sourceBitPos.x.mod(16), sourceBitPos.y.mod(16), sourceBitPos.z.mod(16))
                                            val oldBit = oldBits?.get(oldBitPos.x)?.get(oldBitPos.y)?.get(oldBitPos.z)
                                            if (oldBit?.isAir != false) {
                                                val blockPos = BlockPos(vec.x, vec.y, vec.z)
                                                val entity = ctx.source.world.getBlockEntity(blockPos) as? BitsBlockEntity
                                                val oldBits = entity?.states
                                                oldBits?.get(i)?.get(j)?.get(k)
                                            } else oldBit
                                        } else null
                                    } else {
                                        val blockPos = BlockPos(vec.x, vec.y, vec.z)
                                        if (!selection.contains(BlockVector3.at(blockPos.x, blockPos.y, blockPos.z))) {
                                            val entity = ctx.source.world.getBlockEntity(blockPos) as? BitsBlockEntity
                                            val oldBits = entity?.states
                                            oldBits?.get(i)?.get(j)?.get(k)
                                        } else if (ctx.source.world.getBlockState(blockPos).block == BitsAndChisels.BITS_BLOCK) {
                                            val entity = ctx.source.world.getBlockEntity(blockPos) as? BitsBlockEntity
                                            val oldBits = entity?.states
                                            oldBits?.get(sourceBitPos.x)?.get(sourceBitPos.y)?.get(sourceBitPos.z)
                                        } else null
                                    }
                                    bits[i][j][k] = if (bit != null) {
                                        hasChanged = true
                                        bit
                                    } else Blocks.AIR.defaultState
                                }
                            }
                        }
                        if (hasChanged) {
                            editSession.setBlock(vec, newBlock.toBaseBlock(BitCompoundUtil.write3DBitArray(bits)))
                            (ctx.source.world.getBlockEntity(BlockPos(vec.x, vec.y, vec.z)) as? BitsBlockEntity)?.sync()
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

    private enum class StructDo {
        CREATE,
        DELETE,
        NOTHING,
        AUTO,
    }
    private fun Boolean.structDo(): StructDo = if (this) StructDo.CREATE else StructDo.NOTHING

    private fun sign(
        ctx: CommandContext<ServerCommandSource>,
        name: String? = null,
        author: String? = ctx.source.name,
        structure: StructDo = StructDo.CREATE,
        allowSingleBlockStructure: Boolean = false,
    ): Int = try {
        val actor = FabricAdapter.adaptPlayer(ctx.source.player)
        val world = FabricAdapter.adapt(ctx.source.world)
        val localSession = WorldEdit.getInstance().sessionManager.get(actor)

        val selection = localSession.getSelection(world)
        val isSingleBlock = selection.width == 1 && selection.height == 1 && selection.length == 1
        val structureId = if (structure == StructDo.CREATE && !(selection.width > BitStructureItem.MAX_DIAMETER ||
                    selection.height > BitStructureItem.MAX_DIAMETER ||
                    selection.length > BitStructureItem.MAX_DIAMETER) && !(isSingleBlock && !allowSingleBlockStructure)
        ) {
            UUID.randomUUID()
        } else null
        val structureMap = if (structure == StructDo.AUTO) HashMap<String, UUID>() else null
        val origin = selection.minimumPoint
        val hasMissed = LinkedList<BlockPos>()
        var count = 0
        for (x in 0 until selection.width) {
            for (y in 0 until selection.height) {
                for (z in 0 until selection.length) {
                    val pos = BlockPos(origin.x + x, origin.y + y, origin.z + z)
                    val be = ctx.source.world.getBlockEntity(pos) as? BitsBlockEntity
                    if (be == null) {
                        if (!ctx.source.world.getBlockState(pos).isAir) {
                            hasMissed.add(pos)
                        }
                    } else {
                        name?.let { be.customName = Text.literal(it) }
                        if (author != null) {
                            val loreText = mutableListOf<Text>(Text.literal("Created by $author"))
                            if (selection.width * selection.height * selection.length > 1) loreText.add(
                                Text.literal("($x, $y, $z)").formatted(Formatting.DARK_GRAY)
                            )
                            be.setLoreText(loreText)
                        }
                        if (structureId != null) {
                            be.structureId = structureId
                        } else if (structure == StructDo.DELETE) {
                            be.structureId = null
                        } else if (structure == StructDo.AUTO) be.customName?.let {
                            be.structureId = structureMap!!.getOrPut(it.string, UUID::randomUUID)
                        }
                        BitUtils.update(ctx.source.world, pos)
                        count++
                    }
                }
            }
        }
        if (author != null) {
            ctx.source.sendFeedback(Text.literal("Updated $count blocks with signature"), true)
            if (structure == StructDo.CREATE && structureId == null) {
                ctx.source.sendFeedback(
                    Text.literal(
                        "Warning: Build too ${if (isSingleBlock) "small" else "large"} to be labeled as structure."
                    ).formatted(Formatting.YELLOW),
                    false,
                )
            }
        } else if (structure == StructDo.CREATE && structureId != null) {
            ctx.source.sendFeedback(Text.literal("Created structure of $count blocks with ID $structureId."), true)
        } else if (structure == StructDo.CREATE /* && structureId == null */) {
            ctx.source.sendFeedback(
                Text.literal("Warning: Build too large to be labeled as structure.")
                    .formatted(Formatting.YELLOW),
                false,
            )
        } else if (structure == StructDo.DELETE) {
            ctx.source.sendFeedback(Text.literal("Removed structure ID from $count blocks."), true)
        } else if (structure == StructDo.AUTO) {
            ctx.source.sendFeedback(Text.literal("Created ${structureMap!!.size} structures within $count blocks."), true)
            if (structureMap.size <= 0) {
                ctx.source.sendFeedback(Text.literal(
                    "Note that unsigned bit blocks will not be grouped by //bitstructure auto."
                ).formatted(Formatting.RED), false)
            }
        } else {
            ctx.source.sendFeedback(Text.literal(
                "Inconsistency detected. Please report to admin that function 'sign' was called with no arguments."
            ).formatted(Formatting.RED), true)
        }

        if (hasMissed.isNotEmpty()) {
            ctx.source.sendFeedback(Text.literal(
                if (hasMissed.size < 4) {
                    "Warning: ${hasMissed.joinToString(", ") { "(${it.toShortString()})" }} weren't bit blocks or air."
                } else {
                    "Warning: Selection contained ${hasMissed.size} blocks that weren't bit blocks or air."
                }
            ).formatted(Formatting.YELLOW), false)
        }
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
                    .executes(WorldEditCommands::minify)
                    .then(
                        literal("ignoreLimits")
                            .executes { ctx ->
                                minify(ctx, false)
                            }
                    )
                    .then(
                        literal("noHeuristic")
                            .executes { ctx ->
                                minify(ctx, fullBlockHeuristic = false)
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
            dispatcher.register(
                literal("/bitshift")
                    .requires(Permissions.require(ModPermissions.COMMAND_WORLDEDIT_SHIFT, 4))
                    .then(
                        argument("direction", DirectionArgumentType.direction())
                            .executes { ctx ->
                                shift(
                                    ctx,
                                    DirectionArgumentType.getDirection(ctx, "direction")
                                )
                            }
                            .then(
                                argument("amount", IntegerArgumentType.integer(1, 15))
                                    .executes { ctx ->
                                        shift(
                                            ctx,
                                            DirectionArgumentType.getDirection(ctx, "direction"),
                                            IntegerArgumentType.getInteger(ctx, "amount")
                                        )
                                    }
                                    .then(
                                        literal("merge")
                                            .executes { ctx ->
                                                shift(
                                                    ctx,
                                                    DirectionArgumentType.getDirection(ctx, "direction"),
                                                    IntegerArgumentType.getInteger(ctx, "amount"),
                                                    true,
                                                )
                                            }
                                    )
                            )
                    )
                    .executes(::shift)
                    .then(
                        argument("amount", IntegerArgumentType.integer(1, 15))
                            .executes { ctx ->
                                shift(
                                    ctx,
                                    amount = IntegerArgumentType.getInteger(ctx, "amount")
                                )
                            }
                            .then(
                                literal("merge")
                                    .executes { ctx ->
                                        shift(
                                            ctx,
                                            amount = IntegerArgumentType.getInteger(ctx, "amount"),
                                            merge = true,
                                        )
                                    }
                            )
                    )
            )
            dispatcher.register(
                literal("/bitsign")
                    .requires(Permissions.require(ModPermissions.COMMAND_WORLDEDIT_SIGN, 4))
                    .executes(::sign)
                    .then(
                        argument("title", StringArgumentType.string())
                            .executes { ctx ->
                                sign(ctx, StringArgumentType.getString(ctx, "title"))
                            }
                            .then(
                                argument("author", StringArgumentType.string())
                                    .requires(Permissions.require(ModPermissions.COMMAND_WORLDEDIT_SIGN_OTHER, 4))
                                    .executes { ctx ->
                                        sign(
                                            ctx,
                                            StringArgumentType.getString(ctx, "title"),
                                            StringArgumentType.getString(ctx, "author"),
                                        )
                                    }
                                    .then(
                                        argument("structure", BoolArgumentType.bool())
                                            .executes { ctx ->
                                                sign(
                                                    ctx,
                                                    StringArgumentType.getString(ctx, "title"),
                                                    StringArgumentType.getString(ctx, "author"),
                                                    BoolArgumentType.getBool(ctx, "structure").structDo()
                                                )
                                            }
                                    )
                            )
                            .then(
                                argument("structure", BoolArgumentType.bool())
                                    .executes { ctx ->
                                        sign(
                                            ctx,
                                            StringArgumentType.getString(ctx, "title"),
                                            structure = BoolArgumentType.getBool(ctx, "structure").structDo()
                                        )
                                    }
                            )
                    )
            )
            dispatcher.register(
                literal("/bitstructure")
                    .requires(Permissions.require(ModPermissions.COMMAND_WORLDEDIT_SIGN, 4))
                    .then(
                        literal("create")
                            .executes { ctx ->
                                sign(
                                    ctx,
                                    null,
                                    null,
                                    StructDo.CREATE,
                                    true,
                                )
                            }
                    )
                    .then(
                        literal("clear")
                            .executes { ctx ->
                                sign(
                                    ctx,
                                    null,
                                    null,
                                    StructDo.DELETE,
                                    true,
                                )
                            }
                    )
                    .then(
                        literal("auto")
                            .requires(Permissions.require(ModPermissions.COMMAND_WORLDEDIT_SIGN_OTHER, 4))
                            .executes { ctx ->
                                sign(
                                    ctx,
                                    null,
                                    null,
                                    StructDo.AUTO,
                                    true,
                                )
                            }
                    )
            )
        }
    }
}