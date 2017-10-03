package net.novucs.castle

import com.sk89q.intake.CommandException
import com.sk89q.intake.InvalidUsageException
import com.sk89q.intake.context.CommandLocals
import com.sk89q.intake.dispatcher.SimpleDispatcher
import com.sk89q.intake.parametric.ParametricBuilder
import com.sk89q.intake.util.auth.AuthorizationException
import org.apache.commons.lang.StringUtils
import org.bukkit.ChatColor.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import net.novucs.castle.command.Bindings
import net.novucs.castle.command.Executor
import net.novucs.castle.entity.Castle
import net.novucs.castle.entity.Region
import net.novucs.castle.entity.WallMap

class CastlesPlugin : JavaPlugin() {

    private val playerListener = PlayerListener(this)
    val dispatcher = SimpleDispatcher()
    val manager = Manager(this)
    val worldEditHook = WorldEditHook(this)

    override fun onEnable() {
        worldEditHook.initialize()
        manager.initialize()
        registerCommands()
        server.pluginManager.registerEvents(playerListener, this)
    }

    override fun onDisable() {
        manager.terminate()
    }

    private fun registerCommands() {
        val builder = ParametricBuilder()
        builder.addBinding(
                Bindings(this),
                CommandSender::class.java,
                Region::class.java,
                Castle::class.java,
                Reward::class.java,
                WallMap::class.java
        )
        builder.registerMethodsAsCommands(dispatcher, Executor(this))
        builder.setAuthorizer { locals, permission ->
            val sender = locals.get(CommandSender::class.java)
            sender != null && sender.hasPermission(permission)
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val message = if (args.isEmpty()) "help" else StringUtils.join(args, " ")
        val locals = CommandLocals()
        locals.put(CommandSender::class.java, sender)
        val isConsole = sender === server.consoleSender

        try {
            dispatcher.call(message, locals, arrayOfNulls(0))
        } catch (e: InvalidUsageException) {
            sender.sendMessage("$DARK_AQUA${e.getSimpleUsageString((if (isConsole) "" else "/") + "castle ")}")
            sender.sendMessage("${DARK_RED}Error: $RED${e.message}")
        } catch (e: AuthorizationException) {
            sender.sendMessage("${RED}Permission denied.")
        } catch (e: CommandException) {
            throw RuntimeException(e)
        }

        return true
    }
}
