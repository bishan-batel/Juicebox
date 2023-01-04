package app.bishan.juicebox.feature.real.food

import app.bishan.juicebox.feature.Feature
import io.papermc.paper.event.block.PlayerShearBlockEvent
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import kotlin.math.roundToInt

object FortuneShears : Feature("fortune_shears", true) {

	@EventHandler
	fun onShear(ev: PlayerShearBlockEvent) {
		val fortuneLvl = ev.item.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)
		if (fortuneLvl == 0) return

		ev.drops.forEach { item ->
			val boost = (Math.random() * fortuneLvl).roundToInt()
			item.amount *= boost
		}
	}
}
