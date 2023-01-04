package app.bishan.juicebox.feature.real.tools.instrument

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.feature.internal.ResourcePack
import app.bishan.juicebox.feature.internal.WanderingRecipe
import app.bishan.juicebox.feature.real.tools.instrument.Instrument.getEyeNote
import app.bishan.juicebox.utils.filterItemInHand
import app.bishan.juicebox.utils.hasFlag
import app.bishan.juicebox.utils.raiseFlag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

object Trumpet : Feature("trumpet", true) {
	private val IS_TRUMPET = JuiceboxPlugin.key("is_instrument_trumpet")

	private val TRUMPET = ItemStack(Material.GOLDEN_HORSE_ARMOR).apply {
		itemMeta = itemMeta.apply {
			persistentDataContainer.raiseFlag(IS_TRUMPET)
			displayName(Component.text("Trumpet", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
			lore(
				listOf(
					Component.text("Right click to play a note!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
				) + ResourcePack.NO_TEXTURE_MESSAGE
			)
			addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
		}
	}

	private val TRUMPET_TRADE =
		WanderingRecipe(TRUMPET, 1).withIngredients(ItemStack(Material.EMERALD, 20)).minTemp(0.9).chance(0.01 * 18)

	private val activeTrumpets = mutableSetOf<Player>()


	override fun onEnable() {
		addCustomItem("trumpet", TRUMPET)
		addWanderingTrade(TRUMPET_TRADE)
	}

	@EventHandler
	private fun onHold(ev: PlayerItemHeldEvent) {
		val player = ev.player


		if (activeTrumpets.contains(player)) return


		// wait one tick
		Bukkit.getScheduler().runTask(JuiceboxPlugin.instance, fun() {
			player.inventory.filterItemInHand { it.isTrumpet } ?: return
			activeTrumpets.add(player)

			var task = -1
			task = Bukkit.getScheduler().scheduleSyncRepeatingTask(
				JuiceboxPlugin.instance, fun() {
					if (!player.isValid || !(player.inventory.itemInMainHand.isTrumpet || player.inventory.itemInOffHand.isTrumpet)) {
						activeTrumpets.remove(player)
						Bukkit.getScheduler().cancelTask(task)
						return
					}

					player.sendActionBar(Instrument.getNoteTextComponent(player.getEyeNote().toInt()))
				}, 0L, 1L
			)
		})
	}

	@EventHandler
	private fun onInteract(ev: PlayerInteractEvent) {
		val player = ev.player

		if (!(player.inventory.itemInMainHand.isTrumpet || player.inventory.itemInOffHand.isTrumpet)) return
		if (player.hasCooldown(TRUMPET.type)) return

		player.setCooldown(TRUMPET.type, 1)

		val note = player.getEyeNote().toInt()

		player.world.playSound(
			player.location,
			Sound.BLOCK_NOTE_BLOCK_HARP,
			3f,
			Instrument.noteAsPitch(note.toFloat())
		)

		val color = Instrument.getNoteColor(note)!!
		val offset = Vector(Math.random(), Math.random(), Math.random()).multiply(2).subtract(Vector(1.0, 1.0, 1.0))

		player.world.spawnParticle(
			Particle.NOTE,
			player.location.add(0.0, 1.75, 0.0).add(offset),
			0,
			color.red() / 255.0,
			color.green() / 255.0,
			color.blue() / 255.0,
		)
	}

	private val ItemStack.isTrumpet get() = itemMeta?.persistentDataContainer?.hasFlag(IS_TRUMPET) == true
}
