package com.woalk.mods.bitsandchisels.utils;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

/**
 * We live in a society
 */
public class ColorPolice {
    static final Logger LOGGER = LoggerFactory.getLogger("B&C Color Police");
    static final HashSet<String> CRIMINALS = new HashSet<>();

    private ColorPolice() { }

    public static int getBlockColor(BlockState state, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos, int tintIndex) {
        try {
            BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(state, tintIndex);
            if (world instanceof BlockAndTintGetter clientWorld && pos != null) {
                return tintSource.colorInWorld(state, clientWorld, pos);
            }
            return tintSource.color(state);
        } catch (Exception e) {
            String criminal = null;
            ModContainer criminalMod = null;
            for (StackTraceElement el : e.getStackTrace()) {
                if (!el.getClassName().startsWith("net.minecraft")) {
                    criminal = el.getClassName();
                    break;
                }
            }
            if (CRIMINALS.contains(criminal)) return -1;
            CRIMINALS.add(criminal);
            if (criminal != null) {
                try {
                    String crimePath = criminal.replace('.', '/') + ".class";
                    URI ccl = ColorPolice.class.getClassLoader().getResource(crimePath).toURI();
                    URI crimeLocation = new URI(ccl.toString().replace(crimePath, ""));
                    Path crimeP = Paths.get(crimeLocation);
                    scan:
                    for (ModContainer mc : FabricLoader.getInstance().getAllMods()) {
                        for (Path p : mc.getRootPaths()) {
                            if (p.equals(crimeP)) {
                                criminalMod = mc;
                                break scan;
                            }
                        }
                    }
                } catch (Exception e2) {
                    // noop
                }
            }
            stopYouveViolatedTheLaw("Exception thrown in a color provider; This is likely a serious issue! Full stacktrace in logs");
            if (criminalMod != null) {
                stopYouveViolatedTheLaw("Caused by mod: " + criminalMod.getMetadata().getName() + " (" + criminalMod.getMetadata().getId() + ")");
            }
            if (criminal != null) {
                stopYouveViolatedTheLaw("Caused in class: " + criminal);
            }
            LOGGER.error("Stacktrace: ", e);
        }
        return -1;
    }

    static void stopYouveViolatedTheLaw(String message) {
        Minecraft.getInstance().gui.chatListener().handleSystemMessage(Component.literal(message), false);
        LOGGER.error(message);
    }
}
