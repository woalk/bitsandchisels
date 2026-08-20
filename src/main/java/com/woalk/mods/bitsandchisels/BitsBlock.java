package com.woalk.mods.bitsandchisels;

import com.mojang.serialization.MapCodec;
import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem;
import com.woalk.mods.bitsandchisels.registry.ModItems;
import com.woalk.mods.bitsandchisels.utils.DirectionUtilsKt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BitsBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    private static final MapCodec<BitsBlock> CODEC = simpleCodec(BitsBlock::new);

    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 16);
    public static final IntegerProperty SHADER_TYPE = IntegerProperty.create("shader_type", 0, 16);

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public BitsBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(stateDefinition.any().setValue(LIGHT_LEVEL, 0).setValue(BlockStateProperties.WATERLOGGED, false).setValue(SHADER_TYPE, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED).booleanValue()) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED)) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(LIGHT_LEVEL, BlockStateProperties.WATERLOGGED, SHADER_TYPE);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        BitsBlockEntity blockEntity = world.getBlockEntity(pos, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
        if (!world.isClientSide && blockEntity != null) {
            ItemStack dropStack = getDropItem(world, pos, state, blockEntity, player, true);
            if (dropStack != null) {
                popResource(world, pos, dropStack);
            }
        }
        return playerWillDestroyBlock(world, pos, state, player);
    }

    protected BlockState playerWillDestroyBlock(Level world, BlockPos pos, BlockState state, Player player) {
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Nullable
    public ItemStack getDropItem(
            @NotNull Level world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @Nullable BitsBlockEntity blockEntity,
            @Nullable Player player,
            boolean breakBlock
    ) {
        if (blockEntity == null) {
            blockEntity = world.getBlockEntity(pos, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
            if (blockEntity == null) {
                return null;
            }
        }
        if (blockEntity.getStructureId() != null && (player == null || !player.isShiftKeyDown())) {
            return tryGetStructure(world, pos, blockEntity, player, breakBlock);
        } else {
            // no structure ID, or drop not successful
            final var stack = ItemHelpers.blockToItem(this, state, blockEntity);
            if (player != null) {
                final var rotation = DirectionUtilsKt.getNearestDirectionForVector(pos.subtract(player.blockPosition()));
                final var currentEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                CompoundTag nbt;
                if (currentEntityData != null) {
                    nbt = currentEntityData.copyTag();
                } else {
                    nbt = new CompoundTag();
                }
                nbt.putInt(BitStructureItem.NBT_ROTATION, rotation.get3DDataValue());
                BlockItem.setBlockEntityData(stack, BitsAndChisels.BITS_BLOCK_ENTITY, nbt);
            }
            return stack;
        }
    }

    /** @return {@code true} on success, {@code false} if a normal {@code BitsBlockItem} should be dropped instead. */
    private boolean tryDropStructure(Level world, BlockPos pos, BitsBlockEntity blockEntity) {
        final var stack = tryGetStructure(world, pos, blockEntity, null, true);
        if (stack == null) {
            return false;
        }
        popResource(world, pos, stack);
        return true;
    }

    @Nullable
    private ItemStack tryGetStructure(Level world, BlockPos pos, BitsBlockEntity blockEntity, @Nullable Player player, boolean breakBlock) {
        int minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
        for (var offset : BitStructureItem.Companion.createOffsets(BitStructureItem.MAX_DIAMETER * 2 - 1, -BitStructureItem.MAX_DIAMETER + 1)) {
            final var pos2 = pos.subtract(offset);
            final var blockEntity2 =
                    world.getBlockEntity(pos2, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
            if (blockEntity2 != null && blockEntity.getStructureId() != null && blockEntity.getStructureId().equals(blockEntity2.getStructureId())) {
                if (minX > pos2.getX()) minX = pos2.getX();
                if (minY > pos2.getY()) minY = pos2.getY();
                if (minZ > pos2.getZ()) minZ = pos2.getZ();
            }
        }
        var pos1 = new BlockPos(minX, minY, minZ);
        final var nbts = new ListTag();
        boolean moreThanPos1 = false;
        for (var offset : BitStructureItem.Companion.getOFFSETS()) {
            final var pos2 = pos1.offset(offset);
            final var blockEntity2 =
                    world.getBlockEntity(pos2, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
            final var nbt = new CompoundTag();
            if (blockEntity2 != null && blockEntity.getStructureId() != null && blockEntity.getStructureId().equals(blockEntity2.getStructureId())) {
                blockEntity2.writeBitNbt(nbt);
                nbt.putInt("shader_type", world.getBlockState(pos2).getValue(BitsBlock.SHADER_TYPE));
                if (breakBlock) {
                    world.destroyBlock(pos2, false);
                }
                if (!offset.equals(Vec3i.ZERO)) {
                    moreThanPos1 = true;
                }
            }
            nbts.add(nbt);
        }
        if (!moreThanPos1) return null;
        final var stack = ModItems.INSTANCE.getBitStructureItem().getDefaultInstance();
        final var customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag nbt;
        if (customData != null) {
            nbt = customData.copyTag();
        } else {
            nbt = new CompoundTag();
        }
        nbt.put(BitStructureItem.NBT_BIT_BLOCK_TAGS, nbts);
        if (player != null) {
            final var rotation = DirectionUtilsKt.getNearestDirectionForVector(pos.subtract(player.blockPosition()));
            nbt.put(
                    BitStructureItem.NBT_ROTATION,
                    IntTag.valueOf(rotation.get3DDataValue())
            );
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        stack.applyComponents(blockEntity.components());
        return stack;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        BitsBlockEntity blockEntity = world.getBlockEntity(pos, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
        if (blockEntity == null) return;
        final var data = itemStack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains("StructureId")) {
            blockEntity.setStructureId(data.copyTag().getUUID("StructureId"));
        }
        final var name = itemStack.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            blockEntity.setCustomName(name);
        }
        final var lore = itemStack.get(DataComponents.LORE);
        if (lore != null) {
            blockEntity.applyComponents(blockEntity.components(), DataComponentPatch.builder().set(DataComponents.LORE, lore).build());
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BitsBlockEntity(pos, state);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerLevel)) {
            final var bitStack = pickBitClient(world, pos, state);
            if (bitStack != null) return bitStack;
        }
        final var blockEntity = world.getBlockEntity(pos, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
        if (blockEntity != null) {
            return ItemHelpers.blockToItem(this, state, blockEntity);
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Environment(EnvType.CLIENT)
    private ItemStack pickBitClient(BlockGetter world, BlockPos pos, BlockState state) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player != null && Screen.hasShiftDown()) {
            if (player.getMainHandItem().is(TagKey.create(Registries.ITEM, new ResourceLocation(BitsAndChisels.MODID, "chisel")))) {
                HitResult hit = client.hitResult;
                if (hit != null && hit.getType() == HitResult.Type.BLOCK && world instanceof Level) {
                    Direction direction = ((BlockHitResult) hit).getDirection();
                    int x = (int) Math.floor(((hit.getLocation().x() - pos.getX()) * 16) + (direction.getStepX() * -0.5d));
                    int y = (int) Math.floor(((hit.getLocation().y() - pos.getY()) * 16) + (direction.getStepY() * -0.5d));
                    int z = (int) Math.floor(((hit.getLocation().z() - pos.getZ()) * 16) + (direction.getStepZ() * -0.5d));
                    BlockState bit = BitUtils.getBit((Level) world, pos, x, y, z);
                    if (BitUtils.exists(bit)) {
                        return BitUtils.getBitItemStack(bit);
                    }
                }
            }
        } else if (world instanceof Level) {
            final var blockEntity = world.getBlockEntity(pos, BitsAndChisels.BITS_BLOCK_ENTITY).orElse(null);
            if (blockEntity != null) {
                if (blockEntity.getStructureId() != null && (player == null || !player.isShiftKeyDown())) {
                    return tryGetStructure((Level) world, pos, blockEntity, player, false);
                } else {
                    final var stack = ItemHelpers.blockToItem(this, state, blockEntity);
                    if (player != null) {
                        final var rotation = DirectionUtilsKt.getNearestDirectionForVector(pos.subtract(player.blockPosition()));
                        final var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                        CompoundTag nbt;
                        if (data != null) {
                            nbt = data.copyTag();
                        } else {
                            nbt = new CompoundTag();
                        }
                        nbt.putInt(BitStructureItem.NBT_ROTATION, rotation.get3DDataValue());
                        BlockItem.setBlockEntityData(stack, BitsAndChisels.BITS_BLOCK_ENTITY, nbt);
                    }
                    return stack;
                }
            }
        }
        return null;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        BlockEntity bruhWhyMustMcBeLikeThisAhhhhh = world.getBlockEntity(pos);
        if (bruhWhyMustMcBeLikeThisAhhhhh instanceof BitsBlockEntity) {
            return ((BitsBlockEntity)bruhWhyMustMcBeLikeThisAhhhhh).shape;
        }
        return Shapes.empty();
    }

}
