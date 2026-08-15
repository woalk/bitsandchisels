package com.woalk.mods.bitsandchisels;

import com.woalk.mods.bitsandchisels.blocks.BitsBlockItem;
import com.woalk.mods.bitsandchisels.chisel.EnderChisel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.woalk.mods.bitsandchisels.blueprints.Blueprint;
import com.woalk.mods.bitsandchisels.chisel.DiamondChisel;
import com.woalk.mods.bitsandchisels.chisel.IronChisel;
import com.woalk.mods.bitsandchisels.chisel.SmartChisel;
import com.woalk.mods.bitsandchisels.wrench.WrenchItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;

public class BitsAndChisels implements ModInitializer {

	public static final String MODID = "bitsandchisels";

	public static final Logger LOGGER = LoggerFactory.getLogger("BitsAndChisels");

	public static final CreativeModeTab ITEM_GROUP = FabricItemGroupBuilder.create(new ResourceLocation(MODID, MODID)).icon(BitsAndChisels::getDiamondChiselStack).build();

	public static final BitsBlock BITS_BLOCK = new BitsBlock(FabricBlockSettings.of(Material.METAL).noOcclusion().dynamicShape().destroyTime(4.0f).lightLevel(s -> s.getValue(BitsBlock.LIGHT_LEVEL)));
	public static final BlockItem BITS_BLOCK_ITEM = new BitsBlockItem(BITS_BLOCK, new Item.Properties());
	public static final DiamondChisel DIAMOND_CHISEL = new DiamondChisel(new Item.Properties().tab(ITEM_GROUP));
	public static final EnderChisel ENDER_CHISEL = new EnderChisel(new Item.Properties().tab(ITEM_GROUP));
	public static final IronChisel IRON_CHISEL = new IronChisel(new Item.Properties().tab(ITEM_GROUP));
	public static final SmartChisel SMART_CHISEL = new SmartChisel(new Item.Properties().tab(ITEM_GROUP));
	public static final WrenchItem WRENCH_ITEM = new WrenchItem(new Item.Properties().tab(ITEM_GROUP));
	public static final Blueprint BLUEPRINT = new Blueprint(new Item.Properties().tab(ITEM_GROUP).stacksTo(1));
	public static BlockEntityType<BitsBlockEntity> BITS_BLOCK_ENTITY;

	public static final BitItem BIT_ITEM = new BitItem(new Item.Properties());

	//Hack around "Cannot reference a field before it is defined"
	private static ItemStack getDiamondChiselStack() {
		return new ItemStack(DIAMOND_CHISEL);
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registry.BLOCK, new ResourceLocation(MODID, "bits_block"), BITS_BLOCK);
		BITS_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, "bitsandchisels:bits_block_entity", FabricBlockEntityTypeBuilder.create(BitsBlockEntity::new, BITS_BLOCK).build(null));
		Registry.register(Registry.ITEM, new ResourceLocation(MODID, "bits_block"), BITS_BLOCK_ITEM);
		Registry.register(Registry.ITEM, new ResourceLocation(MODID, "diamond_chisel"), DIAMOND_CHISEL);
		Registry.register(Registry.ITEM, new ResourceLocation(MODID, "ender_chisel"), ENDER_CHISEL);
		Registry.register(Registry.ITEM, new ResourceLocation(MODID, "iron_chisel"), IRON_CHISEL);
		Registry.register(Registry.ITEM, new ResourceLocation(MODID, "smart_chisel"), SMART_CHISEL);
		Registry.register(Registry.ITEM, new ResourceLocation(MODID, "bit_item"), BIT_ITEM);
		Registry.register(Registry.ITEM, new ResourceLocation(MODID, "wrench"), WRENCH_ITEM);
		Registry.register(Registry.ITEM, new ResourceLocation(MODID, "blueprint"), BLUEPRINT);
	}
	
}
