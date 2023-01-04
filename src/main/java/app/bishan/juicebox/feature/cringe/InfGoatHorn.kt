package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.feature.internal.WanderingRecipe
import app.bishan.juicebox.utils.addEnchantmentGlint
import app.bishan.juicebox.utils.missingFlag
import app.bishan.juicebox.utils.raiseFlag
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object InfGoatHorn : Feature("inf_goat_horn", true) {
	private val IS_INF_HORN = JuiceboxPlugin.key("is_inf_goat_horn")

	private val INF_GOAT_HORN = ItemStack(Material.GOAT_HORN).apply {
		itemMeta = itemMeta.apply {
			persistentDataContainer.raiseFlag(IS_INF_HORN)
		}
		addEnchantmentGlint()
	}

	private val INF_GOAT_TRADE =
		WanderingRecipe(INF_GOAT_HORN, 1).withIngredients(ItemStack(Material.EMERALD, 20)).maxTemp(0.21).chance(0.01)

	override fun onEnable() {
		addCustomItem("inf_goat_horn", INF_GOAT_HORN)
		addWanderingTrade(INF_GOAT_TRADE)
	}

	@EventHandler
	private fun onInfHorn(ev: PlayerInteractEvent) {
		if (ev.action.isRightClick.not()) return

		val item = ev.item ?: return
		val player = ev.player

		if (item.type != Material.GOAT_HORN) return
		if (player.hasCooldown(Material.GOAT_HORN)) return
		if (item.itemMeta.persistentDataContainer.missingFlag(IS_INF_HORN)) return

		player.setCooldown(Material.GOAT_HORN, 20 * 30)

		player.world.players.forEach {
			it.playSound(it.location, Sound.ITEM_GOAT_HORN_SOUND_0, 4f, 1f)
		}
		ev.isCancelled = true
	}
}
