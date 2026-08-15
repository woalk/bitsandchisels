package com.woalk.mods.bitsandchisels.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

@Mixin(CubeVoxelShape.class)
public interface CubeVoxelShapeFactory {
    @Invoker("<init>")
    public static CubeVoxelShape getSimpleVoxelShape(DiscreteVoxelShape voxelSet) {
        throw new AssertionError();
    }
}
