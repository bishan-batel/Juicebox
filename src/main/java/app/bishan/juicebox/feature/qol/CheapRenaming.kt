package app.bishan.juicebox.feature.qol

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.AnvilInventory

object CheapRenaming : Feature("cheap_renaming", true) {
	private val monitoringAnvils = mutableMapOf<AnvilInventory, Int>()

	@EventHandler
	fun onRename(ev: InventoryClickEvent) {
		// check and get if anvil inventory
		val inv = ev.inventory
		if (inv !is AnvilInventory) return

		// check if anvil is being monitored
		if (monitoringAnvils.contains(inv)) return
		monitorForRepairCost(inv)
	}

	private fun updateRepairCost(inv: AnvilInventory) {
		// guards
		if (inv.firstItem == null) return
		if (inv.secondItem != null) return
		if (inv.renameText == null) return
		inv.repairCost = 1
		inv.repairCostAmount = 1
	}

	private fun monitorForRepairCost(inv: AnvilInventory) {
		updateRepairCost(inv)
		monitoringAnvils[inv] = Bukkit.getScheduler().scheduleSyncRepeatingTask(
			JuiceboxPlugin.instance, fun() {
				updateRepairCost(inv)
//				Bukkit.broadcast(Component.text("${monitoringAnvils[inv]}:monitoring anvil"))
				if (inv.viewers.isEmpty() || !CheapRenaming.isActive()) {
					Bukkit.getScheduler().cancelTask(monitoringAnvils[inv]!!)
//					Bukkit.broadcast(Component.text("${monitoringAnvils[inv]}:cancel task"))
					monitoringAnvils.remove(inv)
					return
				}
			}, 1L, 1L
		)
	}
}
