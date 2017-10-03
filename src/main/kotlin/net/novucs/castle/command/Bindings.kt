package net.novucs.castle.command

import com.sk89q.intake.parametric.ParameterException
import com.sk89q.intake.parametric.argument.ArgumentStack
import com.sk89q.intake.parametric.binding.BindingBehavior
import com.sk89q.intake.parametric.binding.BindingHelper
import com.sk89q.intake.parametric.binding.BindingMatch
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import net.novucs.castle.Reward
import net.novucs.castle.CastlesPlugin
import net.novucs.castle.entity.*
import net.novucs.castle.entity.*

@Suppress("unused")
class Bindings(private val plugin: CastlesPlugin) : BindingHelper() {

    @Suppress("MemberVisibilityCanPrivate")
    @BindingMatch(type = arrayOf(CommandSender::class), behavior = BindingBehavior.PROVIDES)
    @Throws(ParameterException::class)
    fun getCommandSender(context: ArgumentStack): CommandSender {
        return context.context.locals.get(CommandSender::class.java) ?:
                throw ParameterException("No command sender")
    }

    @BindingMatch(type = arrayOf(Region::class), behavior = BindingBehavior.PROVIDES)
    @Throws(ParameterException::class)
    fun getRegion(context: ArgumentStack): Region {
        val sender = getCommandSender(context) as? Player ?:
                throw ParameterException("Only players may execute this command")
        return plugin.worldEditHook.getSelectedRegion(sender).orElse(null) ?:
                throw ParameterException("Please select a valid region using WorldEdit")
    }

    @BindingMatch(type = arrayOf(Castle::class), behavior = BindingBehavior.CONSUMES, consumedCount = 1)
    @Throws(ParameterException::class)
    fun getCastle(context: ArgumentStack): Castle {
        return plugin.manager.byName(context.next()) ?:
                throw ParameterException("No castle by that name exists")
    }

    @BindingMatch(type = arrayOf(Reward::class), behavior = BindingBehavior.CONSUMES, consumedCount = 1)
    @Throws(ParameterException::class)
    fun getReward(context: ArgumentStack): Reward {
        return plugin.manager.settings!!.rewards[context.next()] ?:
                throw ParameterException("No reward by that name exists")
    }

    @BindingMatch(type = arrayOf(WallMap::class), behavior = BindingBehavior.CONSUMES, consumedCount = 1)
    @Throws(ParameterException::class)
    fun getWalls(context: ArgumentStack): WallMap {
        val material = Material.matchMaterial(context.next()) ?:
                throw ParameterException("Invalid material specified")

        val region = getRegion(context)
        val world = region.max.toLocation().world
        val walls = mutableMapOf<Position, Wall>()

        for (x in region.min.x..region.max.x) {
            for (y in region.min.y..region.max.y) {
                for (z in region.min.z..region.max.z) {
                    val block = world.getBlockAt(x,y,z)
                    if (block.type == material) {
                        val wall = Wall(block, plugin.manager.settings!!.wallStrength)
                        walls.put(wall.position, wall)
                    }
                }
            }
        }

        return WallMap(walls)
    }
}
