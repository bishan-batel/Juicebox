package app.bishan.juicebox.feature.lore

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.hasFlag
import app.bishan.juicebox.utils.raiseFlag
import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Marker
import org.bukkit.entity.Player

object Anomaly : Feature("anomaly", false) {
	private val ANOMALY_MARKER = JuiceboxPlugin.key("is_anomaly_marker")
	private var fieldTask = -1

	override fun onEnable() {
		fieldTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(JuiceboxPlugin.instance, ::fieldTick, 0L, 2L)
		addCommand("testing:anomaly", { sender, args ->
			if (sender !is Player) return@addCommand false
			val marker = sender.world.spawn(sender.location, Marker::class.java)
			marker.persistentDataContainer.raiseFlag(ANOMALY_MARKER)
			true
		})
	}

	override fun onDisable() {
		Bukkit.getScheduler().cancelTask(fieldTask)
	}

	private fun fieldTick() {
		Bukkit.getWorlds().forEach { world ->
			world.getEntitiesByClass(Marker::class.java).filter { it.persistentDataContainer.hasFlag(ANOMALY_MARKER) }
				.forEach(::anomalyTick)
		}
	}

	private fun anomalyTick(marker: Marker) {
		val center = marker.location

		val players =
			marker.world
				.getNearbyEntitiesByType(Player::class.java, center, 113.0)
				.filter { marker.isInAnomaly(it) }

		players.forEach {
			it.isFlying = false
			it.isGliding = false
		}
	}

	private fun Marker.isInAnomaly(person: HumanEntity) = location.distance(person.location) <= 113
}
