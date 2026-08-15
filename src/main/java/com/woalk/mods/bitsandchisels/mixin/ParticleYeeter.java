package com.woalk.mods.bitsandchisels.mixin;

import com.woalk.mods.bitsandchisels.BitsAndChisels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Mixin(ParticleEngine.class)
public class ParticleYeeter {
    @Shadow
    protected ClientLevel level;

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    public void yeet(BlockPos pos, BlockState state, CallbackInfo yeet) {
        if (state.is(BitsAndChisels.BITS_BLOCK)) yeet.cancel();
    }

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    public void alsoyeet(BlockPos pos, Direction direction, CallbackInfo yeet) {
        if (this.level.getBlockState(pos).is(BitsAndChisels.BITS_BLOCK)) yeet.cancel();
    }
}
