package com.woalk.mods.bitsandchisels;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;

public class BitsAndChisels implements ModInitializer {

	public static final String MODID = "bitsandchisels";

	public static final Logger LOGGER = LoggerFactory.getLogger("BitsAndChisels");

	private static final Identifier BITS_BLOCK_ID = Identifier.fromNamespaceAndPath(MODID, "bits_block");
	public static final BlockItemId BITS_BLOCK_ITEM_ID = BlockItemId.create(BITS_BLOCK_ID, BITS_BLOCK_ID);
	public static final BitsBlock BITS_BLOCK = new BitsBlock(BlockBehaviour.Properties.of().setId(BITS_BLOCK_ITEM_ID.block()).noOcclusion().dynamicShape().destroyTime(4.0f).lightLevel(s -> s.getValue(BitsBlock.LIGHT_LEVEL)));
	public static BlockEntityType<BitsBlockEntity> BITS_BLOCK_ENTITY;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(BuiltInRegistries.BLOCK, BITS_BLOCK_ID, BITS_BLOCK);
		BITS_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "bits_block_entity"), FabricBlockEntityTypeBuilder.create(BitsBlockEntity::new, BITS_BLOCK).build(null));
	}
	
}
