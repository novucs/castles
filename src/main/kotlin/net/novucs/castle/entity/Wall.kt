package net.novucs.castle.entity

import org.bukkit.Material
import org.bukkit.block.Block

data class WallMap(
        val map: MutableMap<Position, Wall> = mutableMapOf()
)

data class Wall(val position: Position,
                val material: Material,
                @Transient var strength: Int) {
    constructor(block: Block, strength: Int) : this(Position(block.location), block.type, strength)
}
