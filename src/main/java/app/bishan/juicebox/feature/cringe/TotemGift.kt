package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import org.bukkit.EntityEffect
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.ItemStack

object TotemGift : Feature("totem_gift", false) {
	@EventHandler
	fun onTotemGift(ev: PlayerInteractAtEntityEvent) {
		if (ev.rightClicked !is Player) return
		val holding = ev.player.inventory.getItem(ev.hand) ?: return
		if (holding.type != Material.TOTEM_OF_UNDYING) return
		val player = ev.rightClicked as Player

		holding.amount--
		player.inventory.addItem(ItemStack(Material.TOTEM_OF_UNDYING))
		player.playEffect(EntityEffect.TOTEM_RESURRECT)
	}
}
