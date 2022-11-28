package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack

object SammieShear :
	Feature("sammie_shear", true) {

	// namespace key tag for time of last sheared
	private val LAST_TIME_SHEARED get() = NamespacedKey(JuiceboxPlugin.instance, "LastTimeSheared")

	private const val SHEAR_COOLDOWN_MILLIS = 1000 * 60 // 1 minute

	private val WOOL_MATERIALS = arrayOf(
		Material.WHITE_WOOL,
		Material.ORANGE_WOOL,
		Material.MAGENTA_WOOL,
		Material.LIGHT_BLUE_WOOL,
		Material.YELLOW_WOOL,
		Material.LIME_WOOL,
		Material.PINK_WOOL,
		Material.GRAY_WOOL,
		Material.LIGHT_GRAY_WOOL,
		Material.CYAN_WOOL,
		Material.PURPLE_WOOL,
		Material.BLUE_WOOL,
		Material.BROWN_WOOL,
		Material.GREEN_WOOL,
		Material.RED_WOOL,
		Material.BLACK_WOOL
	)

	// when someone right clicks sammie, she will be sheared
	@EventHandler
	fun onRightClick(ev: PlayerInteractEntityEvent) {
		// guard for player holding shears
		if (ev.player.inventory.itemInMainHand.type != Material.SHEARS) return


		if (!isEntityUUID(ev.rightClicked, PlayersUUID.SAMMIE)) return
		val lastTimeSheared = ev.rightClicked.persistentDataContainer.getLong(LAST_TIME_SHEARED)

		// if last time sheared is null, or if it's been more than 1 minute since last sheared
		val now = System.currentTimeMillis()
		if (lastTimeSheared != null && now - lastTimeSheared <= SHEAR_COOLDOWN_MILLIS) return
		ev.rightClicked.persistentDataContainer.setLong(LAST_TIME_SHEARED, now)

		// spawn 1-3 random color wool block items
		val numWool = (1..3).random()
		for (i in 1..numWool) {
			ev.rightClicked.world.dropItemNaturally(ev.rightClicked.location, ItemStack(WOOL_MATERIALS.random()))
		}
	}
}
