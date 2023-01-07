package io.github.coolmineman.bitsandchisels.mixin;

import io.github.coolmineman.bitsandchisels.registry.ModPermissions;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DebugStickItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DebugStickItem.class)
public class DebugStickItemMixin {
    @Redirect(method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isCreativeLevelTwoOp()Z"))
    public boolean redirect_isCreativeLevelTwoOp(PlayerEntity player) {
        return Permissions.check(player, ModPermissions.USE_DEBUG_STICK, player.isCreativeLevelTwoOp());
    }
}
