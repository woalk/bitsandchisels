package com.woalk.mods.bitsandchisels.chisel;

import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.api.client.RedBoxCallback;
import com.woalk.mods.bitsandchisels.networking.DiamondChiselPacket;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import com.woalk.mods.bitsandchisels.render.RedBoxPipeline;
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
import net.minecraft.world.phys.shapes.Shapes;

public class DiamondChisel extends Item implements ServerPlayNetworking.PlayPayloadHandler<DiamondChiselPacket> {
    private long lastBreakTick = 0;
    private boolean fastBreak = false;

    public DiamondChisel(Properties settings) {
        super(settings);
        ServerPlayNetworking.registerGlobalReceiver(DiamondChiselPacket.TYPE, this);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            Item i = player.getItemInHand(hand).getItem();
            if (i instanceof DiamondChisel) {
                if (!world.isClientSide()) return InteractionResult.FAIL;
                return ((DiamondChisel)i).interactBreakBlockClient(player, world, pos);
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    public void receive(DiamondChiselPacket payload, ServerPlayNetworking.Context context) {
        if (!Permissions.check(context.player(), ModPermissions.USE_CHISEL_DIAMOND, true)) return;
        BlockPos pos = payload.getBlockPos();
        int x = payload.getBitPos().getX();
        int y = payload.getBitPos().getY();
        int z = payload.getBitPos().getZ();
        context.server().executeIfPossible(() -> {
            // Execute on the main thread
            ItemStack stack = context.player().getMainHandItem();
            if (context.player().blockPosition().distSqr(pos) < 81 && stack.getItem() == this) {
                BitUtils.attemptBreak(context.player(), pos, x, y, z);
            }
        });
    }

    public void initClient() {
        RedBoxPipeline.registerExtraction((_, hit) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.player.getMainHandItem().is(this) && hit != null) {
                final var shape = RedBoxPipeline.Shape.singleBit(hit);
                if (shape != null) {
                    return new RedBoxPipeline.State(shape);
                }
            }
            return null;
        });
    }

    public InteractionResult interactBreakBlockClient(Player player, Level world, BlockPos pos) {
        if (getTime() - lastBreakTick < (fastBreak ? 3 : 6)) return InteractionResult.CONSUME;
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;

        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            final var bitPos = RedBoxPipeline.Shape.bitPosition((BlockHitResult) hit);
            if (BitUtils.exists(BitUtils.getBit(world, pos, bitPos.getX(), bitPos.getY(), bitPos.getZ()))) {
                final var packet = new DiamondChiselPacket(pos, new BlockPos(bitPos.getX(), bitPos.getY(), bitPos.getZ()));
                ClientPlayNetworking.send(packet);
                fastBreak = getTime() - lastBreakTick < 10;
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
