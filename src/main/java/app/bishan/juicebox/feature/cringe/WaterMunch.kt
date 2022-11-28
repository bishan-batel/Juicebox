package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.ItemStack

object WaterMunch : Feature("munch_rain", true) {
	@EventHandler
	fun onBucketInteract(ev: PlayerInteractAtEntityEvent) {
		if (!isEntityUUID(ev.rightClicked, PlayersUUID.WATER_MUNCH)) return
		val itemInHand = ev.player.inventory.getItem(ev.hand) ?: return

		when (itemInHand.type) {
			Material.WATER_BUCKET -> {
				ev.player.world.setStorm(true)
				ev.player.inventory.setItem(ev.hand, ItemStack(Material.BUCKET))
			}

			Material.LAVA_BUCKET -> {
				ev.player.world.setStorm(true)
				ev.player.world.isThundering = true
				ev.player.inventory.setItem(ev.hand, ItemStack(Material.BUCKET))
			}

			Material.BUCKET -> {
				if (ev.player.world.hasStorm()) {
					val replacement = if (ev.player.world.isThundering) Material.LAVA_BUCKET else Material.WATER_BUCKET
					itemInHand.amount--
					if (itemInHand.amount <= 0) {
						ev.player.inventory.setItem(ev.hand, ItemStack(replacement))
					} else {
						ev.player.inventory.addItem(ItemStack(replacement))
					}

					ev.player.world.setStorm(false)
					ev.player.world.isThundering = false
				}
			}

			else -> {
				return
			}
		}
		ev.isCancelled = true
	}
}
