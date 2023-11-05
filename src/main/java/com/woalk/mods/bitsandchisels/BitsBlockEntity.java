package com.woalk.mods.bitsandchisels;

import com.woalk.mods.bitsandchisels.mixin.SimpleVoxelShapeFactory;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.List;

public class BitsBlockEntity extends BlockEntity implements RenderAttachmentBlockEntity, Nameable {
    protected BlockState[][][] states;
    @Environment(EnvType.CLIENT)
    protected Mesh mesh;
    protected VoxelShape shape = VoxelShapes.fullCube();
    protected NbtCompound nbtCache;
    private boolean alive = false;
    private Text customName = null;
    private List<Text> loreText = null;

    public BitsBlockEntity(BlockPos pos, BlockState state) {
        this(Blocks.AIR.getDefaultState(), pos, state, false);
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
        super(BitsAndChisels.BITS_BLOCK_ENTITY, pos, state);
        this.states = states;
        this.alive = alive;
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        if (!alive) return;
        writeBitNbt(tag);
        if (customName != null || loreText != null) {
            NbtCompound displayNbt = new NbtCompound();
            writeNameNbt(displayNbt);
            tag.put("display", displayNbt);
        }
    }

    public void writeBitNbt(NbtCompound tag) {
        if (nbtCache == null) {
            BitsAndChisels.LOGGER.error("NbtCache should not be null!");
            BitNbtUtil.write3DBitArray(tag, states);
        } else {
            tag.copyFrom(nbtCache);
        }
    }

    public void writeNameNbt(NbtCompound displayNbt) {
        if (customName != null) {
            displayNbt.putString("Name", Text.Serializer.toJson(this.customName));
        }
        if (loreText != null) {
            NbtList loreNbt = new NbtList();
            loreText.forEach((text) -> loreNbt.add(NbtString.of(oldChiselItemCompat(Text.Serializer.toJson(text)))));
            displayNbt.put("Lore", loreNbt);
        }
    }

    private String oldChiselItemCompat(String json) {
        if (json.equals("{\"text\":\"(+NBT)\"}")) {
            return "\"(+NBT)\"";
        } else return json;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        readBitNbt(tag);
        rebuildShape();
        if (getWorld() != null && getWorld().isClient) {
            postFromClientTag();
        }
        rebuildNbtCache();
        if (tag.contains("display")) {
            NbtCompound displayNbt = tag.getCompound("display");
            if (displayNbt.contains("Name")) {
                customName = Text.Serializer.fromJson(displayNbt.getString("Name"));
            }
            if (displayNbt.contains("Lore")) {
                NbtList loreNbt = displayNbt.getList("Lore", NbtList.STRING_TYPE);
                readLoreText(loreNbt);
            }
        }
        alive = true;
    }

    protected void readBitNbt(NbtCompound tag) {
        BitNbtUtil.read3DBitArray(tag, states);
    }

    public void readLoreText(NbtList loreNbt) {
        loreText = new ArrayList<>(loreNbt.size());
        loreNbt.forEach((textNbt) -> loreText.add(Text.Serializer.fromJson(textNbt.asString())));
    }
    
    protected void rebuildNbtCache() {
        NbtCompound c = new NbtCompound();
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
        BitSetVoxelSet set = new BitSetVoxelSet(16, 16, 16);
        BlockState firststate = states[0][0][0];
        int totalLight = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    BlockState state = states[i][j][k];
                    totalLight += state.getLuminance();
                    if (!state.isAir()) {
                        set.set(i, j, k);
                    }
                    if (firststate != state) {
                        fullcube = false;
                    }
                }
            }
        }
        shape = SimpleVoxelShapeFactory.getSimpleVoxelShape(set);
        if (world != null) {
            BlockState state = world.getBlockState(pos);
            if (fullcube) {
                world.setBlockState(pos, states[0][0][0]);
            }
            int targetlight = MathHelper.clamp((int)(Math.sqrt(totalLight) * 0.0625 /*16/sqrt(4096)*/), 0, 16);
            if (state.get(BitsBlock.LIGHT_LEVEL) != targetlight) {
                world.setBlockState(pos, state.with(BitsBlock.LIGHT_LEVEL, targetlight), 0);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    protected void rebuildMesh() {
        mesh = BitMeshes.createMesh(states, world, pos);
    }

    public void postFromClientTag() {
        rebuildMesh();
        MinecraftClient.getInstance().worldRenderer.scheduleBlockRenders(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this, be -> ((BitsBlockEntity)be).createNbt());
    }

    public void sync() {
        markDirty();
        ((ServerWorld) world).getChunkManager().markForUpdate(getPos());
    }

    // Begin crimes agains modding
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        sync();
        return new NbtCompound();
    }
    // End crimes agains modding

    @Environment(EnvType.CLIENT)
    @Override
    public @Nullable Object getRenderAttachmentData() {
        return mesh;
    }

    @Override
    public Text getName() {
        return null;
    }

    @Nullable
    @Override
    public Text getCustomName() {
        return Nameable.super.getCustomName();
    }

    public void setCustomName(Text customName) {
        this.customName = customName;
    }

    public void setLoreText(List<Text> loreText) {
        this.loreText = loreText;
    }
}
