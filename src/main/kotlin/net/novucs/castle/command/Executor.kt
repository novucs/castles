package net.novucs.castle.command

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableListMultimap
import com.sk89q.intake.Command
import com.sk89q.intake.Require
import com.sk89q.intake.parametric.annotation.Optional
import net.redstoneore.legacyfactions.entity.FPlayerColl
import net.redstoneore.legacyfactions.entity.FactionColl
import org.bukkit.ChatColor.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import net.novucs.castle.RegionCollisionException
import net.novucs.castle.Reward
import net.novucs.castle.TimeUtils
import net.novucs.castle.CastlesPlugin
import net.novucs.castle.entity.Castle
import net.novucs.castle.entity.Position
import net.novucs.castle.entity.Region
import net.novucs.castle.entity.WallMap
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("unused")
class Executor(private val plugin: CastlesPlugin) {

    private val help: ImmutableList<String> by lazy {
        val help = ImmutableList.builder<String>()
        help.add(header("Help"))
        plugin.dispatcher.commands.sortedBy { it.primaryAlias }.forEach {
            val joiner = StringJoiner(" ", it.primaryAlias + " ", "")

            it.description.parameters.forEach {
                if (it.isOptional) {
                    joiner.add("[" + it.name + "]")
                } else {
                    joiner.add("<" + it.name + ">")
                }
            }

            val command = joiner.toString()
            val description = it.description.shortDescription
            help.add("$GREEN/castle $command$WHITE $description")
        }
        return@lazy help.build()
    }

    private val perCommandHelp: ImmutableListMultimap<String, String> by lazy {
        val help = ImmutableListMultimap.builder<String, String>()
        plugin.dispatcher.commands.sortedBy { it.primaryAlias }.forEach {
            help.put(it.primaryAlias, header("Help"))
            val joiner = StringJoiner(" ", it.primaryAlias + " ", "")

            it.description.parameters.forEach {
                if (it.isOptional) {
                    joiner.add("[" + it.name + "]")
                } else {
                    joiner.add("<" + it.name + ">")
                }
            }

            val command = joiner.toString()
            val description = it.description.shortDescription
            help.put(it.primaryAlias, "$GREEN/castle $command$WHITE $description")

            if (it.description.help != null) {
                help.put(it.primaryAlias, it.description.help)
            }
        }
        return@lazy help.build()
    }

    private fun header(title: String): String {
        return "$YELLOW---------- [ ${WHITE}Castle $title $YELLOW] ----------"
    }

    @Command(aliases = arrayOf("help"), desc = "Lists the help")
    @Require("castle.help")
    fun help(sender: CommandSender, @Alias @Optional command: String?) {
        val page = perCommandHelp[command]
        if (page.isEmpty()) {
            help.forEach(sender::sendMessage)
        } else {
            page.forEach(sender::sendMessage)
        }
    }

    @Command(aliases = arrayOf("create"), desc = "Creates a new castle")
    @Require("castle.create")
    fun create(sender: CommandSender, region: Region, @Name name: String) {
        try {
            val castle = Castle(name, region)
            plugin.manager.add(name, castle)
            sender.sendMessage("${GREEN}Castle $name successfully created")
        } catch (e: RegionCollisionException) {
            sender.sendMessage("$RED${e.message}")
        }
    }

    @Command(aliases = arrayOf("resize"), desc = "Modifies a castle region")
    @Require("castle.resize")
    fun resize(sender: CommandSender, castle: Castle, region: Region) {
        castle.region = region
        sender.sendMessage("${GREEN}Castle ${castle.name} successfully updated")
    }

    @Command(aliases = arrayOf("rename"), desc = "Renames a castle")
    @Require("castle.rename")
    fun rename(sender: CommandSender, castle: Castle, @Name name: String) {
        val withSameName = plugin.manager.byName(name)
        if (withSameName != null && withSameName != castle) {
            sender.sendMessage("${RED}A castle by that name already exists")
            return
        }

        sender.sendMessage("${GREEN}Successfully renamed castle ${castle.name} to $name")
        plugin.manager.rename(castle, name)
    }

    @Command(aliases = arrayOf("delete"), desc = "Deletes a castle")
    @Require("castle.delete")
    fun delete(sender: CommandSender, castle: Castle) {
        plugin.manager.delete(castle)
        sender.sendMessage("${GREEN}Castle ${castle.name} successfully deleted")
    }

    @Command(aliases = arrayOf("list"), desc = "Lists all castles")
    @Require("castle.list")
    fun list(sender: CommandSender) {
        sender.sendMessage(header("List"))
        var i = 1
        plugin.manager.all().forEach {
            val owner = FactionColl.get(it.faction) ?: FactionColl.get().wilderness
            sender.sendMessage("${i++}. $GREEN${it.name} $WHITE- Held by ${owner.tag}")
        }
    }

