package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.blocks.AnimatedBitsBlockEntityRenderer
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry

object ModBlockEntityRenderers: IModRegister {
    override fun registerAll() {
        BlockEntityRendererRegistry.register(ModBlockEntities.animatedBitsBlockEntity) { ctx ->
            AnimatedBitsBlockEntityRenderer(ctx)
        }
    }
}