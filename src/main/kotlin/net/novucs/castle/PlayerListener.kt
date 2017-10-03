package net.novucs.castle

import net.redstoneore.legacyfactions.entity.FPlayerColl
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.YELLOW
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import net.novucs.castle.entity.Position
import net.novucs.castle.entity.Wall

class PlayerListener(private val plugin: CastlesPlugin) : Listener {

    @Suppress("unused")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        if (hasMovedBlock(event.from, event.to)) {
            val player = event.player

            plugin.manager.warping.remove(player)?.let {
                it.cancel()
                player.sendMessage("${RED}Pending warp cancelled")
            }

            plugin.manager.byLocation(player.location)?.let {
                plugin.manager.beginCapture(it, player)
            }
        }
    }

    private fun hasMovedBlock(from: Location, to: Location): Boolean {
        return from.blockX != to.blockX ||
                from.blockY != to.blockY ||
                from.blockZ != to.blockZ
    }

    @Suppress("unused")
    @EventHandler(priority = EventPriority.LOWEST)
    fun onBreak(event: BlockBreakEvent) {
        val wall = getCastleWall(event.block) ?: return
        event.isCancelled = true
        wall.strength--

        if (wall.strength <= 0) {
            event.block.type = Material.AIR
            return
        }

        event.player.sendMessage("${YELLOW}Wall damaged, now at ${wall.strength} strength")
    }

    private fun getCastleWall(block: Block): Wall? {
        val position = Position(block.location)
        plugin.manager.all().forEach { castle ->
            val wall = castle.wallMap.map[position]
            if (wall != null) {
                return wall
            }
        }
        return null
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        val player = event.player

        plugin.manager.warping.remove(player)?.let {
            it.cancel()
            player.sendMessage("${RED}Pending warp cancelled")
        }

        plugin.manager.byLocation(event.to)?.let { castle ->
            if (castle.faction == FPlayerColl.get(player).factionId) return
            if (player.hasPermission("castle.warps.use.others")) return

            player.sendMessage("${RED}You are not allowed to warp to castles that you do not own")
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return

        plugin.manager.warping.remove(player)?.let {
            it.cancel()
            player.sendMessage("${RED}Pending warp cancelled")
        }
    }
}
