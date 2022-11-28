package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.ItemStack

object LegacyVolaHelm : Feature("legacy-vola-helm", true) {
	private val VOLA_HELM = run {
		val item = ItemStack(Material.TURTLE_HELMET)
		val im = item.itemMeta
		im.displayName(
			Component
				.text("Vola Helm")
				.color(TextColor.color(100, 110, 255))
				.decoration(TextDecoration.ITALIC, false)
		)
		im.addEnchant(Enchantment.MENDING, 1, false)
		im.isUnbreakable = true
		item.itemMeta = im
		item
	}

	@EventHandler
	private fun onInteractEntity(ev: PlayerInteractAtEntityEvent) {
		val helmet = ev.player.inventory.helmet ?: return
		if (!helmet.isSimilar(VOLA_HELM)) return
		if (ev.player.isSneaking) return
		var vehicle = ev.player.vehicle ?: ev.player

		while (vehicle.isInsideVehicle)
			vehicle = vehicle.vehicle!!

		var clicked = ev.rightClicked
		while (clicked.passengers.size > 0)
			clicked = clicked.passengers[0]

		clicked.addPassenger(vehicle)
	}
}
