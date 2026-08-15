package com.woalk.mods.bitsandchisels.mixin;

import com.woalk.mods.bitsandchisels.api.client.AirSwingItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "startAttack()Z", at = @At("TAIL"))
    void doAttack(CallbackInfoReturnable<Boolean> c) {
        Minecraft thiz = (Minecraft) (Object) this;
        if (thiz.hitResult.getType() == HitResult.Type.MISS) {
            ItemStack stack = thiz.player.getMainHandItem();
            Item item = stack.getItem();
            if (item instanceof AirSwingItem) {
                ((AirSwingItem)item).airSwing(thiz.player, stack);
            }
        }
    }
}
