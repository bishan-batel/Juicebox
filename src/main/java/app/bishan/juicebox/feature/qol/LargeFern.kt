package app.bishan.juicebox.feature.qol

import app.bishan.juicebox.feature.Feature
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockDropItemEvent

object LargeFern : Feature("large_fern", true) {

	@EventHandler
	fun onLargeFernBreak(ev: BlockDropItemEvent) {
		val player = ev.player

		if ((player.inventory.itemInMainHand.type == Material.SHEARS
					|| player.inventory.itemInMainHand.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0).not()
		) return

		if (ev.blockState.type == Material.LARGE_FERN) {
			ev.items.forEach { it.itemStack.type = Material.LARGE_FERN }
		}
	}
}
