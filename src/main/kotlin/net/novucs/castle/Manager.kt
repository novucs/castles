package net.novucs.castle

import com.google.common.collect.ImmutableList
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.novucs.castle.entity.*
import net.redstoneore.legacyfactions.entity.FPlayerColl
import net.redstoneore.legacyfactions.entity.Faction
import net.redstoneore.legacyfactions.entity.FactionColl
import org.bukkit.ChatColor.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import net.novucs.castle.entity.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

class Manager(private val plugin: CastlesPlugin) {

    private val castles = HashMap<String, Castle>()
    var settings: Settings? = null
    val warping = mutableMapOf<Player, BukkitTask>()

    private val castlesFile by lazy {
        val file = File(plugin.dataFolder.toString() + File.separator + "castles.json")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
            FileWriter(file).use { writer ->
                GsonBuilder().setPrettyPrinting().create().toJson(castles, writer)
            }
        }
        return@lazy file
    }

    private val settingsFile by lazy {
        val file = File(plugin.dataFolder.toString() + File.separator + "settings.json")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
            FileWriter(file).use { writer ->
                GsonBuilder().setPrettyPrinting().create().toJson(DEFAULT_SETTINGS, writer)
            }
        }
        return@lazy file
    }

    @Throws(RegionCollisionException::class)
    fun add(name: String, castle: Castle) {
        castles.values
                .filter { it.region.collides(castle.region) }
                .forEach { throw RegionCollisionException(it) }

        castles.put(name.toLowerCase(), castle)
    }

    fun delete(castle: Castle) {
        castles.remove(castle.name)
    }

    fun rename(castle: Castle, name: String) {
        castles.remove(castle.name.toLowerCase())
        castles.put(name.toLowerCase(), castle)
        castle.name = name
    }

    fun byName(name: String): Castle? {
        return castles[name.toLowerCase()]
    }

    fun byLocation(location: Location): Castle? {
        return castles.values.firstOrNull { it.region.contains(location) }
    }

    fun all(): Collection<Castle> {
        return Collections.unmodifiableCollection(castles.values)
    }

    fun beginCapture(castle: Castle, player: Player) {
        castle.capping.add(FPlayerColl.get(player))
    }

    fun disable(castle: Castle) {
        castle.enabled = false

        if (castle.faction != FactionColl.get().wilderness.id) {
            plugin.manager.loss(castle, null)
        }

        castle.previousHead = null
    }

    fun initialize() {
        loadSettings()
        loadCastles()
        plugin.server.scheduler.runTaskTimer(plugin, this::tick, 1, 1)
    }

    fun terminate() {
        saveCastles()
    }

    private fun tick() {
        castles.values.forEach(this::tickCastle)
    }

    private fun tickCastle(castle: Castle) {
        // Do nothing if castle is disabled.
        if (!castle.enabled) return

        // Remove all players not part of a faction.
        val wilderness = FactionColl.get().wilderness.id
        castle.capping.removeIf {
            it.factionId == wilderness
        }

        // Do nothing if the castle has nobody in the region.
        if (castle.capping.size == 0) {
            castle.previousHead = null
            return
        }

        // Remove all players no longer in the castle region.
        castle.capping.removeIf {
            !castle.region.contains(it.player.location)
        }

        val owner = FactionColl.get(castle.faction) ?: FactionColl.get().wilderness

        // All capturing players have now left the castle region.
        if (castle.capping.size == 0) {
            castle.previousHead?.let {
                // Do nothing if the player previously at this castle has
                // already captured it.
                if (castle.faction == it.id) return@let

                // Broadcast the claim ownership being maintained.
                if (castle.faction == wilderness) {
                    plugin.server.broadcastMessage("$YELLOW${it.tag} are no longer capturing castle ${castle.name}")
                } else {
                    plugin.server.broadcastMessage("$YELLOW${owner.tag} have kept their claim over castle ${castle.name}")
                }
            }

            castle.previousHead = null
            return
        }

        // Get the current capturing head faction.
        val currentHead = castle.capping[0].faction

        // Update the capture end and last broadcast times when there is a new
        // faction capturing the castle.
        if (currentHead != castle.previousHead) {
            castle.captureEnd = System.currentTimeMillis() + castle.captureDuration
            castle.lastBroadcast = castle.captureDuration + 1

            if (currentHead.id == castle.faction) {
                // Broadcast the claim ownership has been maintained if the new
                // faction head already owns this castle.
                if (castle.previousHead != null) {
                    plugin.server.broadcastMessage("$YELLOW${owner.tag} have kept their claim over castle ${castle.name}")
                }

                castle.previousHead = currentHead
                return
            }

            // Otherwise broadcast the new faction claiming this castle.
            plugin.server.broadcastMessage("$YELLOW${currentHead.tag} are now capturing castle ${castle.name}")
            castle.previousHead = currentHead
        }

        // Don't try to claim when already owning castle.
        if (currentHead == owner) return

        // Calculate the remaining time until the capture is complete.
        val remainingMillis = castle.captureEnd - System.currentTimeMillis()

        // Broadcast the capture countdown.
        run broadcast@ {
            BROADCAST_INTERVALS.forEach {
                if (castle.lastBroadcast <= it || it <= remainingMillis) {
                    return@forEach
                }

                castle.lastBroadcast = it
                val time = TimeUtils.formatTimeShorthand(TimeUnit.MILLISECONDS, it)
                val action = if (castle.faction == wilderness) "capturing" else "neutralizing"
                plugin.server.broadcastMessage("$YELLOW${currentHead.tag} now have $time until $action castle ${castle.name}")
                return@broadcast
            }
        }

        // Complete the castle capture when the timer is finished.
        if (remainingMillis <= 0) {
            if (castle.faction == wilderness) {
                win(castle, currentHead)
                plugin.server.broadcastMessage("$YELLOW${currentHead.tag} have captured castle ${castle.name}")
            } else {
                loss(castle, currentHead)
                plugin.server.broadcastMessage("$YELLOW${currentHead.tag} have neutralized castle ${castle.name}")
            }
            castle.captureEnd = System.currentTimeMillis() + castle.captureDuration
            castle.lastBroadcast = castle.captureDuration + 1
        }
    }

    private fun loss(castle: Castle, claimers: Faction?) {
        val loosingFaction = FactionColl.get(castle.faction) ?: FactionColl.get().wilderness
        settings!!.rewards[castle.rewardType]?.let { reward ->
            // Execute all the reward commands.
            reward.loss.commands?.forEach { command -> executeCommand(command, loosingFaction) }
        }
        castle.faction = FactionColl.get().wilderness.id
    }

    private fun win(castle: Castle, claimers: Faction) {
        settings!!.rewards[castle.rewardType]?.let { reward ->
            // Execute all the reward commands.
            reward.win.commands?.forEach { command -> executeCommand(command, claimers) }
        }
        castle.faction = claimers.id
        castle.wallMap.map.forEach { (position, wall) ->
            position.toLocation().block.type = wall.material
            wall.strength = settings!!.wallStrength
        }
    }

    private fun executeCommand(command: String, faction: Faction) {
        val commands = LinkedList<String>()
        val factionTag = faction.comparisonTag

        if (command.contains("{player}")) {
            faction.members.forEach { player ->
                commands.add(command
                        .replace("{player}", player.name)
                        .replace("{faction}", factionTag))
            }
        } else {
            commands.add(command.replace("{faction}", factionTag))
        }

        commands.forEach {
            plugin.server.dispatchCommand(plugin.server.consoleSender, it)
        }
    }

    fun loadSettings() {
        settings = Gson().fromJson<Settings>(FileReader(settingsFile), Settings::class.java)
    }

    private fun loadCastles() {
        val castles = GsonBuilder()
                .registerTypeAdapter(Material::class.java, MaterialDeserializer())
                .registerTypeAdapter(WallMap::class.java, WallMapDeserializer())
                .create()
                .fromJson<Map<String, Castle>>(
                        FileReader(castlesFile),
                        object : TypeToken<Map<String, Castle>>() {}.type
                )

        castles.values.forEach {
            it.capping = LinkedList()
            it.captureEnd = 0
            it.lastBroadcast = it.captureDuration + 1
            it.region.center = lazy {
                val max = it.region.max
                val min = it.region.min
                val world = max.world
                val x = (max.x + min.x) / 2
                val y = (max.y + min.y) / 2
                val z = (max.z + min.z) / 2
                Position(world, x, y, z)
            }
            it.wallMap.map.forEach { (_, wall) ->
                wall.strength = settings!!.wallStrength
            }
        }

        this.castles.putAll(castles)
    }

    private fun saveCastles() {
        FileWriter(castlesFile).use { writer ->
            GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(Material::class.java, MaterialSerializer())
                    .registerTypeAdapter(WallMap::class.java, WallMapSerializer())
                    .create()
                    .toJson(castles, writer)
        }
    }

    fun warp(player: Player, castle: Castle) {
        val warp = castle.warp

        if (warp == null) {
            player.sendMessage("${RED}This castle has no warp set")
            return
        }

        if (player.hasPermission("castle.warps.nowarmup")) {
            player warpTo warp
            player.sendMessage("${GREEN}Successfully warped to castle ${castle.name}")
            return
        }

        warping.remove(player)?.let {
            it.cancel()
            player.sendMessage("${RED}Pending warp cancelled")
        }

        val warmUp = plugin.manager.settings!!.warpWarmUp.toLong()
        val message = TimeUtils.formatTimeLonghand(SECONDS, warmUp)
        player.sendMessage("${YELLOW}Warping in $LIGHT_PURPLE$message$YELLOW. Do not move.")

        val task = plugin.server.scheduler.runTaskLater(plugin, {
            if (warping.remove(player) != null) {
                player warpTo warp
                player.sendMessage("${GREEN}Successfully warped to castle ${castle.name}")
            }
        }, warmUp * 20)

        warping.put(player, task)
    }

    companion object {
        private val BROADCAST_INTERVALS = ImmutableList.of<Long>(
                MINUTES.toMillis(10),
                MINUTES.toMillis(5),
                MINUTES.toMillis(4),
                MINUTES.toMillis(3),
                MINUTES.toMillis(2),
                MINUTES.toMillis(1),
                SECONDS.toMillis(30),
                SECONDS.toMillis(10),
                SECONDS.toMillis(5),
                SECONDS.toMillis(4),
                SECONDS.toMillis(3),
                SECONDS.toMillis(2),
                SECONDS.toMillis(1)
        )

        private class MaterialDeserializer : JsonDeserializer<Material> {
            override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Material {
                return Material.matchMaterial(json.asString)
            }
        }

        private class MaterialSerializer : JsonSerializer<Material> {
            override fun serialize(material: Material, type: Type, context: JsonSerializationContext): JsonElement {
                return context.serialize(material.name)
            }
        }

        private class WallMapDeserializer : JsonDeserializer<WallMap> {
            override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): WallMap {
                val walls = mutableMapOf<Position, Wall>()
                json.asJsonArray.forEach {
                    val wallJson = it.asJsonObject
                    val positionJson = wallJson.get("position").asJsonObject
                    val position = Position(
                            positionJson.get("world").asString,
                            positionJson.get("x").asInt,
                            positionJson.get("y").asInt,
                            positionJson.get("z").asInt
                    )

                    val material = context.deserialize<Material>(wallJson.get("material"), Material::class.java)
                    val wall = Wall(position, material, 0)
                    walls.put(position, wall)
                }
                return WallMap(walls)
            }
        }

        private class WallMapSerializer : JsonSerializer<WallMap> {
            override fun serialize(wallMap: WallMap, type: Type, context: JsonSerializationContext): JsonElement {
                return context.serialize(wallMap.map.values)
            }
        }
    }
}
