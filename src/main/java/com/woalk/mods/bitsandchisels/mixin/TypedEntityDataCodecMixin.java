package com.woalk.mods.bitsandchisels.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.item.component.TypedEntityData$1")
public class TypedEntityDataCodecMixin {
	/**
	 * When decoding block_entity_data, the codec removes "id" from the CompoundTag
	 * and fails if it is absent. Old BitsAndChisels worlds that were upgraded to
	 * Minecraft 26.2 before the V99 ITEM_TO_BLOCKENTITY fix was in place have
	 * block_entity_data without an "id", causing the items to be silently dropped.
	 *
	 * This redirect intercepts the {@code tag.remove("id")} call and, when the
	 * result would be {@code null}, checks whether the tag belongs to one of our
	 * block entities (by looking for mod-specific keys) and synthesises the
	 * correct id so the decode can proceed.
	 */
	@Redirect(
		method = "lambda$decode$0",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/nbt/CompoundTag;remove(Ljava/lang/String;)Lnet/minecraft/nbt/Tag;"
		)
	)
	private static Tag bitsandchisels$fixMissingBlockEntityId(CompoundTag tag, String key) {
		Tag result = tag.remove(key);
		if (result != null) {
			return result;
		}
		// The tag has no "id" — check if it contains data from our mod's block
		// entities and supply the correct id so the item is not lost.
		if (tag.contains("frames")) {
			return StringTag.valueOf("bitsandchisels:animated_bits_block");
		}
		if (tag.contains("bits_v2") || tag.contains("palette")) {
			return StringTag.valueOf("bitsandchisels:bits_block_entity");
		}
		return null; // not our data — let the original error path continue
	}
}
