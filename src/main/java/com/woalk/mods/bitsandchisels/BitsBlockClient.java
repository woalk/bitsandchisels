package com.woalk.mods.bitsandchisels;

import com.woalk.mods.bitsandchisels.blocks.BitItemModel;
import com.woalk.mods.bitsandchisels.blocks.BitStructureItemModel;
import com.woalk.mods.bitsandchisels.blocks.BitsBlockItemModel;
import com.woalk.mods.bitsandchisels.blocks.BitsBlockModel;
import com.woalk.mods.bitsandchisels.registry.ModBlocks;
import com.woalk.mods.bitsandchisels.registry.ModItems;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.resources.Identifier;

public class BitsBlockClient implements ClientModInitializer {

    /**
     * The vanilla model all block models inherit from. Its display transforms are the ones any
     * block-shaped item model has to use, otherwise the item is rendered untransformed, i.e. flat
     * in the GUI and at full block size in hand and on the ground.
     */
    private static final Identifier BLOCK_MODEL = Identifier.withDefaultNamespace("block/block");

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
                if (itemId.equals(Identifier.fromNamespaceAndPath(BitsAndChisels.MODID, "bits_block"))
                        || itemId.equals(Identifier.fromNamespaceAndPath(BitsAndChisels.MODID, "animated_bits_block"))) {
                    BitsAndChisels.LOGGER.info("Registering BitsBlockItemModel for {}", itemId);
                    return new BitsBlockItemModel(blockItemTransforms(ctx));
                } else if (itemId.equals(Identifier.fromNamespaceAndPath(BitsAndChisels.MODID, "bit_item"))) {
                    BitsAndChisels.LOGGER.info("Registering BitItemModel for {}", itemId);
                    return new BitItemModel(blockItemTransforms(ctx));
                } else if (itemId.equals(Identifier.fromNamespaceAndPath(BitsAndChisels.MODID, "bit_structure"))) {
                    BitsAndChisels.LOGGER.info("Registering BitStructureItemModel for {}", itemId);
                    return new BitStructureItemModel(blockItemTransforms(ctx));
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
     * Reads the display transforms of the vanilla {@code minecraft:block/block} model, which the
     * custom item models apply to their layers.
     */
    private static ItemTransforms blockItemTransforms(ModelModifier.AfterBakeItem.Context ctx) {
        try {
            return ctx.bakingContext().blockModelBaker().getModel(BLOCK_MODEL).getTopTransforms();
        } catch (RuntimeException e) {
            BitsAndChisels.LOGGER.warn("Could not resolve the transforms of {}", BLOCK_MODEL, e);
            return ItemTransforms.NO_TRANSFORMS;
        }
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
