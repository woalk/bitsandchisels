package com.woalk.mods.bitsandchisels;

import com.woalk.mods.bitsandchisels.blocks.BitStructureModel;
import com.woalk.mods.bitsandchisels.blocks.BitsBlockModel;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelProviderException;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.util.Identifier;

public class BitsBlockClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLoadingRegistry.INSTANCE.registerResourceProvider(rm -> new BitsModelProvider());
        BitsAndChisels.DIAMOND_CHISEL.initClient();
        BitsAndChisels.ENDER_CHISEL.initClient();
        BitsAndChisels.IRON_CHISEL.initClient();
        BitsAndChisels.BIT_ITEM.initClient();
        BitsAndChisels.SMART_CHISEL.initClient();
    }

    public static class BitsModelProvider implements ModelResourceProvider {
        public static final Identifier BITS_BLOCK_MODEL = new Identifier("bitsandchisels:block/bits_block_model");
        public static final Identifier BITS_BLOCK_ITEM_MODEL = new Identifier("bitsandchisels:item/bits_block");
        public static final Identifier ANIMATED_BITS_BLOCK_ITEM_MODEL = new Identifier("bitsandchisels:item/animated_bits_block");
        public static final Identifier BIT_STRUCTURE_MODEL = new Identifier("bitsandchisels:item/bit_structure");
        public static final Identifier BIT_ITEM_MODEL = new Identifier("bitsandchisels:item/bit_item");

        @Override
        public @Nullable UnbakedModel loadModelResource(Identifier resourceId, ModelProviderContext context) throws ModelProviderException {
            if(resourceId.equals(BITS_BLOCK_MODEL) || resourceId.equals(BITS_BLOCK_ITEM_MODEL) || resourceId.equals(ANIMATED_BITS_BLOCK_ITEM_MODEL)) {
                return new BitsBlockModel();
            } else if (resourceId.equals(BIT_ITEM_MODEL)) {
                return new BitItemModel();
            } else if (resourceId.equals(BIT_STRUCTURE_MODEL)) {
                return new BitStructureModel();
            } else {
                return null;
            }
        }
    }

    
}
