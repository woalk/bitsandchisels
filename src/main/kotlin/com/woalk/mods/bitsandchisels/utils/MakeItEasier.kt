package com.woalk.mods.bitsandchisels.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i

fun blockPosOf(vec: Vec3i): BlockPos = BlockPos(vec.x, vec.y, vec.z)
