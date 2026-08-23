package com.woalk.mods.bitsandchisels.utils;

import com.woalk.mods.bitsandchisels.BitsAndChisels;
import com.woalk.mods.bitsandchisels.blocks.BitsBlock;
import com.woalk.mods.bitsandchisels.blocks.BitsBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;

public class ItemHelpers {
    private ItemHelpers(){}

    public static ItemStack blockToItem(Block block, BlockState state, BlockEntity entity) {
        return addBlockStateNbt(addBlockEntityNbt(new ItemStack(block), entity), state);
    }

    public static ItemStack addBlockEntityNbt(ItemStack stack, BlockEntity blockEntity) {
        if (blockEntity instanceof BitsBlockEntity bitsBlockEntity) {
            stack.applyComponents(blockEntity.collectComponents());
            CompoundTag compoundTag = new CompoundTag();
            bitsBlockEntity.writeBitNbt(compoundTag);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockEntity.getType(), compoundTag));
        }
        return stack;
    }

    public static ItemStack addBlockStateNbt(ItemStack stack, BlockState state) {
        int shaderType = state.getValue(BitsBlock.SHADER_TYPE);
        if (shaderType != 0) {
            final var properties = new HashMap<String, String>();
            properties.put(BitsBlock.SHADER_TYPE.getName(), BitsBlock.SHADER_TYPE.getName(shaderType));
            stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(properties));
        }
        return stack;
    }
}
