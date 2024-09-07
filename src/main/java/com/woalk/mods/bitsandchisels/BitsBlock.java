package com.woalk.mods.bitsandchisels;

import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.blocks.BitStructureModel;
import com.woalk.mods.bitsandchisels.registry.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
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
        BitsBlockEntity blockEntity = world.getBlockEntity(pos, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
        if (!world.isClient && blockEntity != null) {
            if (blockEntity.getStructureId() == null || player.isSneaking() ||
                    !tryDropStructure(world, pos, blockEntity)) {
                // no structure ID, or drop not successful
                dropStack(world, pos, ItemHelpers.blockToItem(this, state, blockEntity));
            }
        }
    }

    /** @return {@code true} on success, {@code false} if a normal {@code BitsBlockItem} should be dropped instead. */
    private boolean tryDropStructure(World world, BlockPos pos, BitsBlockEntity blockEntity) {
        final var OFFSETS = BitStructureModel.Companion.getOFFSETS();
        int minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
        for (var offset : OFFSETS) {
            final var pos2 = pos.subtract(offset);
            final var blockEntity2 =
                    world.getBlockEntity(pos2, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
            if (blockEntity2 != null && blockEntity2.getStructureId().equals(blockEntity.getStructureId())) {
                if (minX > pos2.getX()) minX = pos2.getX();
                if (minY > pos2.getY()) minY = pos2.getY();
                if (minZ > pos2.getZ()) minZ = pos2.getZ();
            }
        }
        var pos1 = new BlockPos(minX, minY, minZ);
        final var nbts = new NbtList();
        boolean moreThanPos1 = false;
        for (var offset : OFFSETS) {
            final var pos2 = pos1.add(offset);
            final var blockEntity2 =
                    world.getBlockEntity(pos2, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
            final var nbt = new NbtCompound();
            if (blockEntity2 != null && blockEntity2.getStructureId().equals(blockEntity.getStructureId())) {
                blockEntity2.writeBitNbt(nbt);
                world.breakBlock(pos2, false);
                if (!offset.equals(Vec3i.ZERO)) {
                    moreThanPos1 = true;
                }
            }
            nbts.add(nbt);
        }
        if (!moreThanPos1) return false;
        final var stack = ModItems.INSTANCE.getBitStructureItem().getDefaultStack();
        stack.setSubNbt(BitStructureModel.NBT_BIT_BLOCK_TAGS, nbts);
        dropStack(world, pos, stack);
        return true;
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
        if (!(world instanceof ServerWorld)) {
            final var bitStack = pickBitClient(world, pos, state);
            if (bitStack != null) return bitStack;
        }
        return ItemHelpers.blockToItem(this, state, world.getBlockEntity(pos));
    }

    @Environment(EnvType.CLIENT)
    private ItemStack pickBitClient(BlockView world, BlockPos pos, BlockState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player != null && Screen.hasShiftDown() &&
                player.getMainHandStack().isIn(TagKey.of(Registry.ITEM_KEY, new Identifier(BitsAndChisels.MODID, "chisel")))) {
            HitResult hit = client.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK && world instanceof World) {
                Direction direction = ((BlockHitResult) hit).getSide();
                int x = (int) Math.floor(((hit.getPos().getX() - pos.getX()) * 16) + (direction.getOffsetX() * -0.5d));
                int y = (int) Math.floor(((hit.getPos().getY() - pos.getY()) * 16) + (direction.getOffsetY() * -0.5d));
                int z = (int) Math.floor(((hit.getPos().getZ() - pos.getZ()) * 16) + (direction.getOffsetZ() * -0.5d));
                BlockState bit = BitUtils.getBit((World) world, pos, x, y, z);
                if (BitUtils.exists(bit)) {
                    return BitUtils.getBitItemStack(bit);
                }
            }
        }
        return null;
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
