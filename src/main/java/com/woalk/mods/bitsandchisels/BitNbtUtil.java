package com.woalk.mods.bitsandchisels;

import java.util.ArrayList;

import com.woalk.mods.bitsandchisels.blocks.ColoredBlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ListTag;

public class BitNbtUtil {
    private BitNbtUtil() { }

    public static boolean read3DBitArray(CompoundTag tag, BlockState[][][] out) {
        ArrayList<BlockState> palette = new ArrayList<>();
        ListTag paletteTag = (ListTag) tag.get("palette");
        if (paletteTag == null) return false;
        for (Tag statetag : paletteTag) {
            palette.add(BitNbtUtil.toBlockState((CompoundTag) statetag));
        }
        ByteArrayTag bits2 = (ByteArrayTag) tag.get("bits_v2");
        int index = 0;
        if (bits2 != null) {
            byte[] bitsBytes = bits2.getAsByteArray();
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    for (int k = 0; k < 16; k++) {
                        out[i][j][k] = palette.get((bitsBytes[index + 1] << 8 | bitsBytes[index] & 0xFF));
                        index += 2;
                    }
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public static void write3DBitArray(CompoundTag tag, BlockState[][][] in) {
        ArrayList<BlockState> palette = new ArrayList<>();
        byte[] bits = new byte[8192];
        int arrayIndex = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    BlockState state = in[i][j][k];
                    short index = (short) palette.indexOf(state);
                    if (index == -1) {
                        palette.add(state);
                        index = (short) (palette.size() - 1);
                    }
                    bits[arrayIndex] = (byte)(index & 0xff);
                    bits[arrayIndex + 1] = (byte)((index >> 8) & 0xff);
                    arrayIndex += 2;
                }
            }
        }
        tag.put("bits_v2", new ByteArrayTag(bits));
        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            paletteTag.add(BitNbtUtil.fromBlockState(state));
        }
        tag.put("palette", paletteTag);
    }

    public static BlockState toBlockState(@Nullable CompoundTag tag) {
        if (tag == null) {
            return Blocks.AIR.defaultBlockState();
        } else if (tag.contains(ColoredBlockState.TAG_COLOR)) {
            return new ColoredBlockState(tag.getInt(ColoredBlockState.TAG_COLOR).get());
        } else {
            return NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag);
        }
    }

    public static CompoundTag fromBlockState(@Nullable BlockState state) {
        if (state == null) {
            return NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState());
        } else if (state instanceof ColoredBlockState) {
            CompoundTag nbt = NbtUtils.writeBlockState(state);
            nbt.putInt(ColoredBlockState.TAG_COLOR, ((ColoredBlockState) state).getColor());
            return nbt;
        } else {
            return NbtUtils.writeBlockState(state);
        }
    }
}
