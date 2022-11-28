package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.ItemStack

object BeeHoneyFactory : Feature("beehoneyfactory", true) {
	private const val HUNGER_DECREASE = 7
	private const val HONEY_COOL_DOWN_MILLIS = 1000 * 60
	private var lastTimeHoneyed = 0L

	@EventHandler
	fun takeHoney(ev: PlayerInteractAtEntityEvent) {
		if (!isEntityUUID(ev.rightClicked, PlayersUUID.BEE)) return
		val bee = ev.rightClicked as Player

		val item = ev.player.inventory.getItem(ev.hand)

		// guards for glass bottle in hand
		if (item.type != Material.GLASS_BOTTLE) return

		// skip if cool down has passed
		if (this.lastTimeHoneyed + HONEY_COOL_DOWN_MILLIS < System.currentTimeMillis()) {
			this.lastTimeHoneyed = System.currentTimeMillis()

			val honey = ItemStack(Material.HONEY_BOTTLE)

			bee.foodLevel -= HUNGER_DECREASE
			if (ev.player.gameMode == GameMode.CREATIVE) {
				ev.player.inventory.addItem(honey)
				return
			}

			if (item.amount <= 1) {
				// replace in hand if just one
				ev.player.inventory.setItem(ev.hand, honey)
			} else {
				// if in stack then decrement and just push it to somewhere on their inventory
				item.amount--
				ev.player.inventory.addItem(honey)
			}

		}
	}
}
