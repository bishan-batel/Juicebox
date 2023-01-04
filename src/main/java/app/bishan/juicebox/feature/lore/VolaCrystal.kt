package app.bishan.juicebox.feature.lore

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.hasFlag
import app.bishan.juicebox.utils.raiseFlag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.ItemStack

object VolaCrystal : Feature("vola_crystal", true) {
	private val KEY_TAMER_CRYSTAL get() = JuiceboxPlugin.key("vola_crystal")
	private val TAMER_CRYSTAL = ItemStack(Material.AMETHYST_SHARD).apply {
		itemMeta = itemMeta?.apply {
			displayName(
				Component.text("Vola Crystal").decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
					.color(TextColor.color(0, 255, 100))
			)
			addUnsafeEnchantment(Enchantment.OXYGEN, 1)
			persistentDataContainer.raiseFlag(KEY_TAMER_CRYSTAL)
		}
	}

	override fun onEnable() {
		addCustomItem("crystal", TAMER_CRYSTAL)
	}

	// when a player clicks on an entity, then the player will start riding the entity
	@EventHandler
	fun onRightClick(ev: PlayerInteractAtEntityEvent) {
		val player = ev.player
		val item = player.inventory.itemInMainHand
		if (item.itemMeta?.persistentDataContainer?.hasFlag(KEY_TAMER_CRYSTAL) != true) return

		var bottomOfCurrentStack = player as Entity
		while (bottomOfCurrentStack.isInsideVehicle) bottomOfCurrentStack = bottomOfCurrentStack.vehicle!!

		var topOfNewStack = ev.rightClicked
		while (topOfNewStack.passengers.isNotEmpty()) topOfNewStack = topOfNewStack.passengers.first()

		// amy sfx
		player.world.playSound(player.location, "block.amethyst_block.hit", 1f, 1f)
		topOfNewStack.addPassenger(bottomOfCurrentStack)
	}
}