    @Command(aliases = arrayOf("info"), desc = "Displays castle info")
    @Require("castle.info")
    fun info(sender: CommandSender, castle: Castle) {
        val holder = FactionColl.get(castle.faction) ?: FactionColl.get().wilderness
        val captureDuration = TimeUtils.formatTimeLonghand(TimeUnit.MILLISECONDS, castle.captureDuration)
        val center = castle.region.center.value

        val holderTag = if (sender is Player) {
            holder.getTag(FactionColl.get(sender))
        } else {
            holder.tag
        }

        listOf(
                header("Info | ${castle.name}"),
                "Status: ${if (castle.enabled) "${DARK_GREEN}ENABLED" else "${RED}DISABLED"}",
                "Reward Type: ${castle.rewardType}",
                "Holder: $holderTag",
                "Capture duration: $GREEN$captureDuration",
                "Location: $GREEN${center.world} ${center.x},${center.y},${center.z}"
        ).forEach(sender::sendMessage)
    }

    @Command(aliases = arrayOf("duration"), desc = "Sets the capture duration")
    @Require("castle.duration")
    fun duration(sender: CommandSender, castle: Castle, @Seconds duration: Int) {
        castle.captureDuration = TimeUnit.SECONDS.toMillis(duration.toLong())
        val captureDuration = TimeUtils.formatTimeLonghand(TimeUnit.MILLISECONDS, castle.captureDuration)
        sender.sendMessage("${GREEN}Castle ${castle.name} now has a capture duration of $captureDuration")
    }

    @Command(aliases = arrayOf("enable"), desc = "Enables a castle")
    @Require("castle.enable")
    fun enable(sender: CommandSender, castle: Castle) {
        if (castle.enabled) {
            sender.sendMessage("${RED}Castle ${castle.name} is already enabled")
            return
        }

        castle.enabled = true
        plugin.server.broadcastMessage("${YELLOW}Castle ${castle.name} is now running")
        sender.sendMessage("${GREEN}Successfully enabled ${castle.name}")
    }

    @Command(aliases = arrayOf("disable"), desc = "Disables a castle")
    @Require("castle.disable")
    fun disable(sender: CommandSender, castle: Castle) {
        if (!castle.enabled) {
            sender.sendMessage("${RED}Castle ${castle.name} is already disabled")
            return
        }

        plugin.manager.disable(castle)
        plugin.server.broadcastMessage("${YELLOW}Castle ${castle.name} is no longer running")
        sender.sendMessage("${GREEN}Successfully disabled ${castle.name}")
    }

    @Command(aliases = arrayOf("reward"), desc = "Sets the reward")
    @Require("castle.reward")
    fun reward(sender: CommandSender, castle: Castle, reward: Reward) {
        castle.rewardType = reward.name.toLowerCase()
        sender.sendMessage("${GREEN}Castle successfully updated")
    }

    @Command(aliases = arrayOf("reload"), desc = "Reloads the settings")
    @Require("castle.reload")
    fun reload(sender: CommandSender) {
        plugin.manager.loadSettings()
        sender.sendMessage("${GREEN}Successfully reloaded the settings")
    }

    @Command(aliases = arrayOf("addwalls"), desc = "Adds walls")
    @Require("castle.walls.add")
    fun addWalls(sender: CommandSender, castle: Castle, @Material wallMap: WallMap) {
        var count = 0
        wallMap.map.forEach { (position, wall) ->
            if (castle.wallMap.map.put(position, wall) == null) {
                count++
            }
        }
        sender.sendMessage("${GREEN}Successfully added $count blocks to the castle ${castle.name}'s walls")
    }

    @Command(aliases = arrayOf("remwalls"), desc = "Removes walls")
    @Require("castle.walls.remove")
    fun remWalls(sender: CommandSender, castle: Castle, @Material wallMap: WallMap) {
        var count = 0
        wallMap.map.forEach { position, wall ->
            if (castle.wallMap.map.remove(position) != null) {
                count++
            }
        }
        sender.sendMessage("${GREEN}Successfully removed $count blocks from the castle ${castle.name}'s walls")
    }

    @Command(aliases = arrayOf("clearwalls"), desc = "Clears walls")
    @Require("castle.walls.clear")
    fun clearWalls(sender: CommandSender, castle: Castle) {
        castle.wallMap.map.clear()
        sender.sendMessage("${GREEN}Successfully cleared all wall blocks for castle ${castle.name}")
    }

    @Command(aliases = arrayOf("setwarp"), desc = "Sets a castle warp")
    @Require("castle.warps.set")
    fun setWarp(sender: CommandSender, castle: Castle) {
        if (sender !is Player) {
            sender.sendMessage("${RED}Only players may execute this command")
            return
        }

        castle.warp = Position(sender.location)
        sender.sendMessage("${GREEN}Successfully set the warp for castle ${castle.name}")
    }

    @Command(aliases = arrayOf("warp"), desc = "Warps to a castle")
    @Require("castle.warps.use")
    fun warp(sender: CommandSender, castle: Castle) {
        if (sender !is Player) {
            sender.sendMessage("${RED}Only players may execute this command")
            return
        }

        if (castle.faction != FPlayerColl.get(sender).factionId) {
            sender.sendMessage("${RED}Only players who own this castle may warp here")
            return
        }

        plugin.manager.warp(sender, castle)
    }
}
