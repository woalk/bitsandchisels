package com.woalk.mods.bitsandchisels;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class BitsBlock extends BlockWithEntity implements Waterloggable {

    public static final IntProperty LIGHT_LEVEL = IntProperty.of("light_level", 0, 16);
    public static final IntProperty SHADER_TYPE = IntProperty.of("shader_type", 0, 16);

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public BitsBlock(Settings settings) {
        super(settings);
        this.setDefaultState(stateManager.getDefaultState().with(LIGHT_LEVEL, 0).with(Properties.WATERLOGGED, false).with(SHADER_TYPE, 0));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(Properties.WATERLOGGED).booleanValue()) {
            world.createAndScheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Boolean.TRUE.equals(state.get(Properties.WATERLOGGED)) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        builder.add(LIGHT_LEVEL, Properties.WATERLOGGED, SHADER_TYPE);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (world instanceof ServerWorld && blockEntity != null) {
            dropStack(world, pos, ItemHelpers.blockToItem(this, state, blockEntity));
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        BitsBlockEntity blockEntity = world.getBlockEntity(pos, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
        if (blockEntity == null) return;
        if (itemStack.hasCustomName()) {
            blockEntity.setCustomName(itemStack.getName());
        }
        NbtCompound displayNbt = itemStack.getSubNbt("display");
        if (displayNbt != null && displayNbt.contains("Lore")) {
            blockEntity.readLoreText(displayNbt.getList("Lore", NbtElement.STRING_TYPE));
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BitsBlockEntity(pos, state);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return ItemHelpers.blockToItem(this, state, world.getBlockEntity(pos));
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BlockEntity bruhWhyMustMcBeLikeThisAhhhhh = world.getBlockEntity(pos);
        if (bruhWhyMustMcBeLikeThisAhhhhh instanceof BitsBlockEntity) {
            return ((BitsBlockEntity)bruhWhyMustMcBeLikeThisAhhhhh).shape;
        }
        return VoxelShapes.empty();
    }

}
