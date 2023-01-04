package app.bishan.juicebox.feature.vehicle

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector

object SuburuForester : VehicleBase<Pig>("suburu_forester", false) {
	private val seatModel = ArmorStandComponent(this, "seat").apply {
		onCreate {
			it.world.spawnEntity(it.location, EntityType.ARMOR_STAND) as ArmorStand
		}

		position(Vector(2.0, 0.0, 0.0))
	}

	override fun createAllModels() = listOf(
		seatModel
	)

	override fun onEnable() {
		super.onEnable()

		addCommand("suburu", { sender, args ->
			if (sender !is Player) return@addCommand false

			val loc = sender.location
			spawn(loc)
			true
		})
	}

	override fun Pig.vehicleDrivingTick() {

	}

	@EventHandler
	private fun onMove(ev: VehicleMoveEvent) {
		ev.vehicle.asVehicleBase()?.run {
			val forward = location.direction.normalize()
			val right = forward.crossProduct(Vector(0.0, 1.0, 0.0)).normalize()
			val up = right.crossProduct(forward).normalize()
			val baseAngle = EulerAngle(0.0, 0.0, 0.0)

			updateModels(this, forward, right, up, baseAngle)
		}
	}

	override fun createVehicleEntity(loc: Location) = (loc.world.spawnEntity(loc, EntityType.PIG) as Pig).apply {
		customName(Component.text("Suburu Forester"))
		isCustomNameVisible = true
	}
}
