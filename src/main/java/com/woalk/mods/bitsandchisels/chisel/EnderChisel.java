package com.woalk.mods.bitsandchisels.chisel;

import com.woalk.mods.bitsandchisels.BitItem;
import com.woalk.mods.bitsandchisels.BitsAndChisels;
import com.woalk.mods.bitsandchisels.BitsBlockEntity;
import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.api.client.RedBoxCallback;
import com.woalk.mods.bitsandchisels.networking.EnderChiselPacket;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class EnderChisel extends Item implements ServerPlayNetworking.PlayPayloadHandler<EnderChiselPacket> {
    private long lastBreakTick = 0;
    private boolean fastBreak = false;

    public EnderChisel(Properties settings) {
        super(settings);
        ServerPlayNetworking.registerGlobalReceiver(EnderChiselPacket.TYPE, this);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            Item i = player.getMainHandItem().getItem();
            if (i instanceof EnderChisel) {
                if (!world.isClientSide()) return InteractionResult.FAIL;
                return ((EnderChisel) i).interactBreakBlockClient(player, world, pos);
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    public boolean canDestroyBlock(ItemStack itemStack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        return !(user instanceof Player player && player.isCreative());
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        builder.accept(
                Component.translatable(getDescriptionId() + ".desc")
                        .withStyle(ChatFormatting.DARK_PURPLE)
                        .withStyle(ChatFormatting.ITALIC)
        );
    }

    @Override
    public void receive(EnderChiselPacket payload, ServerPlayNetworking.Context context) {
        final var player = context.player();
        if (!Permissions.check(player, ModPermissions.USE_CHISEL_ENDER, true)) return;
        BlockPos pos = payload.getBlockPos();
        final int x = payload.getBitPos().getX();
        final int y = payload.getBitPos().getY();
        final int z = payload.getBitPos().getZ();
        context.server().execute(() -> {
            // Execute on the main thread
            BitsAndChisels.LOGGER.warn("Ender Chisel used");
            ItemStack stack = player.getMainHandItem();
            ItemStack bit = player.getOffhandItem();
            if (player.level().isLoaded(pos) && player.blockPosition().distSqr(pos) < 81) {
                if (stack.getItem() == this && bit.getItem() instanceof BitItem) {
                    BlockState oldState = BitUtils.getBit(player.level(), pos, x, y, z);
                    BlockState targetState = BitUtils.getBit(bit);
                    for (int i = 0; i < 16; i++) {
                        for (int j = 0; j < 16; j++) {
                            for (int k = 0; k < 16; k++) {
                                BlockState newState = BitUtils.getBit(player.level(), pos, i, j, k);
                                if (newState != null && newState.equals(oldState) || oldState == null) {
                                    BitUtils.setBit(player.level(), pos, i, j, k, targetState);
                                }
                            }
                        }
                    }
                    BitUtils.update(player.level(), pos);
                } else {
                    player.sendSystemMessage(Component.literal("No bit selected in off-hand.").withStyle(ChatFormatting.RED));
                }
            }
        });
    }

    public void initClient() {
        RedBoxCallback.EVENT.register((redBoxDrawer, matrixStack, vertexConsumer, worldoffsetx, worldoffsety, worldoffsetz) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player.getMainHandItem().getItem() == this) {
                HitResult hit = client.hitResult;
                if (hit.getType() == HitResult.Type.BLOCK) {
                    Direction direction = ((BlockHitResult) hit).getDirection();
                    BlockPos pos = ((BlockHitResult) hit).getBlockPos();
                    int x = (int) Math.floor(((hit.getLocation().x() - pos.getX()) * 16) + (direction.getStepX() * -0.5d));
                    int y = (int) Math.floor(((hit.getLocation().y() - pos.getY()) * 16) + (direction.getStepY() * -0.5d));
                    int z = (int) Math.floor(((hit.getLocation().z() - pos.getZ()) * 16) + (direction.getStepZ() * -0.5d));
                    redBoxDrawer.drawRedBox(matrixStack, vertexConsumer, pos, x, y, z, x + 1, y + 1, z + 1, worldoffsetx, worldoffsety, worldoffsetz);
                }
            }
        });
    }

    public InteractionResult interactBreakBlockClient(Player player, Level world, BlockPos pos) {
        if (getTime() - lastBreakTick < (fastBreak ? 3 : 6)) return InteractionResult.CONSUME;
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;

        if (hit.getType() == HitResult.Type.BLOCK) {
            Direction direction = ((BlockHitResult) hit).getDirection();
            int x = (int) Math.floor(((hit.getLocation().x() - pos.getX()) * 16) + (direction.getStepX() * -0.5d));
            int y = (int) Math.floor(((hit.getLocation().y() - pos.getY()) * 16) + (direction.getStepY() * -0.5d));
            int z = (int) Math.floor(((hit.getLocation().z() - pos.getZ()) * 16) + (direction.getStepZ() * -0.5d));
            if (!(world.getBlockEntity(pos) instanceof BitsBlockEntity)) {
                return InteractionResult.CONSUME;
            }
            if (BitUtils.exists(BitUtils.getBit(world, pos, x, y, z))) {
                final var packet = new EnderChiselPacket(pos, new BlockPos(x, y, z));
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
