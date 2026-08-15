package com.woalk.mods.bitsandchisels.chisel;

import com.woalk.mods.bitsandchisels.BitsAndChisels;
import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.api.client.AirSwingItem;
import com.woalk.mods.bitsandchisels.api.client.RedBoxCallback;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import io.netty.buffer.Unpooled;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class SmartChisel extends TieredItem implements ServerPlayNetworking.PlayChannelHandler, AirSwingItem {
    public static final ResourceLocation PACKET_ID = new ResourceLocation("bitsandchisels", "smart_chisel_packet");

    BlockPos block = null;

    int bitx = 0;
    int bity = 0;
    int bitz = 0;

    boolean haspos1 = false;

    private long lastBreakTick = 0;

    public SmartChisel(Properties settings) {
        super(Tiers.IRON, settings);
        ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, this);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            Item i = player.getItemInHand(hand).getItem();
            if (i instanceof SmartChisel) {
                if (!world.isClientSide) return InteractionResult.FAIL;
                return ((SmartChisel)i).interactBreakBlockClient(player, world, pos);
            }
            return InteractionResult.PASS;
        });
    }

    public void initClient() {
        RedBoxCallback.EVENT.register((redBoxDrawer, matrixStack, vertexConsumer, worldoffsetx, worldoffsety, worldoffsetz) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player.getMainHandItem().getItem() == BitsAndChisels.SMART_CHISEL) {
                if (haspos1) {
                    HitResult hit = client.hitResult;
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        Direction direction = ((BlockHitResult)hit).getDirection();
                        BlockPos pos = ((BlockHitResult)hit).getBlockPos();
                        if (pos.distSqr(block) > 9) return;
                        int x = (int) Math.floor(((hit.getLocation().x() - pos.getX()) * 16) + (direction.getStepX() * -0.5d));
                        int y = (int) Math.floor(((hit.getLocation().y() - pos.getY()) * 16) + (direction.getStepY() * -0.5d));
                        int z = (int) Math.floor(((hit.getLocation().z() - pos.getZ()) * 16) + (direction.getStepZ() * -0.5d));
                        
                        int blockXOffset = pos.getX() - block.getX();
                        int blockYOffset = pos.getY() - block.getY();
                        int blockZOffset = pos.getZ() - block.getZ();

                        int minx = Math.min(bitx, blockXOffset * 16 + x);
                        int miny = Math.min(bity, blockYOffset * 16 + y);
                        int minz = Math.min(bitz, blockZOffset * 16 + z);

                        int maxx = Math.max(bitx, blockXOffset * 16 + x);
                        int maxy = Math.max(bity, blockYOffset * 16 + y);
                        int maxz = Math.max(bitz, blockZOffset * 16 + z);
                        
                        redBoxDrawer.drawRedBox(matrixStack, vertexConsumer, block, minx, miny, minz, maxx + 1, maxy + 1, maxz + 1, worldoffsetx, worldoffsety, worldoffsetz);
                    }
                } else {
                    HitResult hit = client.hitResult;
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        Direction direction = ((BlockHitResult)hit).getDirection();
                        BlockPos pos = ((BlockHitResult)hit).getBlockPos();
                        int x = (int) Math.floor(((hit.getLocation().x() - pos.getX()) * 16) + (direction.getStepX() * -0.5d));
                        int y = (int) Math.floor(((hit.getLocation().y() - pos.getY()) * 16) + (direction.getStepY() * -0.5d));
                        int z = (int) Math.floor(((hit.getLocation().z() - pos.getZ()) * 16) + (direction.getStepZ() * -0.5d));
                        redBoxDrawer.drawRedBox(matrixStack, vertexConsumer, pos, x, y, z, x + 1, y + 1, z + 1, worldoffsetx, worldoffsety, worldoffsetz);
                    }
                }
            }
        });
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        if (!Permissions.check(player, ModPermissions.USE_CHISEL_SMART, true)) return;
        BlockPos pos = buf.readBlockPos();
        int x1 = buf.readInt();
        int y1 = buf.readInt();
        int z1 = buf.readInt();
        int x2 = buf.readInt();
        int y2 = buf.readInt();
        int z2 = buf.readInt();
        server.execute(() -> {
            if (
                player.getMainHandItem().getItem() == BitsAndChisels.SMART_CHISEL &&
                player.blockPosition().distSqr(pos) < 81 &&
                x1 <= x2 && y1 <= y2 && z1 <= z2 &&
                ((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)) + ((z2 - z1) * (z2 - z1)) <= 1000000
            ) {
                BitUtils.attemptBreakRegion(player, pos, x1, y1, z1, x2, y2, z2);
            }
        });
    }

    public InteractionResult interactBreakBlockClient(Player player, Level world, BlockPos pos) {
        if (getTime() - lastBreakTick < 5) return InteractionResult.CONSUME;
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;

        if (hit.getType() == HitResult.Type.BLOCK) {
            Direction direction = ((BlockHitResult)hit).getDirection();
            int x = (int) Math.floor(((hit.getLocation().x() - pos.getX()) * 16) + (direction.getStepX() * -0.5d));
            int y = (int) Math.floor(((hit.getLocation().y() - pos.getY()) * 16) + (direction.getStepY() * -0.5d));
            int z = (int) Math.floor(((hit.getLocation().z() - pos.getZ()) * 16) + (direction.getStepZ() * -0.5d));
            lastBreakTick = getTime();
            if (BitUtils.exists(BitUtils.getBit(world, pos, x, y, z))) {
                if (!haspos1 || pos.distSqr(block) > 9) {
                    block = pos;
                    bitx = x;
                    bity = y;
                    bitz = z;
                    haspos1 = true;
                } else {
                    FriendlyByteBuf passedData = new FriendlyByteBuf(Unpooled.buffer());

                    int blockXOffset = pos.getX() - block.getX();
                    int blockYOffset = pos.getY() - block.getY();
                    int blockZOffset = pos.getZ() - block.getZ();

                    int minx = Math.min(bitx, blockXOffset * 16 + x);
                    int miny = Math.min(bity, blockYOffset * 16 + y);
                    int minz = Math.min(bitz, blockZOffset * 16 + z);

                    int maxx = Math.max(bitx, blockXOffset * 16 + x);
                    int maxy = Math.max(bity, blockYOffset * 16 + y);
                    int maxz = Math.max(bitz, blockZOffset * 16 + z);

                    passedData.writeBlockPos(block);
                    passedData.writeInt(minx);
                    passedData.writeInt(miny);
                    passedData.writeInt(minz);
                    passedData.writeInt(maxx);
                    passedData.writeInt(maxy);
                    passedData.writeInt(maxz);
                    ClientPlayNetworking.send(PACKET_ID, passedData);
                    haspos1 = false;
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.CONSUME;
    }

    private static long getTime() {
        return System.currentTimeMillis() / 50;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void airSwing(LocalPlayer player, ItemStack stack) {
        this.haspos1 = false;
    }
}
