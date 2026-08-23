package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.utils.rotate
import com.woalk.mods.bitsandchisels.utils.toYaw
import net.minecraft.advancements.triggers.CriteriaTriggers
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.core.UUIDUtil
import net.minecraft.core.Vec3i
import net.minecraft.core.component.DataComponents
import net.minecraft.util.ProblemReporter
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.storage.TagValueInput
import java.util.*
import java.util.function.Consumer
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt

class BitStructureItem(settings: Properties) : Item(settings) {
    companion object {
        const val NBT_BIT_BLOCK_TAGS = "BitBlockTags"
        const val NBT_ROTATION = "Rotation"
        const val MAX_DIAMETER = 5
        val OFFSETS = createOffsets(MAX_DIAMETER)

        fun createOffsets(diameter: Int, offset: Int = 0) = Array(diameter * diameter * diameter) { i ->
            Vec3i(i % diameter + offset, (i / diameter) % diameter + offset, (i / diameter) / diameter + offset)
        }
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        if (context.level.isClientSide) return InteractionResult.SUCCESS
        val actionResult = this.place(BlockPlaceContext(context))
        if (!actionResult.consumesAction() && components().get(DataComponents.FOOD) != null) {
            val actionResult2 = use(context.level, context.player!!, context.hand)
            return if (actionResult2 == InteractionResult.CONSUME) InteractionResult.CONSUME else actionResult2
        }
        return actionResult
    }

    private fun place(context: BlockPlaceContext): InteractionResult {
        if (!context.canPlace()) return InteractionResult.FAIL
        val blockPos = context.clickedPos
        val world = context.level
        val playerEntity = context.player
        val itemStack = context.itemInHand
        val fullTag = itemStack.components.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return InteractionResult.FAIL
        var bitBlockState: BlockState? = null
        val structureId = UUID.randomUUID()
        val rotation = ((Direction.from3DDataValue(fullTag.getIntOr(NBT_ROTATION, 0)).getUnitVec3i().toYaw() - (playerEntity?.yRot ?: 0f)) / 90).roundToInt() * 90
        for ((index, nbtElement) in fullTag.getListOrEmpty(NBT_BIT_BLOCK_TAGS).withIndex()) {
            val tag = nbtElement.asCompound().orElse(null) ?: continue
            if (tag.isEmpty) continue
            if (index >= OFFSETS.size) return InteractionResult.FAIL
            val bitBlockPos = blockPos.offset(OFFSETS[index].rotate(rotation))
            if (!world.getBlockState(bitBlockPos).canBeReplaced()) return InteractionResult.FAIL
            if (playerEntity != null && !world.isUnobstructed(BitsAndChisels.BITS_BLOCK.defaultBlockState(), bitBlockPos, CollisionContext.of(playerEntity))) {
                return InteractionResult.FAIL
            }
        }
        for ((index, nbtElement) in fullTag.getListOrEmpty(NBT_BIT_BLOCK_TAGS).withIndex()) {
            val tag = nbtElement.asCompound().orElse(null) ?: continue
            if (tag.isEmpty) continue
            val bitBlockPos = blockPos.offset(OFFSETS[index].rotate(rotation))
            bitBlockState = BitsAndChisels.BITS_BLOCK.defaultBlockState().setValue(
                BlockStateProperties.WATERLOGGED,
                world.getFluidState(bitBlockPos).type === Fluids.WATER
            ).setValue(BitsBlock.SHADER_TYPE, tag.getIntOr("shader_type", 0))
            world.setBlockAndUpdate(bitBlockPos, bitBlockState)
            world.getBlockEntity(bitBlockPos, BitsAndChisels.BITS_BLOCK_ENTITY).getOrNull()?.let { blockEntity ->
                val nbtCompound2: CompoundTag = blockEntity.saveWithoutMetadata(context.level.registryAccess())
                val nbtCompound3 = nbtCompound2.copy()
                nbtCompound2.merge(tag)
                // TODO: WorldEdit rotation support needs updating for 26.2
                // ChiselExtent rotation was removed as NBTConverter API changed
                nbtCompound2.store("StructureId", UUIDUtil.CODEC, structureId)
                val name = itemStack.components.get(DataComponents.CUSTOM_NAME)
                blockEntity.customName = name
                val lore = itemStack.components.get(DataComponents.LORE)?.lines.orEmpty().toMutableList()
                lore.add(Component.literal(OFFSETS[index].toShortString()).withStyle(ChatFormatting.DARK_GRAY))
                blockEntity.setLoreText(lore)
                if (nbtCompound2 != nbtCompound3) {
                    blockEntity.loadCustomOnly(TagValueInput.create(ProblemReporter.Collector.DISCARDING, world.registryAccess(), nbtCompound2))
                    blockEntity.setChanged()
                    return@let
                }
            }
            if (playerEntity is ServerPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(playerEntity, blockPos, itemStack)
            }
        }
        if (bitBlockState != null) {
            val blockSoundGroup = bitBlockState.soundType
            world.playSound(
                playerEntity,
                blockPos,
                blockSoundGroup.placeSound,
                SoundSource.BLOCKS,
                (blockSoundGroup.getVolume() + 1.0f) / 2.0f,
                blockSoundGroup.getPitch() * 0.8f
            )
            world.gameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Context.of(playerEntity, bitBlockState))
            if (playerEntity == null || !playerEntity.abilities.instabuild) {
                itemStack.shrink(1)
            }
            return InteractionResult.SUCCESS
        }
        return InteractionResult.FAIL
    }

    override fun appendHoverText(
        itemStack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        builder: Consumer<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag)
        val fullTag = itemStack.components.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return
        var count = 0
        for (nbtElement in fullTag.getListOrEmpty(NBT_BIT_BLOCK_TAGS)) {
            val compound = nbtElement.asCompound().orElse(null) ?: continue
            if (compound.isEmpty) continue
            count++
        }
        builder.accept(Component.translatable("${getDescriptionId()}.desc", count).withStyle(ChatFormatting.GRAY))
    }
}
