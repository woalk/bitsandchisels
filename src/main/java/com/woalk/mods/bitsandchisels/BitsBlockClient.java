package com.woalk.mods.bitsandchisels;

import com.woalk.mods.bitsandchisels.blocks.BitsBlockModel;
import com.woalk.mods.bitsandchisels.blocks.BitStructureItemModel;
import com.woalk.mods.bitsandchisels.registry.ModBlocks;
import com.woalk.mods.bitsandchisels.registry.ModItems;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

public class BitsBlockClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            // Register block state resolver for bits_block
            pluginContext.registerBlockStateResolver(BitsAndChisels.BITS_BLOCK, context -> {
                BitsBlockModel model = new BitsBlockModel();
                for (var state : context.block().getStateDefinition().getPossibleStates()) {
                    context.setModel(state, new SimpleUnbakedRoot(model));
                }
            });

            // Register block state resolver for animated_bits_block
            pluginContext.registerBlockStateResolver(ModBlocks.INSTANCE.getAnimatedBitsBlock(), context -> {
                BitsBlockModel model = new BitsBlockModel();
                for (var state : context.block().getStateDefinition().getPossibleStates()) {
                    context.setModel(state, new SimpleUnbakedRoot(model));
                }
            });

            // Replace item models after bake
            pluginContext.modifyItemModelAfterBake().register((model, ctx) -> {
                Identifier itemId = ctx.itemId();
                if (itemId.equals(Identifier.fromNamespaceAndPath("bitsandchisels", "bits_block"))
                        || itemId.equals(Identifier.fromNamespaceAndPath("bitsandchisels", "animated_bits_block"))) {
                    return new BitsBlockItemModel();
                } else if (itemId.equals(Identifier.fromNamespaceAndPath("bitsandchisels", "bit_item"))) {
                    return new BitItemModel();
                } else if (itemId.equals(Identifier.fromNamespaceAndPath("bitsandchisels", "bit_structure"))) {
                    return new BitStructureItemModel();
                }
                return model;
            });
        });

        ModItems.DIAMOND_CHISEL.initClient();
        ModItems.ENDER_CHISEL.initClient();
        ModItems.IRON_CHISEL.initClient();
        ModItems.BIT_ITEM.initClient();
        ModItems.SMART_CHISEL.initClient();
    }

    /**
     * Simple UnbakedRoot that wraps a pre-built BlockStateModel.
     */
    private static class SimpleUnbakedRoot implements BlockStateModel.UnbakedRoot {
        private final BlockStateModel model;

        SimpleUnbakedRoot(BlockStateModel model) {
            this.model = model;
        }

        @Override
        public BlockStateModel bake(net.minecraft.world.level.block.state.BlockState state, net.minecraft.client.resources.model.ModelBaker baker) {
            return model;
        }

        @Override
        public Object visualEqualityGroup(net.minecraft.world.level.block.state.BlockState state) {
            return model;
        }

        @Override
        public void resolveDependencies(net.minecraft.client.resources.model.ResolvableModel.Resolver resolver) {
            // No dependencies
        }
    }
}
