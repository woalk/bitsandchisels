package com.woalk.mods.bitsandchisels;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public class ItemHelpers {
    private ItemHelpers(){}

    public static ItemStack blockToItem(Block block, BlockState state, BlockEntity entity) {
        return addBlockStateNbt(addBlockEntityNbt(new ItemStack(block), entity), state);
    }

    public static ItemStack addBlockEntityNbt(ItemStack stack, BlockEntity blockEntity) {
        if (blockEntity instanceof BitsBlockEntity) {
            CompoundTag compoundTag = new CompoundTag();
            ((BitsBlockEntity) blockEntity).writeBitNbt(compoundTag);
            CompoundTag compoundTag3;
            stack.addTagElement("BlockEntityTag", compoundTag);
            compoundTag3 = new CompoundTag();
            ((BitsBlockEntity) blockEntity).writeNameNbt(compoundTag3);
            stack.addTagElement("display", compoundTag3);
        }
        return stack;
    }

    public static ItemStack addBlockStateNbt(ItemStack stack, BlockState state) {
        int shaderType = state.getValue(BitsBlock.SHADER_TYPE);
        if (shaderType != 0) {
            CompoundTag compountTag2 = new CompoundTag();
            compountTag2.putInt("shader_type", shaderType);
            stack.addTagElement("BlockStateTag", compountTag2);
        }
        return stack;
    }
}
