package net.novucs.castle.entity

import net.redstoneore.legacyfactions.entity.FPlayer
import net.redstoneore.legacyfactions.entity.Faction
import net.redstoneore.legacyfactions.entity.FactionColl
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES

data class Castle(
        // Configuration
        var name: String,
        var region: Region,
        var wallMap: WallMap = WallMap(),
        var captureDuration: Long = MINUTES.toMillis(5),
        var rewardType: String = "default",
        var warp: Position? = null,

        // State
        var enabled: Boolean = false,
        var faction: String? = FactionColl.get().wilderness.id,
        @Transient var previousHead: Faction? = null,
        @Transient var capping: MutableList<FPlayer> = LinkedList(),
        @Transient var captureEnd: Long = 0,
        @Transient var lastBroadcast: Long = Long.MAX_VALUE
)
