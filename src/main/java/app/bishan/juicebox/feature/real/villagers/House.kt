package app.bishan.juicebox.feature.real.villagers

import app.bishan.juicebox.JuiceboxPlugin
import org.bukkit.Location
import org.bukkit.Tag
import org.bukkit.block.Bed
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.block.data.type.Door
import kotlin.math.pow

class House private constructor() {
	companion object {
		val HOUSE_MARKER = JuiceboxPlugin.key("house_marker")

		private const val MAX_HOUSE_SIZE = 1000

		fun asHouse(bed: Bed): House? {
			val block = bed.block
			val loc = block.location

//			 look for a door in a 10x10x10 cube around the bed with flood fill
			var door: Block? = null
			val blocks = mutableSetOf<Block>()
			val queue = mutableListOf<Block>()
			queue.add(block)
			while (queue.isNotEmpty()) {
				val current = queue.removeAt(0)
				if (blocks.contains(current)) continue
				blocks.add(current)

				if (blocks.size > MAX_HOUSE_SIZE) return null

				if (Tag.WOODEN_DOORS.isTagged(current.type)) {
					door = current
					break
				}
				if (current.isSolid || current.isCollidable) continue

				for (x in -1..1) {
					for (y in -1..1) {
						for (z in -1..1) {
							val b = loc.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
							queue.add(b)
						}
					}
				}
			}
			if (door == null) return null

			return null
		}
	}
}
