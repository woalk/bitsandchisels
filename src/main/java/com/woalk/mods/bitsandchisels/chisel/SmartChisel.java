package com.woalk.mods.bitsandchisels.chisel;

import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.api.client.AirSwingItem;
import com.woalk.mods.bitsandchisels.api.client.RedBoxCallback;
import com.woalk.mods.bitsandchisels.networking.SmartChiselPacket;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import com.woalk.mods.bitsandchisels.render.RedBoxPipeline;
import io.netty.buffer.Unpooled;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SmartChisel extends Item implements ServerPlayNetworking.PlayPayloadHandler<SmartChiselPacket>, AirSwingItem {
    BlockPos block = null;

    int bitx = 0;
    int bity = 0;
    int bitz = 0;

    boolean haspos1 = false;

    private long lastBreakTick = 0;

    public SmartChisel(Properties settings) {
        super(settings);
        ServerPlayNetworking.registerGlobalReceiver(SmartChiselPacket.TYPE, this);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            Item i = player.getItemInHand(hand).getItem();
            if (i instanceof SmartChisel) {
                if (!world.isClientSide()) return InteractionResult.FAIL;
                return ((SmartChisel)i).interactBreakBlockClient(player, world, pos);
            }
            return InteractionResult.PASS;
        });
    }

    public void initClient() {
        RedBoxPipeline.registerExtraction((_, hit) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.player.getMainHandItem().is(this) && hit != null) {
                final var pos1 = haspos1 ? new RedBoxPipeline.Shape.BitSelection(block, bitx, bity, bitz) : null;
                final var shape = RedBoxPipeline.Shape.multiBit(hit, pos1);
                if (shape != null) {
                    return new RedBoxPipeline.State(shape);
                }
            }
            return null;
        });
    }

    @Override
    public void receive(SmartChiselPacket payload, ServerPlayNetworking.Context context) {
        if (!Permissions.check(context.player(), ModPermissions.USE_CHISEL_SMART, true)) return;
        BlockPos pos = payload.getBlockPos();
        int x1 = payload.getBitPos().getX();
        int y1 = payload.getBitPos().getY();
        int z1 = payload.getBitPos().getZ();
        int x2 = payload.getBitPos2().getX();
        int y2 = payload.getBitPos2().getY();
        int z2 = payload.getBitPos2().getZ();
        context.server().execute(() -> {
            if (
                context.player().getMainHandItem().getItem() == this &&
                context.player().blockPosition().distSqr(pos) < 81 &&
                x1 <= x2 && y1 <= y2 && z1 <= z2 &&
                ((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)) + ((z2 - z1) * (z2 - z1)) <= 1000000
            ) {
                BitUtils.attemptBreakRegion(context.player(), pos, x1, y1, z1, x2, y2, z2);
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
                    int blockXOffset = pos.getX() - block.getX();
                    int blockYOffset = pos.getY() - block.getY();
                    int blockZOffset = pos.getZ() - block.getZ();

                    int minx = Math.min(bitx, blockXOffset * 16 + x);
                    int miny = Math.min(bity, blockYOffset * 16 + y);
                    int minz = Math.min(bitz, blockZOffset * 16 + z);

                    int maxx = Math.max(bitx, blockXOffset * 16 + x);
                    int maxy = Math.max(bity, blockYOffset * 16 + y);
                    int maxz = Math.max(bitz, blockZOffset * 16 + z);

                    final var packet = new SmartChiselPacket(block, new BlockPos(minx, miny, minz), new BlockPos(maxx, maxy, maxz));
                    ClientPlayNetworking.send(packet);
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
