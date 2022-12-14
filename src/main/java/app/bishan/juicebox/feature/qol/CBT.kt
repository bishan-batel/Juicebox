package app.bishan.juicebox.feature.qol

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerMoveEvent

object CBT : Feature("cbt", true) {
	private const val COCK_DAMAGE = 1.5

	@EventHandler
	private fun onMove(ev: EntityMoveEvent) {
		if (ev.hasChangedBlock()) move(ev.to, ev.entity)
	}

	@EventHandler
	private fun onMove(ev: PlayerMoveEvent) = move(ev.to, ev.player)

	private fun move(loc: Location, ent: LivingEntity) {
		if (loc.block.type != Material.STONECUTTER) return
		ent.damage(1.5)

		var task = -1
		task = Bukkit.getScheduler().scheduleSyncRepeatingTask(
			JuiceboxPlugin.instance,
			fun() {
				if (!ent.isValid || ent.isDead || ent.location.block.type != Material.STONECUTTER) {
					Bukkit.getScheduler().cancelTask(task)
					return
				}
				ent.damage(COCK_DAMAGE)
			}, 0, 5
		)
	}
}
