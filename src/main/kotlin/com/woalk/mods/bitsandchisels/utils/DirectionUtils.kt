package com.woalk.mods.bitsandchisels.utils

import com.sk89q.worldedit.math.Vector3
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i
import kotlin.math.abs
import kotlin.math.atan2

fun getNearestDirectionForVector(pos: Vec3i): Direction {
    val wePos = Vector3.at(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    return wePos.toYaw().run {
        val northYaw = com.sk89q.worldedit.util.Direction.NORTH.toVector().toYaw()
        val eastYaw = com.sk89q.worldedit.util.Direction.EAST.toVector().toYaw()
        val southYaw = com.sk89q.worldedit.util.Direction.SOUTH.toVector().toYaw()
        val westYaw = com.sk89q.worldedit.util.Direction.WEST.toVector().toYaw()
        mapOf(
            Pair(Direction.NORTH, abs(this - northYaw)),
            Pair(Direction.EAST, abs(this - eastYaw)),
            Pair(Direction.SOUTH, abs(this - southYaw)),
            Pair(Direction.WEST, abs(this - westYaw)),
        ).minBy { it.value }.key
    }
}

fun Vec3i.toYaw(): Double = Math.toDegrees(((atan2(-x.toDouble(), z.toDouble()) + 2 * Math.PI) % (2 * Math.PI)))
