package com.woalk.mods.bitsandchisels.mixin;

import net.minecraft.util.datafix.schemas.V99;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(V99.class)
public interface V99Accessor {
    @Accessor("ITEM_TO_BLOCKENTITY")
    static Map<String, String> getItemToBlockEntity() {
        throw new AssertionError();
    }
}
