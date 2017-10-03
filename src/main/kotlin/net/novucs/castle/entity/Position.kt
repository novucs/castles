package net.novucs.castle.entity

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

infix fun Player.warpTo(position: Position) {
    player.teleport(position.toTeleportFriendlyLocation(player))
}

data class Position(
        val world: String,
        val x: Int,
        val y: Int,
        val z: Int) {

    constructor(location: Location) : this(location.world.name,
            location.blockX, location.blockY, location.blockZ)

    fun toLocation(): Location {
        return Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun toTeleportFriendlyLocation(player: Player): Location {
        val world = Bukkit.getWorld(world)
        val x = this.x.toDouble() + 0.5
        val y = this.y.toDouble()
        val z = this.z.toDouble() + 0.5
        val yaw = player.location.yaw
        val pitch = player.location.pitch
        return Location(world, x, y, z, yaw, pitch)
    }
}
