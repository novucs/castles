package net.novucs.castle.entity

import org.bukkit.Location

@Suppress("MemberVisibilityCanPrivate")
class Region {

    val max: Position
    val min: Position
    @Transient var center: Lazy<Position>

    constructor(pos1: Position, pos2: Position) {
        if (pos1.world != pos2.world) {
            throw IllegalArgumentException("Positions of a region must be in the same world")
        }

        val world = pos1.world
        val maxX = Math.max(pos1.x, pos2.x)
        val maxY = Math.max(pos1.y, pos2.y)
        val maxZ = Math.max(pos1.z, pos2.z)
        max = Position(world, maxX, maxY, maxZ)

        val minX = Math.min(pos1.x, pos2.x)
        val minY = Math.min(pos1.y, pos2.y)
        val minZ = Math.min(pos1.z, pos2.z)
        min = Position(world, minX, minY, minZ)

        center = lazy {
            val x = (max.x + min.x) / 2
            val y = (max.y + min.y) / 2
            val z = (max.z + min.z) / 2
            Position(world, x, y, z)
        }
    }

    constructor(loc1: Location, loc2: Location) : this(Position(loc1), Position(loc2))

    operator fun contains(location: Location): Boolean {
        return max.world == location.world.name &&
                max.x >= location.x && min.x <= location.x &&
                max.y >= location.y && min.y <= location.y &&
                max.z >= location.z && min.z <= location.z
    }

    fun collides(other: Region): Boolean {
        return max.world == other.max.world &&
                min.x <= other.max.x && max.x >= other.min.x &&
                min.y <= other.max.y && max.y >= other.min.y &&
                min.z <= other.max.z && max.z >= other.min.z
    }
}
