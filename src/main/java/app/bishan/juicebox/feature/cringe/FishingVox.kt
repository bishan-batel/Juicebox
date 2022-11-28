package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerFishEvent

object FishingVox : Feature("fishing_vox", true) {
	private const val FISHING_CHANCE = 0.01

	@EventHandler
	fun onFish(ev: PlayerFishEvent) {
		if (ev.state != PlayerFishEvent.State.CAUGHT_FISH) return
		if (Math.random() > FISHING_CHANCE) return

		// grab entity vox
		val vox = (Bukkit.getEntity(PlayersUUID.VOX) ?: return) as Player
		if (vox.gameMode != GameMode.SURVIVAL) return

		val caught = ev.caught
		if (caught != null) {
			if (caught is Item) {
				caught.pickupDelay = 20
			}
			caught.addPassenger(vox)
		}
//		ev.hook.hookedEntity = vox
	}
}
