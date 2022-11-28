package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.ItemStack

object MooMilk : Feature("moo_milk", true) {
	@EventHandler
	fun onMilk(ev: PlayerInteractAtEntityEvent) {
		if (!isEntityUUID(ev.rightClicked, PlayersUUID.MADI)) return
		val player = ev.player

		val itemInHand = player.inventory.getItem(ev.hand)
		if (itemInHand.type != Material.BUCKET) return

		if (player.gameMode == GameMode.CREATIVE || itemInHand.amount > 1) {
			player.inventory.addItem(ItemStack(Material.MILK_BUCKET))
		} else {
			player.inventory.setItem(ev.hand, ItemStack(Material.MILK_BUCKET))
		}

		ev.rightClicked.world.playSound(ev.rightClicked.location, "entity.cow.milk", 1f, 1f)
		ev.rightClicked.world.playSound(ev.rightClicked.location, "entity.cow.ambient", 1f, 1f)
	}
}
