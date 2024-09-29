package com.woalk.mods.bitsandchisels;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class ItemHelpers {
    private ItemHelpers(){}

    public static ItemStack blockToItem(Block block, BlockState state, BlockEntity entity) {
        return addBlockStateNbt(addBlockEntityNbt(new ItemStack(block), entity), state);
    }

    public static ItemStack addBlockEntityNbt(ItemStack stack, BlockEntity blockEntity) {
        if (blockEntity instanceof BitsBlockEntity) {
            NbtCompound compoundTag = new NbtCompound();
            ((BitsBlockEntity) blockEntity).writeBitNbt(compoundTag);
            NbtCompound compoundTag3;
            stack.setSubNbt("BlockEntityTag", compoundTag);
            compoundTag3 = new NbtCompound();
            ((BitsBlockEntity) blockEntity).writeNameNbt(compoundTag3);
            stack.setSubNbt("display", compoundTag3);
            if (((BitsBlockEntity) blockEntity).getStructureId() != null) {
                stack.getOrCreateNbt().putUuid("StructureId", ((BitsBlockEntity) blockEntity).getStructureId());
            }
        }
        return stack;
    }

    public static ItemStack addBlockStateNbt(ItemStack stack, BlockState state) {
        int shaderType = state.get(BitsBlock.SHADER_TYPE);
        if (shaderType != 0) {
            NbtCompound compountTag2 = new NbtCompound();
            compountTag2.putInt("shader_type", shaderType);
            stack.setSubNbt("BlockStateTag", compountTag2);
        }
        return stack;
    }
}
