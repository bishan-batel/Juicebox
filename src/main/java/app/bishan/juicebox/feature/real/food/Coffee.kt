package app.bishan.juicebox.feature.real.food

import app.bishan.juicebox.feature.Feature
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.VillagerAcquireTradeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Recipe

object Coffee : Feature("coffee", false) {
	override fun onEnable() {

	}

	@EventHandler
	private fun onPotionPlace(ev: InventoryClickEvent) {
		val brewingInventory = ev.clickedInventory ?: return
		if (brewingInventory.type != InventoryType.BREWING) return

		Bukkit.broadcast(Component.text("bruh: ${ev.currentItem}"))
	}
}
