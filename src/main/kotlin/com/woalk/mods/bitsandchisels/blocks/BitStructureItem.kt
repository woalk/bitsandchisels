package com.woalk.mods.bitsandchisels.blocks

import com.sk89q.worldedit.fabric.internal.NBTConverter
import com.sk89q.worldedit.math.transform.AffineTransform
import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.compat.ChiselExtent
import com.woalk.mods.bitsandchisels.utils.rotate
import com.woalk.mods.bitsandchisels.utils.toYaw
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.client.item.TooltipContext
import net.minecraft.fluid.Fluids
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtString
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent
import java.util.*
import kotlin.math.roundToInt

class BitStructureItem(settings: Settings) : Item(settings) {
    companion object {
        const val NBT_BIT_BLOCK_TAGS = "BitBlockTags"
        const val NBT_ROTATION = "Rotation"
        const val MAX_DIAMETER = 5
        val OFFSETS = createOffsets(MAX_DIAMETER)

        fun createOffsets(diameter: Int, offset: Int = 0) = Array(diameter * diameter * diameter) { i ->
            Vec3i(i % diameter + offset, (i / diameter) % diameter + offset, (i / diameter) / diameter + offset)
        }
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        if (context.world.isClient) return ActionResult.SUCCESS
        val actionResult = this.place(ItemPlacementContext(context))
        if (!actionResult.isAccepted && this.isFood) {
            val actionResult2 = use(context.world, context.player, context.hand).result
            return if (actionResult2 == ActionResult.CONSUME) ActionResult.CONSUME_PARTIAL else actionResult2
        }
        return actionResult
    }

    private fun place(context: ItemPlacementContext): ActionResult {
        if (!context.canPlace()) return ActionResult.FAIL
        val blockPos = context.blockPos
        val world = context.world
        val playerEntity = context.player
        val itemStack = context.stack
        val fullTag = itemStack.nbt ?: return ActionResult.FAIL
        var bitBlockState: BlockState? = null
        val structureId = UUID.randomUUID()
        val rotation = ((Direction.byId(fullTag.getInt(NBT_ROTATION)).vector.toYaw() - (playerEntity?.yaw ?: 0f)) / 90).roundToInt() * 90
        for ((index, nbtElement) in fullTag.getList(NBT_BIT_BLOCK_TAGS, NbtElement.COMPOUND_TYPE.toInt()).withIndex()) {
            val tag = nbtElement as NbtCompound
            if (tag.isEmpty) continue
            if (index >= OFFSETS.size) return ActionResult.FAIL
            val bitBlockPos = blockPos.add(OFFSETS[index].rotate(rotation))
            if (!world.getBlockState(bitBlockPos).material.isReplaceable) return ActionResult.FAIL
            if (!world.canPlace(BitsAndChisels.BITS_BLOCK.defaultState, bitBlockPos, ShapeContext.of(playerEntity))) {
                return ActionResult.FAIL
            }
        }
        for ((index, nbtElement) in fullTag.getList(NBT_BIT_BLOCK_TAGS, NbtElement.COMPOUND_TYPE.toInt()).withIndex()) {
            val tag = nbtElement as NbtCompound
            if (tag.isEmpty) continue
            val bitBlockPos = blockPos.add(OFFSETS[index].rotate(rotation))
            bitBlockState = BitsAndChisels.BITS_BLOCK.defaultState.with(
                Properties.WATERLOGGED,
                world.getFluidState(bitBlockPos).fluid === Fluids.WATER
            ).with(BitsBlock.SHADER_TYPE, tag.getInt("shader_type"))
            world.setBlockState(bitBlockPos, bitBlockState)
            world.getBlockEntity(bitBlockPos)?.let { blockEntity ->
                if (!(world.isClient || !blockEntity.copyItemDataRequiresOperator()
                            || playerEntity != null && playerEntity.isCreativeLevelTwoOp)) {
                    return@let
                }
                val nbtCompound2: NbtCompound = blockEntity.createNbt()
                val nbtCompound3 = nbtCompound2.copy()
                nbtCompound2.copyFrom(nbtElement)
                val nbtCompound4 = NBTConverter.fromNative(nbtCompound2)
                val nbtCompound5 = NBTConverter.toNative(
                    ChiselExtent.apply(AffineTransform().rotateY(rotation.toDouble()), nbtCompound4)
                )
                nbtCompound2.put("bits_v2", nbtCompound5.get("bits_v2"))
                nbtCompound2.put("palette", nbtCompound5.get("palette"))
                nbtCompound2.putUuid("StructureId", structureId)
                val displayNbt = itemStack.getSubNbt("display")?.copy() ?: NbtCompound()
                val lore = displayNbt.getList("Lore", NbtElement.STRING_TYPE.toInt())
                lore.add(NbtString.of("{\"color\":\"dark_gray\",\"text\":\"(${OFFSETS[index].toShortString()})\"}"))
                displayNbt.put("Lore", lore)
                nbtCompound2.put("display", displayNbt)
                if (nbtCompound2 != nbtCompound3) {
                    blockEntity.readNbt(nbtCompound2)
                    blockEntity.markDirty()
                    return@let
                }
            }
            if (playerEntity is ServerPlayerEntity) {
                Criteria.PLACED_BLOCK.trigger(playerEntity, blockPos, itemStack)
            }
        }
        if (bitBlockState != null) {
            val blockSoundGroup = BitsAndChisels.BITS_BLOCK.getSoundGroup(bitBlockState)
            world.playSound(
                playerEntity,
                blockPos,
                bitBlockState.soundGroup.placeSound,
                SoundCategory.BLOCKS,
                (blockSoundGroup.getVolume() + 1.0f) / 2.0f,
                blockSoundGroup.getPitch() * 0.8f
            )
            world.emitGameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Emitter.of(playerEntity, bitBlockState))
            if (playerEntity == null || !playerEntity.abilities.creativeMode) {
                itemStack.decrement(1)
            }
            return ActionResult.success(world.isClient)
        }
        return ActionResult.FAIL
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        val fullTag = stack.nbt ?: return
        var count = 0
        for (nbtElement in fullTag.getList(NBT_BIT_BLOCK_TAGS, NbtElement.COMPOUND_TYPE.toInt())) {
            if ((nbtElement as NbtCompound).isEmpty) continue
            count++
        }
        tooltip.add(Text.translatable("${getTranslationKey(stack)}.desc", count).formatted(Formatting.GRAY))
    }
}
