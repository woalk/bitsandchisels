package com.woalk.mods.bitsandchisels.api;

import com.woalk.mods.bitsandchisels.blocks.ColoredBlockState;
import com.woalk.mods.bitsandchisels.registry.ModBlocks;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import me.lucko.fabric.api.permissions.v0.Permissions;
import org.jetbrains.annotations.Nullable;

import com.woalk.mods.bitsandchisels.BitNbtUtil;
import com.woalk.mods.bitsandchisels.BitsAndChisels;
import com.woalk.mods.bitsandchisels.BitsBlockEntity;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class BitUtils {
    private BitUtils(){}

    public static boolean setBit(Level world, BlockPos block, int x, int y, int z, BlockState state) {
        return setBit(world, block, x, y, z, state, BitsAndChisels.BITS_BLOCK.defaultBlockState());
    }

    public static boolean setBit(Level world, BlockPos block, int x, int y, int z, BlockState state,
                                 BlockState bitBlockState) {
        BlockState target_state = world.getBlockState(block);
        BlockEntity e1 = world.getBlockEntity(block);
        boolean isColoredBlock = target_state.is(ModBlocks.INSTANCE.getColoredBlock());
        if (target_state.isAir()) {
            world.setBlockAndUpdate(block, bitBlockState);
        } else if (!target_state.is(BitsAndChisels.BITS_BLOCK) && target_state.isCollisionShapeFullBlock(world, block) && (e1 == null || isColoredBlock)) {
            if (isColoredBlock) {
                target_state = new ColoredBlockState(state, world, block);
                world.removeBlockEntity(block);
            }
            world.setBlock(block, bitBlockState, 0);
            world.setBlockEntity(new BitsBlockEntity(target_state, block, BitsAndChisels.BITS_BLOCK.defaultBlockState(), true));
        }
        e1 = world.getBlockEntity(block);
        if (e1 instanceof BitsBlockEntity) {
            BitsBlockEntity e = (BitsBlockEntity) e1;
            e.setState(x, y, z, state);
            return true;
        }
        return false;
    }

    public static void update(Level world, BlockPos block) {
        BlockEntity e1 = world.getBlockEntity(block);
        if (e1 instanceof BitsBlockEntity) {
            BitsBlockEntity e = (BitsBlockEntity) e1;
            e.rebuildServer();
            e.sync();
        }
    }

    public static boolean canPlace(Player player, BlockPos block, int x, int y, int z) {
        return canPlace(player, block, x, y, z, true);
    }

    public static boolean canPlace(Player player, BlockPos block, int x, int y, int z, boolean checkPermission) {
        Level world = player.getCommandSenderWorld();
        if (!world.mayInteract(player, block)) return false;
        if (checkPermission && !Permissions.check(player, ModPermissions.USE_BIT, true)) return false;
        if (world.getBlockState(block).isAir()) {
            return true;
        }
        BlockEntity e1 = world.getBlockEntity(block);
        if (e1 instanceof BitsBlockEntity) {
            return ((BitsBlockEntity) e1).getState(x, y, z).isAir();
        }
        return false;
    }
    
    public static boolean canBreak(Player player, BlockPos block, int x, int y, int z) {
        Level world = player.getCommandSenderWorld();
        if (!world.isLoaded(block)) return false;
        if (!world.mayInteract(player, block)) return false;
        if (!Permissions.check(player, ModPermissions.USE_CHISEL, true)) return false;
        return true;
    }

    public static @Nullable BlockState getBit(Level world, BlockPos block, int x, int y, int z) {
        BlockState state = world.getBlockState(block);
        BlockEntity e1 = world.getBlockEntity(block);
        if (e1 instanceof BitsBlockEntity) {
            BitsBlockEntity e = (BitsBlockEntity) e1;
            return e.getState(x, y, z);
        } else if (!state.is(BitsAndChisels.BITS_BLOCK) && canBeChiseled(state, world, block) && state.isCollisionShapeFullBlock(world, block)) {
            return state;
        }
        return null;
    }

    public static @Nullable BlockState[][][] getBitArray(Level world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof BitsBlockEntity) {
            return ((BitsBlockEntity)be).getStates();
        }
        return null;
    }

    public static boolean exists(@Nullable BlockState bit) {
        return bit != null && !bit.isAir();
    }

    public static ItemStack getBitItemStack(BlockState state) {
        ItemStack result = new ItemStack(BitsAndChisels.BIT_ITEM);
        result.addTagElement("bit", BitNbtUtil.fromBlockState(state));
        return result;
    }

    public static BlockState getBit(ItemStack stack) {
        return BitNbtUtil.toBlockState(stack.getTagElement("bit"));
    }

    public static boolean canBeChiseled(BlockState state, Level world, BlockPos pos) {
        return state.getDestroySpeed(world, pos) <= 100 && state.getDestroySpeed(world, pos) >= 0;
    }
    
    public static void attemptBreak(ServerPlayer player, BlockPos pos, int x, int y, int z) {
        if (canBreak(player, pos, x, y, z)) {
            Level world = player.level();
            BlockState oldstate = BitUtils.getBit(world, pos, x, y, z);
            if (BitUtils.exists(oldstate) && BitUtils.setBit(world, pos, x, y, z, Blocks.AIR.defaultBlockState())) {
                BitUtils.update(world, pos);
                if (!player.isCreative()) {
                    player.getInventory().placeItemBackInInventory(BitUtils.getBitItemStack(oldstate));
                }
            }
        }
    }
    
    public static void attemptBreakRegion(ServerPlayer player, BlockPos rootPos, int x1, int y1, int z1, int x2, int y2, int z2) {
        Level world = player.level();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        Object2IntArrayMap<BlockState> drops = new Object2IntArrayMap<>();
        for (int i = x1; i <= x2; i++) {
            for (int j = y1; j <= y2; j++) {
                for (int k = z1; k <= z2; k++) {
                    mut.set(rootPos.getX() + Math.floorDiv(i, 16), rootPos.getY() + Math.floorDiv(j, 16), rootPos.getZ() + Math.floorDiv(k, 16));
                    int x = Math.floorMod(i, 16);
                    int y = Math.floorMod(j, 16);
                    int z = Math.floorMod(k, 16);
                    if (canBreak(player, mut, x, y, z)) {
                        BlockState oldstate = BitUtils.getBit(world, mut, x, y, z);
                        if (BitUtils.exists(oldstate) && BitUtils.setBit(world, mut, x, y, z, Blocks.AIR.defaultBlockState())) {
                            drops.put(oldstate, drops.getOrDefault(oldstate, 0) + 1);
                        }
                    }
                }
            }
        }
        int blockx1 = rootPos.getX() + Math.floorDiv(x1, 16);
        int blocky1 = rootPos.getY() + Math.floorDiv(y1, 16);
        int blockz1 = rootPos.getZ() + Math.floorDiv(z1, 16);
        int blockx2 = rootPos.getX() + Math.floorDiv(x2, 16);
        int blocky2 = rootPos.getY() + Math.floorDiv(y2, 16);
        int blockz2 = rootPos.getZ() + Math.floorDiv(z2, 16);
        for (int i = blockx1; i <= blockx2; i++) {
            for (int j = blocky1; j <= blocky2; j++) {
                for (int k = blockz1; k <= blockz2; k++) {
                    mut.set(i, j, k);
                    BitUtils.update(world, mut);
                }
            }
        }
        if (!player.isCreative()) {
            for (Object2IntMap.Entry<BlockState> e : drops.object2IntEntrySet()) {
                ItemStack stack = BitUtils.getBitItemStack(e.getKey());
                stack.setCount(e.getIntValue());
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }
}
