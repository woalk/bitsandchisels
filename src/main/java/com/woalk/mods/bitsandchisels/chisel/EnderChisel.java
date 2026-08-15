package com.woalk.mods.bitsandchisels.chisel;

import com.woalk.mods.bitsandchisels.BitItem;
import com.woalk.mods.bitsandchisels.BitsAndChisels;
import com.woalk.mods.bitsandchisels.BitsBlockEntity;
import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.api.client.RedBoxCallback;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import io.netty.buffer.Unpooled;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnderChisel extends TieredItem implements ServerPlayNetworking.PlayChannelHandler {

    public static final ResourceLocation PACKET_ID = new ResourceLocation("bitsandchisels", "ender_chisel_packet");
    private long lastBreakTick = 0;
    private boolean fastBreak = false;

    public EnderChisel(Properties settings) {
        super(Tiers.STONE, settings);
        ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, this);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            Item i = player.getMainHandItem().getItem();
            if (i instanceof EnderChisel) {
                if (!world.isClientSide) return InteractionResult.FAIL;
                return ((EnderChisel) i).interactBreakBlockClient(player, world, pos);
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player miner) {
        return !miner.isCreative();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        tooltip.add(
                Component.translatable(getDescriptionId() + ".desc")
                        .withStyle(ChatFormatting.DARK_PURPLE)
                        .withStyle(ChatFormatting.ITALIC)
        );
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        if (!Permissions.check(player, ModPermissions.USE_CHISEL_ENDER, true)) return;
        BlockPos pos = buf.readBlockPos();
        final int x = buf.readInt();
        final int y = buf.readInt();
        final int z = buf.readInt();
        server.execute(() -> {
            // Execute on the main thread
            BitsAndChisels.LOGGER.warn("Ender Chisel used");
            ItemStack stack = player.getMainHandItem();
            ItemStack bit = player.getOffhandItem();
            if (player.level.isLoaded(pos) && player.blockPosition().distSqr(pos) < 81) {
                if (stack.getItem() == BitsAndChisels.ENDER_CHISEL && bit.getItem() instanceof BitItem) {
                    BlockState oldState = BitUtils.getBit(player.level, pos, x, y, z);
                    BlockState targetState = BitUtils.getBit(bit);
                    for (int i = 0; i < 16; i++) {
                        for (int j = 0; j < 16; j++) {
                            for (int k = 0; k < 16; k++) {
                                BlockState newState = BitUtils.getBit(player.level, pos, i, j, k);
                                if (newState != null && newState.equals(oldState) || oldState == null) {
                                    BitUtils.setBit(player.level, pos, i, j, k, targetState);
                                }
                            }
                        }
                    }
                    BitUtils.update(player.level, pos);
                } else {
                    player.sendSystemMessage(Component.literal("No bit selected in off-hand.").withStyle(ChatFormatting.RED));
                }
            }
        });
    }

    public void initClient() {
        RedBoxCallback.EVENT.register((redBoxDrawer, matrixStack, vertexConsumer, worldoffsetx, worldoffsety, worldoffsetz) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player.getMainHandItem().getItem() == BitsAndChisels.ENDER_CHISEL) {
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
                FriendlyByteBuf passedData = new FriendlyByteBuf(Unpooled.buffer());
                passedData.writeBlockPos(pos);
                passedData.writeInt(x);
                passedData.writeInt(y);
                passedData.writeInt(z);
                ClientPlayNetworking.send(PACKET_ID, passedData);
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
