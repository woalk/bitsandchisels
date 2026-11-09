package com.woalk.mods.bitsandchisels.blocks;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.woalk.mods.bitsandchisels.utils.BitNbtUtil;
import com.woalk.mods.bitsandchisels.BitsAndChisels;
import com.woalk.mods.bitsandchisels.mixin.CubeVoxelShapeFactory;
import com.woalk.mods.bitsandchisels.render.BitMeshes;
import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public class BitsBlockEntity extends BlockEntity implements RenderDataBlockEntity, Nameable {
    protected BlockState[][][] states;
    @Environment(EnvType.CLIENT)
    protected Mesh mesh;
    protected VoxelShape shape = Shapes.block();
    protected CompoundTag nbtCache;
    private boolean alive = false;
    private UUID structureId = null;
    @Nullable
    private Component name = null;
    @Nullable
    private List<Component> lore = null;

    public BitsBlockEntity(BlockPos pos, BlockState state) {
        this(Blocks.AIR.defaultBlockState(), pos, state, false);
    }

    public BitsBlockEntity(BlockState fillState, BlockPos pos, BlockState state, boolean alive) {
        this(new BlockState[16][16][16], pos, state, alive);
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    states[i][j][k] = fillState;
                }
            }
        }
    }

    public BitsBlockEntity(BlockState[][][] states, BlockPos pos, BlockState state, boolean alive) {
        this(BitsAndChisels.BITS_BLOCK_ENTITY, states, pos, state, alive);
    }

    public BitsBlockEntity(BlockEntityType<? extends BitsBlockEntity> type, BlockPos pos, BlockState state) {
        this(type, new BlockState[16][16][16], pos, state, false);
        final BlockState fillState = Blocks.AIR.defaultBlockState();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    states[i][j][k] = fillState;
                }
            }
        }
    }

    public BitsBlockEntity(BlockEntityType<? extends BitsBlockEntity> type, BlockState[][][] states, BlockPos pos, BlockState state, boolean alive) {
        super(type, pos, state);
        this.states = states;
        this.alive = alive;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!alive) return;
        final var tag = new CompoundTag();
        writeBitNbt(tag);
        for (var entry : tag.entrySet()) {
            if (entry.getValue().getId() == Tag.TAG_BYTE_ARRAY) {
                output.putByteArray(entry.getKey(), entry.getValue().asByteArray().get());
            } else if (entry.getValue().getId() == Tag.TAG_LIST) {
                output.store(
                        entry.getKey(),
                        Codec.list(CompoundTag.CODEC),
                        entry.getValue()
                                .asList()
                                .get()
                                .stream()
                                .map(it -> it.asCompound().get())
                                .toList()
                );
            }
        }
        if (structureId != null) {
            output.store("StructureId", UUIDUtil.CODEC, structureId);
        }
        if (this.name != null) {
            output.store("CustomName", ComponentSerialization.CODEC, this.name);
        }
        if (this.lore != null) {
            output.store("Lore", ItemLore.CODEC, new ItemLore(lore));
        }
    }

    public void writeBitNbt(CompoundTag tag) {
        if (nbtCache == null) {
            BitsAndChisels.LOGGER.error("NbtCache should not be null!");
            BitNbtUtil.write3DBitArray(tag, states);
        } else {
            tag.merge(nbtCache);
        }
    }

    protected CompoundTag extractImportantNbt(@NonNull ValueInput input) {
        final var tag = new CompoundTag();
        final var bits2 = input.getOptionalByteArray("bits_v2").orElse(null);
        final var palette = input.read("palette", Codec.list(CompoundTag.CODEC)).orElse(null);
        if (bits2 != null && palette != null) {
            tag.putByteArray("bits_v2", bits2);
            final var listTag = new ListTag();
            listTag.addAll(palette);
            tag.put("palette", listTag);
        }
        return tag;
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        if (input.contains("CustomName")) {
            this.name = parseCustomNameSafe(input, "CustomName");
        }
        if (input.contains("Lore")) {
            this.lore = input.read("Lore", ItemLore.CODEC).get().lines();
        }
        final var tag = extractImportantNbt(input);
        readBitNbt(tag);
        rebuildShape();
        if (getLevel() != null && getLevel().isClientSide()) {
            postFromClientTag();
        }
        rebuildNbtCache();
        if (input.contains("display")) {
            CompoundTag displayNbt = input.read("display", CompoundTag.CODEC).get();
            if (displayNbt.contains("Name")) {
                this.name = parseComponent(displayNbt.getStringOr("Name", "''"));
            }
            if (displayNbt.contains("Lore")) {
                this.lore = displayNbt.getList("Lore").get()
                        .stream()
                        .map(it -> parseComponent(it.asString().orElse("''")))
                        .toList();
            }
        }
        if (input.contains("StructureId")) {
            structureId = input.read("StructureId", UUIDUtil.CODEC).get();
        }
        alive = true;
    }

    private Component parseComponent(String json) {
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    protected void readBitNbt(CompoundTag tag) {
        BitNbtUtil.read3DBitArray(tag, states);
    }
    
    protected void rebuildNbtCache() {
        CompoundTag c = new CompoundTag();
        BitNbtUtil.write3DBitArray(c, states);
        nbtCache = c;
    }

    public void setState(int x, int y, int z, BlockState state) {
        states[x][y][z] = state;
        alive = true;
    }

    /**
     * Used to replace the backing array, don't call this w/o a good reason.
     */
    public void setStates(BlockState[][][] states) {
        this.states = states;
    }

    public void rebuildServer() {
        rebuildShape();
        rebuildNbtCache();
    }

    public BlockState getState(int x, int y, int z) {
        return states[x][y][z];
    }

    public BlockState[][][] getStates() {
        return states;
    }

    protected void rebuildShape() {
        boolean fullcube = true;
        BitSetDiscreteVoxelShape set = new BitSetDiscreteVoxelShape(16, 16, 16);
        BlockState firststate = states[0][0][0];
        int totalLight = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    BlockState state = states[i][j][k];
                    totalLight += state.getLightEmission();
                    if (!state.isAir()) {
                        set.fill(i, j, k);
                    }
                    if (firststate != state) {
                        fullcube = false;
                    }
                }
            }
        }
        shape = CubeVoxelShapeFactory.getSimpleVoxelShape(set);
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            if (!state.is(BitsAndChisels.BITS_BLOCK)) {
                // skip rebuilding, we are currently building terrain
                return;
            }
            if (fullcube) {
                level.setBlockAndUpdate(worldPosition, states[0][0][0]);
            }
            int targetlight = Mth.clamp((int)(Math.sqrt(totalLight) * 0.0625 /*16/sqrt(4096)*/), 0, 16);
            if (state.getValue(BitsBlock.LIGHT_LEVEL) != targetlight) {
                level.setBlock(worldPosition, state.setValue(BitsBlock.LIGHT_LEVEL, targetlight), 0);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    protected void rebuildMesh() {
        mesh = BitMeshes.createMesh(states, level, worldPosition, null);
    }

    public void postFromClientTag() {
        rebuildMesh();
        Minecraft.getInstance().levelExtractor.setBlocksDirty(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sync() {
        setChanged();
        assert level != null;
        ((ServerLevel) level).getChunkSource().blockChanged(getBlockPos());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public @org.jspecify.annotations.Nullable Object getRenderData() {
        if (this.mesh == null && this.states != null && this.level != null) {
            rebuildMesh();
        }
        return this.mesh;
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : Component.translatable("block.bitsandchisels.bits_block");
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    public void setCustomName(@Nullable Component customName) {
        this.name = customName;
    }

    public @Nullable List<Component> getLoreText() {
        return lore;
    }

    public void setLoreText(List<Component> loreText) {
        this.lore = loreText;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        this.name = input.get(DataComponents.CUSTOM_NAME);
        ItemLore itemLore = input.get(DataComponents.LORE);
        if (itemLore != null && !itemLore.lines().isEmpty()) {
            this.lore = itemLore.lines();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.CUSTOM_NAME, this.name);
        if (this.lore != null) {
            builder.set(DataComponents.LORE, new ItemLore(this.lore));
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("CustomName");
        output.discard("Lore");
    }

    public UUID getStructureId() {
        return structureId;
    }

    public void setStructureId(UUID structureId) {
        this.structureId = structureId;
    }
}
