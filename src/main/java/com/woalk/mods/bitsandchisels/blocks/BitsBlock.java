package com.woalk.mods.bitsandchisels.blocks;

import com.mojang.serialization.MapCodec;
import com.woalk.mods.bitsandchisels.BitsAndChisels;
import com.woalk.mods.bitsandchisels.registry.ModTagKeys;
import com.woalk.mods.bitsandchisels.render.RedBoxPipeline;
import com.woalk.mods.bitsandchisels.utils.ItemHelpers;
import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.registry.ModItems;
import com.woalk.mods.bitsandchisels.utils.DirectionUtilsKt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

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
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(BlockStateProperties.WATERLOGGED).booleanValue() && !level.isClientSide()) {
            ((ServerLevel) level).scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
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
        BitsBlockEntity blockEntity = world.getBlockEntity(pos) instanceof BitsBlockEntity be ? be : null;
        if (!world.isClientSide() && blockEntity != null) {
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

    public void initServer() {
        PlayerPickItemEvents.BLOCK.register((player, pos, state, _) -> {
            if (state.is(this)) {
                return pickBitServer(player, pos, state);
            }
            return null;
        });
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
            if (world.getBlockEntity(pos) instanceof BitsBlockEntity be) {
                blockEntity = be;
            } else {
                return null;
            }
        }
        if (blockEntity.getStructureId() != null && (player == null || !player.isShiftKeyDown())) {
            final var structureStack = tryGetStructure(world, pos, blockEntity, player, breakBlock);
            if (structureStack != null) {
                return structureStack;
            } // else
        } // else ^+
        {
            // no structure ID, or drop not successful
            final var stack = ItemHelpers.blockToItem(this, state, blockEntity);
            if (player != null) {
                final var rotation = DirectionUtilsKt.getNearestDirectionForVector(pos.subtract(player.blockPosition()));
                final var currentEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                CompoundTag nbt;
                if (currentEntityData != null) {
                    nbt = currentEntityData.copyTagWithoutId();
                } else {
                    nbt = new CompoundTag();
                }
                nbt.putInt(BitStructureItem.NBT_ROTATION, rotation.get3DDataValue());
                stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(getBlockEntityType(), nbt));
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
        stack.applyComponents(blockEntity.collectComponents());
        final var name = blockEntity.getCustomName();
        var lore = blockEntity.getLoreText();
        final var patch = DataComponentPatch.builder();
        if (name != null) {
            patch.set(DataComponents.CUSTOM_NAME, name);
        }
        if (lore != null) {
            lore = new ArrayList<>(lore);
            if (lore.getLast().getString().matches("^\\(?\\d+, \\d+, \\d+\\)?$")) {
                lore.removeLast();
            }
            patch.set(DataComponents.LORE, new ItemLore(lore));
        }
        stack.applyComponents(patch.build());
        return stack;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!(world.getBlockEntity(pos) instanceof BitsBlockEntity blockEntity)) return;
        final var data = itemStack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            final var tag = data.copyTag();
            if (tag.contains("StructureId")) {
                blockEntity.setStructureId(tag.read("StructureId", UUIDUtil.CODEC).get());
            }
        }
        final var name = itemStack.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            blockEntity.setCustomName(name);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BitsBlockEntity(pos, state);
    }

    protected BlockEntityType<? extends BitsBlockEntity> getBlockEntityType() {
        return BitsAndChisels.BITS_BLOCK_ENTITY;
    }

    @Override
    protected @NonNull ItemStack getCloneItemStack(LevelReader world, @NonNull BlockPos pos, @NonNull BlockState state, boolean includeData) {
        if (world.isClientSide()) {
            final var bitStack = pickBitClient(world, pos, state);
            if (bitStack != null) return bitStack;
        }
        final var blockEntity = world.getBlockEntity(pos) instanceof BitsBlockEntity be ? be : null;
        if (blockEntity != null) {
            return ItemHelpers.blockToItem(this, state, blockEntity);
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Deprecated
    @Environment(EnvType.CLIENT)
    private ItemStack pickBitClient(BlockGetter world, BlockPos pos, BlockState state) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player != null && client.hasShiftDown()) {
            if (player.getMainHandItem().is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(BitsAndChisels.MODID, "chisel")))) {
                HitResult hit = client.hitResult;
                if (hit != null && hit.getType() == HitResult.Type.BLOCK && world instanceof Level) {
                    final var bitPos = RedBoxPipeline.Shape.bitPosition((BlockHitResult) hit);
                    BlockState bit = BitUtils.getBit((Level) world, pos, bitPos.getX(), bitPos.getY(), bitPos.getZ());
                    if (BitUtils.exists(bit)) {
                        return BitUtils.getBitItemStack(bit);
                    }
                }
            }
        } else if (world instanceof Level) {
            final var blockEntity = world.getBlockEntity(pos) instanceof BitsBlockEntity be ? be : null;
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
                            nbt = data.copyTagWithoutId();
                        } else {
                            nbt = new CompoundTag();
                        }
                        nbt.putInt(BitStructureItem.NBT_ROTATION, rotation.get3DDataValue());
                        stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(getBlockEntityType(), nbt));
                    }
                    return stack;
                }
            }
        }
        return null;
    }

    private @Nullable ItemStack pickBitServer(@NonNull ServerPlayer player, @NonNull BlockPos pos, @NonNull BlockState state) {
        final ServerLevel world = player.level();
        if (player.isShiftKeyDown()) {
            if (player.getMainHandItem().is(ModTagKeys.INSTANCE.getChisel())) {
                HitResult hit = player.pick(player.blockInteractionRange(), 0.0f, false);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    final var bitPos = RedBoxPipeline.Shape.bitPosition((BlockHitResult) hit);
                    BlockState bit = BitUtils.getBit(world, pos, bitPos.getX(), bitPos.getY(), bitPos.getZ());
                    if (BitUtils.exists(bit)) {
                        return BitUtils.getBitItemStack(bit);
                    }
                }
            }
        } else {
            final var blockEntity = world.getBlockEntity(pos) instanceof BitsBlockEntity be ? be : null;
            if (blockEntity != null) {
                if (blockEntity.getStructureId() != null) {
                    final var structureStack = tryGetStructure(world, pos, blockEntity, player, false);
                    if (structureStack != null) {
                        return structureStack;
                    } // else
                } // else ^+
                {
                    final var stack = ItemHelpers.blockToItem(this, state, blockEntity);
                    final var rotation = DirectionUtilsKt.getNearestDirectionForVector(pos.subtract(player.blockPosition()));
                    final var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                    CompoundTag nbt;
                    if (data != null) {
                        nbt = data.copyTagWithoutId();
                    } else {
                        nbt = new CompoundTag();
                    }
                    nbt.putInt(BitStructureItem.NBT_ROTATION, rotation.get3DDataValue());
                    stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(getBlockEntityType(), nbt));
                    return stack;
                }
            }
        }
        return null;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (world.getBlockEntity(pos) instanceof BitsBlockEntity be) {
            return be.shape;
        }
        return Shapes.empty();
    }

}
