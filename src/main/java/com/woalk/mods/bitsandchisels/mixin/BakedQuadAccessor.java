package com.woalk.mods.bitsandchisels.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(BakedQuad.class)
public interface BakedQuadAccessor {
    @Accessor
    public BakedQuad.MaterialInfo getMaterialInfo();
}
