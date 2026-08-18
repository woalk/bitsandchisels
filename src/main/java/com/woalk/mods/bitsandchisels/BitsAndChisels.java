package com.woalk.mods.bitsandchisels;

import com.woalk.mods.bitsandchisels.blocks.BitsBlockItem;
import com.woalk.mods.bitsandchisels.chisel.EnderChisel;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.woalk.mods.bitsandchisels.chisel.DiamondChisel;
import com.woalk.mods.bitsandchisels.chisel.IronChisel;
import com.woalk.mods.bitsandchisels.chisel.SmartChisel;
import com.woalk.mods.bitsandchisels.wrench.WrenchItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;

public class BitsAndChisels implements ModInitializer {

	public static final String MODID = "bitsandchisels";

	public static final Logger LOGGER = LoggerFactory.getLogger("BitsAndChisels");

	public static final BitsBlock BITS_BLOCK = new BitsBlock(FabricBlockSettings.of().noOcclusion().dynamicShape().destroyTime(4.0f).lightLevel(s -> s.getValue(BitsBlock.LIGHT_LEVEL)));
	public static BlockEntityType<BitsBlockEntity> BITS_BLOCK_ENTITY;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MODID, "bits_block"), BITS_BLOCK);
		BITS_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "bitsandchisels:bits_block_entity", FabricBlockEntityTypeBuilder.create(BitsBlockEntity::new, BITS_BLOCK).build(null));
	}
	
}
