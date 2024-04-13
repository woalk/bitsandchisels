package com.woalk.mods.bitsandchisels.api.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

public interface AirSwingItem {
    @Environment(EnvType.CLIENT)
    void airSwing(ClientPlayerEntity player, ItemStack stack);
}
