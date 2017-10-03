package net.novucs.castle

import net.novucs.castle.entity.Castle

class RegionCollisionException(regionHolder: Castle) :
        Exception("Selected region overlaps with an existing castle named " + regionHolder.name)
