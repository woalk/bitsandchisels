package com.woalk.mods.bitsandchisels.mixin;

import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DebugStickItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DebugStickItem.class)
public class DebugStickItemMixin {
    @Redirect(method = "handleInteraction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canUseGameMasterBlocks()Z"))
    public boolean redirect_isCreativeLevelTwoOp(Player player) {
        return Permissions.check(player, ModPermissions.USE_DEBUG_STICK, player.canUseGameMasterBlocks());
    }
}
