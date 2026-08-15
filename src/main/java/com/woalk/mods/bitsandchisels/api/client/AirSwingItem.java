package com.woalk.mods.bitsandchisels.api.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public interface AirSwingItem {
    @Environment(EnvType.CLIENT)
    void airSwing(LocalPlayer player, ItemStack stack);
}
