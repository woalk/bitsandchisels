package com.woalk.mods.bitsandchisels.chisel;

import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.api.client.RedBoxCallback;
import com.woalk.mods.bitsandchisels.networking.IronChiselPacket;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class IronChisel extends Item implements ServerPlayNetworking.PlayPayloadHandler<IronChiselPacket> {
    private long lastBreakTick = 0;

    public IronChisel(Properties settings) {
        super(settings);
        ServerPlayNetworking.registerGlobalReceiver(IronChiselPacket.TYPE, this);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            Item i = player.getItemInHand(hand).getItem();
            if (i instanceof IronChisel) {
                if (!world.isClientSide()) return InteractionResult.FAIL;
                return ((IronChisel)i).interactBreakBlockClient(player, world, pos);
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    public void receive(IronChiselPacket payload, ServerPlayNetworking.Context context) {
        if (!Permissions.check(context.player(), ModPermissions.USE_CHISEL_IRON, true)) return;
        BlockPos pos = payload.getBlockPos();
        int x = payload.getBitPos().getX();
        int y = payload.getBitPos().getY();
        int z = payload.getBitPos().getZ();
        context.server().executeIfPossible(() -> {
            // Execute on the main thread
            ItemStack stack = context.player().getMainHandItem();
            if (stack.getItem() == this && context.player().blockPosition().distSqr(pos) < 81) {
                BitUtils.attemptBreakRegion(context.player(), pos, x, y, z, x + 3, y + 3, z + 3);
            }
        });
    }

    public void initClient() {
        RedBoxCallback.EVENT.register((redBoxDrawer, matrixStack, vertexConsumer, worldoffsetx, worldoffsety, worldoffsetz) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player.getMainHandItem().getItem() == this) {
                HitResult hit = client.hitResult;
                if (hit.getType() == HitResult.Type.BLOCK) {
                    Direction direction = ((BlockHitResult)hit).getDirection();
                    BlockPos pos = ((BlockHitResult)hit).getBlockPos();
                    int x = (int) Math.floor(Math.floor(((hit.getLocation().x() - pos.getX()) * 16) + (direction.getStepX() * -0.5d)) / 4) * 4;
                    int y = (int) Math.floor(Math.floor(((hit.getLocation().y() - pos.getY()) * 16) + (direction.getStepY() * -0.5d)) / 4) * 4;
                    int z = (int) Math.floor(Math.floor(((hit.getLocation().z() - pos.getZ()) * 16) + (direction.getStepZ() * -0.5d)) / 4) * 4;
                    redBoxDrawer.drawRedBox(matrixStack, vertexConsumer, pos, x, y, z, x + 4, y + 4, z + 4, worldoffsetx, worldoffsety, worldoffsetz);
                }
            }
        });
    }

    public InteractionResult interactBreakBlockClient(Player player, Level world, BlockPos pos) {
        if (getTime() - lastBreakTick < 5) return InteractionResult.CONSUME;
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;

        if (hit.getType() == HitResult.Type.BLOCK) {
            Direction direction = ((BlockHitResult)hit).getDirection();
            int x = (int) Math.floor(Math.floor(((hit.getLocation().x() - pos.getX()) * 16) + (direction.getStepX() * -0.5d)) / 4) * 4;
            int y = (int) Math.floor(Math.floor(((hit.getLocation().y() - pos.getY()) * 16) + (direction.getStepY() * -0.5d)) / 4) * 4;
            int z = (int) Math.floor(Math.floor(((hit.getLocation().z() - pos.getZ()) * 16) + (direction.getStepZ() * -0.5d)) / 4) * 4;
            if (BitUtils.exists(BitUtils.getBit(world, pos, x, y, z))) {
                final var packet = new IronChiselPacket(pos, new BlockPos(x, y, z));
                ClientPlayNetworking.send(packet);
                lastBreakTick = getTime();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.CONSUME;
    }
    
    private static long getTime() {
        return System.currentTimeMillis() / 50;
    }
}
