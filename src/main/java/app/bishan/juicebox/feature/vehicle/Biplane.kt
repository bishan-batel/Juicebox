package app.bishan.juicebox.feature.vehicle

import net.minecraft.world.entity.vehicle.ChestBoat
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftChestBoat

class Biplane(loc: Location) : ChestBoat((loc.world as CraftWorld).handle, loc.x, loc.y, loc.z) {
	init {
		location.x = loc.x
		location.y = loc.y
		location.z = loc.z
		location.yaw = loc.yaw
		location.pitch = loc.pitch

		val boat = CraftChestBoat(Bukkit.getServer() as CraftServer, this)
		boat.customName
	}

	override fun getRowingTime(i: Int, f: Float): Float {
		return super.getRowingTime(i, f)
	}
}
