package net.novucs.castle

import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.entity.Player
import net.novucs.castle.entity.Region
import java.util.*

class WorldEditHook(private val plugin: CastlesPlugin) {

    private var worldEditPlugin: WorldEditPlugin? = null

    fun initialize() {
        val worldEdit = this.plugin.server.pluginManager.getPlugin("WorldEdit")
        if (worldEdit == null || worldEdit !is WorldEditPlugin || !worldEdit.isEnabled) {
            plugin.logger.severe("Failed to locate WorldEdit, disabling...")
            plugin.server.pluginManager.disablePlugin(plugin)
            return
        }
        this.worldEditPlugin = worldEdit
    }

    fun getSelectedRegion(player: Player): Optional<Region> {
        if (worldEditPlugin == null) {
            throw IllegalStateException("WorldEdit hook not initialized")
        }

        val selection = worldEditPlugin!!.getSelection(player) ?: return Optional.empty()
        val max = selection.maximumPoint
        val min = selection.minimumPoint

        return if (max == null || min == null) {
            Optional.empty()
        } else Optional.of(Region(max, min))

    }
}
