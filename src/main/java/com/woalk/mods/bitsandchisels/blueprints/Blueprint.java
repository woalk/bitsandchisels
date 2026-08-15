package com.woalk.mods.bitsandchisels.blueprints;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.woalk.mods.bitsandchisels.BitNbtUtil;
import com.woalk.mods.bitsandchisels.BitsAndChisels;
import com.woalk.mods.bitsandchisels.api.BitUtils;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class Blueprint extends Item implements CauldronInteraction {
    private static final String BLUEPRINT_STRING = "blueprint";

    public Blueprint(Properties settings) {
        super(settings);
        CauldronInteraction.WATER.put(this, this);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (!world.isClientSide) {
            if (!Permissions.check(Objects.requireNonNull(context.getPlayer()),
                    ModPermissions.USE_BLUEPRINT, true)) {
                return InteractionResult.PASS;
            }
            if (context.getItemInHand().getTagElement(BLUEPRINT_STRING) == null) {
                BlockState[][][] target = BitUtils.getBitArray(world, context.getClickedPos());
                if (target != null) {
                    CompoundTag blueprintTag = context.getItemInHand().getOrCreateTagElement(BLUEPRINT_STRING);
                    BitNbtUtil.write3DBitArray(blueprintTag, target);
                    return InteractionResult.SUCCESS;
                }
            } else {
                BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
                BlockState[][][] bits = new BlockState[16][16][16];
                BitNbtUtil.read3DBitArray(context.getItemInHand().getTagElement(BLUEPRINT_STRING), bits);
                Map<BlockState, IntList> invMap = indexPlayerInventory(context.getPlayer());
                boolean update = false;
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        for (int k = 0; k < 16; k++) {
                            BlockState bitState = bits[i][j][k];
                            if (BitUtils.canPlace(context.getPlayer(), placePos, i, j, k, false)) {
                                if (context.getPlayer().isCreative()) {
                                    if (BitUtils.setBit(world, placePos, i, j, k, bitState)) update = true;
                                } else {
                                    IntList checkSlots = invMap.get(bitState);
                                    if (checkSlots != null) {
                                        for (int z = 0; z < checkSlots.size(); z++) {
                                            int slot = checkSlots.getInt(z);
                                            ItemStack slotStack = context.getPlayer().getInventory().getItem(slot);
                                            if (!slotStack.isEmpty() && BitUtils.setBit(world, placePos, i, j, k, bitState)) {
                                                slotStack.shrink(1);
                                                update = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (update) {
                    BitUtils.update(world, placePos);
                    context.getPlayer().getCooldowns().addCooldown(BitsAndChisels.BLUEPRINT, 5);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private Map<BlockState, IntList> indexPlayerInventory(Player playerEntity) {
        HashMap<BlockState, IntList> result = new HashMap<>();
        for (int i = 0; i < playerEntity.getInventory().getContainerSize(); i++) {
            ItemStack stack = playerEntity.getInventory().getItem(i);
            if (stack.getItem() == BitsAndChisels.BIT_ITEM) {
                BlockState itemState = BitUtils.getBit(stack);
                if (!itemState.isAir()) {
                    IntList intList = result.computeIfAbsent(itemState, k -> new IntArrayList(5));
                    intList.add(i);
                }
            }
        }
        return result;
    }

    @Override
    public Component getName(ItemStack stack) {
        return stack.getTagElement(BLUEPRINT_STRING) == null ? Component.translatable(this.getDescriptionId() + ".unwritten") : super.getName(stack);
    }

    @Override
    public InteractionResult interact(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        stack.removeTagKey(BLUEPRINT_STRING);
        return InteractionResult.SUCCESS;
    }
    
}
