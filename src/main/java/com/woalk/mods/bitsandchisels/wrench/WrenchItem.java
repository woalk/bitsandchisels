package com.woalk.mods.bitsandchisels.wrench;

import com.woalk.mods.bitsandchisels.BitsAndChisels;
import com.woalk.mods.bitsandchisels.BitsBlockEntity;
import com.woalk.mods.bitsandchisels.registry.ModPermissions;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction.Axis;

import java.util.Objects;

public class WrenchItem extends Item {

    public WrenchItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide &&
                context.getLevel().getBlockState(context.getClickedPos()).is(BitsAndChisels.BITS_BLOCK) &&
                Permissions.check(Objects.requireNonNull(context.getPlayer()),
                        ModPermissions.USE_WRENCH, true)) {
            BlockEntity e1 = context.getLevel().getBlockEntity(context.getClickedPos());
            if (e1 instanceof BitsBlockEntity) {
                BitsBlockEntity e = (BitsBlockEntity) e1;
                if (context.getPlayer().isShiftKeyDown()){
                    invert(context.getClickedFace().getAxis(), e);
                }
                else {
                    rotate(context.getClickedFace().getAxis(), e);
                }

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
    void invert(Axis axis, BitsBlockEntity e) {
        BlockState[][][] rotated = new BlockState[16][16][16];
        switch(axis) {
            case X:
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        for (int k = 0; k < 16; k++) {
                            rotated[i][j][k] = e.getState(i, j, -k+15);
                        }
                    }
                }
                break;
            case Y:
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        for (int k = 0; k < 16; k++) {
                            rotated[i][j][k] = e.getState( i, -j+15,k);
                        }
                    }
                }
                break;
            case Z:
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        for (int k = 0; k < 16; k++) {
                            rotated[i][j][k] = e.getState(-i+15, j, k);
                        }
                    }
                }
                break;
        }
        e.setStates(rotated);
        e.rebuildServer();
        e.sync();
    }


    //Rotation Algorithm from https://stackoverflow.com/questions/53110374/how-to-rotate-2-d-array-in-java, extended to 3D
    void rotate(Axis axis, BitsBlockEntity e) {
        BlockState[][][] rotated = new BlockState[16][16][16];
        switch(axis) {
            case X:
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        for (int k = 0; k < 16; k++) {
                            rotated[i][j][k] = e.getState(i, 16 - k - 1, j);
                        }
                    }
                }
                break;
            case Y:
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        for (int k = 0; k < 16; k++) {
                            rotated[i][j][k] = e.getState(16 - k - 1, j, i);
                        }
                    }
                }
                break;
            case Z:
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        for (int k = 0; k < 16; k++) {
                            rotated[i][j][k] = e.getState(16 - j - 1, i, k);
                        }
                    }
                }
                break;
        }
        e.setStates(rotated);
        e.rebuildServer();
        e.sync();
    }
    
}
