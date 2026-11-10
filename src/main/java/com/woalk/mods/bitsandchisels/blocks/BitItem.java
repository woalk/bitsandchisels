package com.woalk.mods.bitsandchisels.blocks;

import com.woalk.mods.bitsandchisels.render.RedBoxPipeline;
import com.woalk.mods.bitsandchisels.utils.BitNbtUtil;
import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.api.client.RedBoxCallback;
import com.woalk.mods.bitsandchisels.networking.BitPacket;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import io.netty.buffer.Unpooled;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class BitItem extends Item implements ServerPlayNetworking.PlayPayloadHandler<BitPacket> {
    BlockPos block = null;

    int bitx = 0;
    int bity = 0;
    int bitz = 0;

    public boolean haspos1 = false;

    public BitItem(Properties settings) {
        super(settings);
        ServerPlayNetworking.registerGlobalReceiver(BitPacket.TYPE, this);
    }

    @Override
    public void receive(BitPacket payload, ServerPlayNetworking.Context context) {
        if (!Permissions.check(context.player(), ModPermissions.USE_BIT, true)) return;
        BlockPos pos = payload.getBlockPos();
        int x1 = payload.getBitPos().getX();
        int y1 = payload.getBitPos().getY();
        int z1 = payload.getBitPos().getZ();
        int x2 = payload.getBitPos2().getX();
        int y2 = payload.getBitPos2().getY();
        int z2 = payload.getBitPos2().getZ();
        InteractionHand hand = payload.isMainHandInteraction() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        context.server().execute(() -> {
            // Execute on the main thread
            if (
                context.player().getItemInHand(hand).getItem() == this &&
                context.player().blockPosition().distSqr(pos) < 81 &&
                x1 <= x2 && y1 <= y2 && z1 <= z2 &&
                ((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)) + ((z2 - z1) * (z2 - z1)) <= 1000000
            ) {
                Level world = context.player().level();
                BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                ItemStack stack = context.player().getItemInHand(hand);
                BlockState state = BitUtils.getBit(stack);
                placeloop:
                for (int i = x1; i <= x2; i++) {
                    for (int j = y1; j <= y2; j++) {
                        for (int k = z1; k <= z2; k++) {
                            mut.set(pos.getX() + Math.floorDiv(i, 16), pos.getY() + Math.floorDiv(j, 16), pos.getZ() + Math.floorDiv(k, 16));
                            int x = Math.floorMod(i, 16);
                            int y = Math.floorMod(j, 16);
                            int z = Math.floorMod(k, 16);
                            if (world.isLoaded(mut) && !BitUtils.exists(BitUtils.getBit(world, mut, x, y, z))) {
                                boolean b = BitUtils.setBit(world, mut, x, y, z, state);
                                if (b && !context.player().isCreative()) stack.shrink(1);
                                if (stack.isEmpty()) {
                                    for (int a = 0; a < context.player().getInventory().getContainerSize(); a++) {
                                        ItemStack teststack = context.player().getInventory().getItem(a);
                                        if (!teststack.isEmpty() && teststack.getItem() == this) {
                                            BlockState itemState = BitUtils.getBit(teststack);
                                            if (itemState == state) {
                                                stack = teststack;
                                                break;
                                            }
                                        }
                                    }
                                    if (stack.isEmpty()) {
                                        break placeloop;
                                    }
                                }
                            }
                        }
                    }
                }
                int blockx1 = pos.getX() + Math.floorDiv(x1, 16);
                int blocky1 = pos.getY() + Math.floorDiv(y1, 16);
                int blockz1 = pos.getZ() + Math.floorDiv(z1, 16);
                int blockx2 = pos.getX() + Math.floorDiv(x2, 16);
                int blocky2 = pos.getY() + Math.floorDiv(y2, 16);
                int blockz2 = pos.getZ() + Math.floorDiv(z2, 16);
                for (int i = blockx1; i <= blockx2; i++) {
                    for (int j = blocky1; j <= blocky2; j++) {
                        for (int k = blockz1; k <= blockz2; k++) {
                            mut.set(i, j, k);
                            BitUtils.update(world, mut);
                        }
                    }
                }
            }
        });
    }

    public void initClient() {
        RedBoxPipeline.registerExtraction((_, hit) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.player.getMainHandItem().is(this) && hit != null) {
                final var pos1 = haspos1 ? new RedBoxPipeline.Shape.BitSelection(block, bitx, bity, bitz) : null;
                final var shape = RedBoxPipeline.Shape.multiBit(hit, pos1, true);
                if (shape != null) {
                    return new RedBoxPipeline.State(shape);
                }
            }
            return null;
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide()) return InteractionResult.CONSUME;
        BlockPos pos = context.getClickedPos();
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;
            
        if (hit.getType() == HitResult.Type.BLOCK) {
            Direction direction = ((BlockHitResult)hit).getDirection();
            int x = ((int) Math.floor(((hit.getLocation().x() - pos.getX()) * 16) + (direction.getStepX() * -0.5d))) + direction.getStepX();
            int y = ((int) Math.floor(((hit.getLocation().y() - pos.getY()) * 16) + (direction.getStepY() * -0.5d))) + direction.getStepY();
            int z = ((int) Math.floor(((hit.getLocation().z() - pos.getZ()) * 16) + (direction.getStepZ() * -0.5d))) + direction.getStepZ();

            if (x > 15) {
                pos = pos.offset(1, 0, 0);
                x -= 16;
            }
            if (y > 15) {
                pos = pos.offset(0, 1, 0);
                y -= 16;
            }
            if (z > 15) {
                pos = pos.offset(0, 0, 1);
                z -= 16;
            }
            if (x < 0) {
                pos = pos.offset(-1, 0, 0);
                x += 16;
            }
            if (y < 0) {
                pos = pos.offset(0, -1, 0);
                y += 16;
            }
            if (z < 0) {
                pos = pos.offset(0, 0, -1);
                z += 16;
            }

            if (BitUtils.canPlace(context.getPlayer(), pos, x, y, z)) {
                if (context.getPlayer().isShiftKeyDown()) {
                    if (haspos1) {
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

                        final var packet = new BitPacket(block != null ? block : pos, new BlockPos(minx, miny, minz), new BlockPos(maxx, maxy, maxz), context.getHand().equals(InteractionHand.MAIN_HAND));
                        ClientPlayNetworking.send(packet);
                        haspos1 = false;
                        return InteractionResult.SUCCESS;
                    } else {
                        this.block = pos;
                        bitx = x;
                        bity = y;
                        bitz = z;
                        haspos1 = true;
                    }
                } else {
                    final var bit = new BlockPos(x, y, z);
                    final var packet = new BitPacket(block != null ? block : pos, bit, bit, context.getHand().equals(InteractionHand.MAIN_HAND));
                    ClientPlayNetworking.send(packet);
                    return InteractionResult.SUCCESS;
                }
                
            }
            
        }
        return InteractionResult.PASS;
    }

    @Override
    public Component getName(ItemStack stack) {
        final var data = stack.getComponents().get(DataComponents.CUSTOM_DATA);
        final var bitNbt = data != null ? data.copyTag().getCompound("bit").orElse(null) : null;
        BlockState state = bitNbt != null ? BitNbtUtil.toBlockState(bitNbt) : Blocks.AIR.defaultBlockState();
        return Component.translatable(this.getDescriptionId(), Component.translatable(state.getBlock().getDescriptionId()));
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        if (tooltipFlag.isAdvanced()) {
            final var data = itemStack.getComponents().get(DataComponents.CUSTOM_DATA);
            final CompoundTag blockState = data != null ? data.copyTag().getCompound("bit").orElse(null) : null;
            if (blockState != null) {
                if (blockState.contains("Name")) {
                    Tag name = blockState.get("Name");
                    if (name != null) {
                        blockState.put("id", name);
                    }
                }
                builder.accept(Component.literal(blockState.getString("id").get()).withStyle(ChatFormatting.DARK_PURPLE));
                final CompoundTag remainder = blockState.copy();
                remainder.remove("Name");
                if (!remainder.isEmpty()) {
                    remainder.asString().ifPresent(s -> builder.accept(Component.literal(s).withStyle(ChatFormatting.DARK_GREEN)));
                }
            }
        }
    }
}
