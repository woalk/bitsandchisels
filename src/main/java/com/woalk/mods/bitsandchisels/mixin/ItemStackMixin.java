package com.woalk.mods.bitsandchisels.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Worlds created before 1.20.5 stored the bits data of {@code BitsBlockItem}s inside a {@code BlockEntityTag}
 * NBT compound that did not contain an {@code id}. Minecraft's item componentization data fixer migrates that
 * tag into the {@code minecraft:block_entity_data} component verbatim, so the {@code id} stays missing.
 * <p>
 * Since {@code minecraft:block_entity_data} is decoded through {@link net.minecraft.world.item.component.CustomData#CODEC_WITH_ID}
 * (which requires an {@code id}), the whole {@link ItemStack} fails to decode and Minecraft discards it with
 * {@code Tried to load invalid item: 'Missing id for entity in: ...'}.
 * <p>
 * This mixin restores the missing block entity {@code id} on the serialized NBT right before it is parsed,
 * so the data survives the load instead of being dropped.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    private static final String BLOCK_ENTITY_DATA_KEY = "minecraft:block_entity_data";

    @Inject(method = "parse", at = @At("HEAD"))
    private static void bitsandchisels$fixMissingBlockEntityId(HolderLookup.Provider provider, Tag tag, CallbackInfoReturnable<Optional<ItemStack>> cir) {
        if (!(tag instanceof CompoundTag itemTag)) {
            return;
        }

        final String blockEntityId = bitsandchisels$blockEntityIdFor(itemTag.getString("id"));
        if (blockEntityId == null) {
            return;
        }

        if (!itemTag.contains("components", Tag.TAG_COMPOUND)) {
            return;
        }
        final CompoundTag components = itemTag.getCompound("components");
        if (!components.contains(BLOCK_ENTITY_DATA_KEY, Tag.TAG_COMPOUND)) {
            return;
        }

        final CompoundTag blockEntityData = components.getCompound(BLOCK_ENTITY_DATA_KEY);
        if (!blockEntityData.contains("id", Tag.TAG_STRING)) {
            blockEntityData.putString("id", blockEntityId);
        }
    }

    private static String bitsandchisels$blockEntityIdFor(String itemId) {
        switch (itemId) {
            case "bitsandchisels:bits_block":
                return "bitsandchisels:bits_block_entity";
            case "bitsandchisels:animated_bits_block":
                return "bitsandchisels:animated_bits_block";
            default:
                return null;
        }
    }
}
