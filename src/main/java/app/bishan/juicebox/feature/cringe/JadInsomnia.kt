package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.Scheduler
import app.bishan.juicebox.utils.isEntityUUID
import io.papermc.paper.event.player.PlayerDeepSleepEvent
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler

object JadInsomnia : Feature("jad_insomnia", true) {
	@EventHandler
	private fun onSleep(ev: PlayerDeepSleepEvent) {
		if (Math.random() > 0.5) return
		if (!isEntityUUID(ev.player, PlayersUUID.JAD)) return
		val jad = ev.player

		Scheduler.defer(30) {
			jad.kick(
				Component.text("You may not rest now. There are hot singles in your area.")
			)
		}
	}
}
