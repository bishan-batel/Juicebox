package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Rabbit
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack

object ModernRabbitCatching : Feature("modern_rabbit_catching", true) {
	private const val RABBIT_ESCAPE_RATE = 0.25

	@EventHandler
	private fun onRabbitEat(ev: PlayerItemConsumeEvent) {
		if (ev.item.type != Material.RABBIT_STEW) return
		if (Math.random() > RABBIT_ESCAPE_RATE) return
		ev.player.world.spawnEntity(ev.player.location, EntityType.RABBIT, true)
		ev.player.foodLevel -= 10
	}

	@EventHandler
	private fun onRabbitStewed(ev: PlayerInteractAtEntityEvent) {
		if (ev.rightClicked !is Rabbit) return

		val inventory = ev.player.inventory
		val item = inventory.getItem(ev.hand)

		if (item.type != Material.BOWL) return

		val stew = ItemStack(Material.RABBIT_STEW)
		ev.rightClicked.remove()
		if (ev.player.gameMode == GameMode.CREATIVE) {
			inventory.addItem(stew)
		} else if (item.amount <= 1)
			inventory.setItem(ev.hand, stew)
		else {
			item.amount--
			inventory.addItem(stew)
		}
	}
}
