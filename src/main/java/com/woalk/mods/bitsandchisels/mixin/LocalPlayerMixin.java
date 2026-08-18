package com.woalk.mods.bitsandchisels.mixin;

import com.woalk.mods.bitsandchisels.registry.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.woalk.mods.bitsandchisels.BitsAndChisels;
import net.minecraft.client.player.LocalPlayer;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "aiStep", at = @At("RETURN"))
    void e(CallbackInfo funniVarName) {
        if (!(((LocalPlayer)(Object)this).isShiftKeyDown())) ModItems.BIT_ITEM.haspos1 = false;
    }
}
