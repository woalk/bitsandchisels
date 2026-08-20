package com.woalk.mods.bitsandchisels.blocks

import com.sk89q.worldedit.fabric.internal.NBTConverter
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.util.formatting.text.NbtComponent
import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.compat.ChiselExtent
import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import com.woalk.mods.bitsandchisels.utils.rotate
import com.woalk.mods.bitsandchisels.utils.toYaw
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients.components
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.nbt.StringTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import java.util.*
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
            val actionResult2 = use(context.level, context.player, context.hand).result
            return if (actionResult2 == InteractionResult.CONSUME) InteractionResult.CONSUME_PARTIAL else actionResult2
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
        val rotation = ((Direction.from3DDataValue(fullTag.getInt(NBT_ROTATION)).normal.toYaw() - (playerEntity?.yRot ?: 0f)) / 90).roundToInt() * 90
        for ((index, nbtElement) in fullTag.getList(NBT_BIT_BLOCK_TAGS, Tag.TAG_COMPOUND.toInt()).withIndex()) {
            val tag = nbtElement as CompoundTag
            if (tag.isEmpty) continue
            if (index >= OFFSETS.size) return InteractionResult.FAIL
            val bitBlockPos = blockPos.offset(OFFSETS[index].rotate(rotation))
            if (!world.getBlockState(bitBlockPos).canBeReplaced()) return InteractionResult.FAIL
            if (!world.isUnobstructed(BitsAndChisels.BITS_BLOCK.defaultBlockState(), bitBlockPos, CollisionContext.of(playerEntity))) {
                return InteractionResult.FAIL
            }
        }
        for ((index, nbtElement) in fullTag.getList(NBT_BIT_BLOCK_TAGS, Tag.TAG_COMPOUND.toInt()).withIndex()) {
            val tag = nbtElement as CompoundTag
            if (tag.isEmpty) continue
            val bitBlockPos = blockPos.offset(OFFSETS[index].rotate(rotation))
            bitBlockState = BitsAndChisels.BITS_BLOCK.defaultBlockState().setValue(
                BlockStateProperties.WATERLOGGED,
                world.getFluidState(bitBlockPos).type === Fluids.WATER
            ).setValue(BitsBlock.SHADER_TYPE, tag.getInt("shader_type"))
            world.setBlockAndUpdate(bitBlockPos, bitBlockState)
            world.getBlockEntity(bitBlockPos, BitsAndChisels.BITS_BLOCK_ENTITY).getOrNull()?.let { blockEntity ->
                if (!(world.isClientSide || !blockEntity.onlyOpCanSetNbt()
                            || playerEntity != null && playerEntity.canUseGameMasterBlocks())) {
                    return@let
                }
                val nbtCompound2: CompoundTag = blockEntity.saveWithoutMetadata(context.level.registryAccess())
                val nbtCompound3 = nbtCompound2.copy()
                nbtCompound2.merge(nbtElement)
                val nbtCompound4 = NBTConverter.fromNative(nbtCompound2)
                val nbtCompound5 = NBTConverter.toNative(
                    ChiselExtent.apply(AffineTransform().rotateY(rotation.toDouble()), nbtCompound4)
                )
                nbtCompound2.put("bits_v2", nbtCompound5.get("bits_v2"))
                nbtCompound2.put("palette", nbtCompound5.get("palette"))
                nbtCompound2.putUUID("StructureId", structureId)
                val name = itemStack.components.get(DataComponents.CUSTOM_NAME)
                blockEntity.customName = name
                val lore = itemStack.components.get(DataComponents.LORE)?.lines.orEmpty().toMutableList()
                lore.add(Component.literal(OFFSETS[index].toShortString()).withStyle(ChatFormatting.DARK_GRAY))
                blockEntity.setLoreText(lore)
                if (nbtCompound2 != nbtCompound3) {
                    blockEntity.loadCustomOnly(nbtCompound2, context.level.registryAccess())
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
            return InteractionResult.sidedSuccess(world.isClientSide)
        }
        return InteractionResult.FAIL
    }

    override fun appendHoverText(
        stack: ItemStack,
        tooltipContext: TooltipContext,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, tooltipContext, tooltip, flag)
        val fullTag = stack.components.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return
        var count = 0
        for (nbtElement in fullTag.getList(NBT_BIT_BLOCK_TAGS, Tag.TAG_COMPOUND.toInt())) {
            if ((nbtElement as CompoundTag).isEmpty) continue
            count++
        }
        tooltip.add(Component.translatable("${getDescriptionId(stack)}.desc", count).withStyle(ChatFormatting.GRAY))
    }
}
