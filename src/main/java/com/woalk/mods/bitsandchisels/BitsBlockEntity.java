package com.woalk.mods.bitsandchisels;

import com.woalk.mods.bitsandchisels.mixin.CubeVoxelShapeFactory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BitsBlockEntity extends BlockEntity implements RenderAttachmentBlockEntity, Nameable {
    protected BlockState[][][] states;
    @Environment(EnvType.CLIENT)
    protected Mesh mesh;
    protected VoxelShape shape = Shapes.block();
    protected CompoundTag nbtCache;
    private boolean alive = false;
    private Component customName = null;
    private List<Component> loreText = null;
    private UUID structureId = null;

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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!alive) return;
        writeBitNbt(tag);
        if (customName != null || loreText != null) {
            CompoundTag displayNbt = new CompoundTag();
            writeNameNbt(displayNbt);
            tag.put("display", displayNbt);
        }
        if (structureId != null) {
            tag.putUUID("StructureId", structureId);
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

    public void writeNameNbt(CompoundTag displayNbt) {
        if (customName != null) {
            displayNbt.putString("Name", Component.Serializer.toJson(this.customName));
        }
        if (loreText != null) {
            ListTag loreNbt = new ListTag();
            loreText.forEach((text) -> loreNbt.add(StringTag.valueOf(oldChiselItemCompat(Component.Serializer.toJson(text)))));
            displayNbt.put("Lore", loreNbt);
        }
    }

    private String oldChiselItemCompat(String json) {
        if (json.equals("{\"text\":\"(+NBT)\"}")) {
            return "\"(+NBT)\"";
        } else return json;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readBitNbt(tag);
        rebuildShape();
        if (getLevel() != null && getLevel().isClientSide) {
            postFromClientTag();
        }
        rebuildNbtCache();
        if (tag.contains("display")) {
            CompoundTag displayNbt = tag.getCompound("display");
            if (displayNbt.contains("Name")) {
                customName = Component.Serializer.fromJson(displayNbt.getString("Name"));
            }
            if (displayNbt.contains("Lore")) {
                ListTag loreNbt = displayNbt.getList("Lore", ListTag.TAG_STRING);
                readLoreText(loreNbt);
            }
        }
        if (tag.contains("StructureId")) {
            structureId = tag.getUUID("StructureId");
        }
        alive = true;
    }

    protected void readBitNbt(CompoundTag tag) {
        BitNbtUtil.read3DBitArray(tag, states);
    }

    public void readLoreText(ListTag loreNbt) {
        loreText = new ArrayList<>(loreNbt.size());
        loreNbt.forEach((textNbt) -> loreText.add(Component.Serializer.fromJson(textNbt.getAsString())));
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
        if (level != null && !level.isClientSide) {
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
        Minecraft.getInstance().levelRenderer.setBlocksDirty(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
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
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public @Nullable Object getRenderAttachmentData() {
        return mesh;
    }

    @Override
    public Component getName() {
        return null;
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return Nameable.super.getCustomName();
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
    }

    public void setLoreText(List<Component> loreText) {
        this.loreText = loreText;
    }

    public UUID getStructureId() {
        return structureId;
    }

    public void setStructureId(UUID structureId) {
        this.structureId = structureId;
    }
}
