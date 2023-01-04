package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import kotlin.math.truncate

object CustomHorns : Feature("custom_horns", true) {
	private val IS_CUSTOM_HORN = JuiceboxPlugin.key("is_custom_horn")
	private val CUSTOM_SOUND = JuiceboxPlugin.key("custom_horn_sfx")
	private val CUSTOM_SOUND_VOLUME = JuiceboxPlugin.key("custom_horn_vol")
	private val CUSTOM_SOUND_PITCH = JuiceboxPlugin.key("custom_horn_pitch")
	private val monitoringAnvils = mutableMapOf<AnvilInventory, BukkitTask>()

	override fun onEnable() {
		addCommand("hidden:custom_horn", this::applySound, null)

		monitoringAnvils.forEach { it.value.cancel() }
		monitoringAnvils.clear()
	}

	override fun onDisable() {
	}

	private fun applySound(sender: CommandSender, args: Array<out String>): Boolean {
		if (sender !is Player) {
			sender.sendMessage(Component.text("You must be a player to use this command!", NamedTextColor.RED))
			return true
		}

		// get item in hand and check if it's a horn
		val item = sender.inventory.itemInMainHand
		if (item.type != Material.GOAT_HORN) {
			sender.sendMessage(Component.text("You must be holding a goat horn to use this command!", NamedTextColor.RED))
			return true
		}

		// get the sound specified in args
		val sound = args.getOrNull(0)

		val volume = args.getOrNull(1)?.toFloatOrNull()
		val pitch = args.getOrNull(2)?.toFloatOrNull()

		// set the custom sound
		applySfx(item, sound, volume, pitch)

		return true
	}

	private fun applySfx(item: ItemStack, sfx: String?, vol: Float?, pitch: Float?) {
		item.itemMeta = item.itemMeta?.apply {
			persistentDataContainer.setString(CUSTOM_SOUND, sfx ?: "null")
			persistentDataContainer.raiseFlag(IS_CUSTOM_HORN)
			persistentDataContainer.setFloat(CUSTOM_SOUND_VOLUME, vol ?: 10f)
			persistentDataContainer.setFloat(CUSTOM_SOUND_PITCH, pitch ?: 1f)

			lore(
				if (sfx == null || sfx == "null")
					listOf(
						Component.textOfChildren(
							Component.text("No bound custom sound", NamedTextColor.RED),
						)
					) else
					listOf(
						Component.textOfChildren(
							Component.text("Custom sound: ", NamedTextColor.GRAY),
							Component.text(sfx, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
						),
						Component.text("Volume: ", NamedTextColor.GRAY).append(
							Component.text((truncate((vol ?: 1f) * 100) / 100).toString(), NamedTextColor.RED)
								.decoration(TextDecoration.ITALIC, false)
						),
						Component.text("Pitch: ", NamedTextColor.GRAY).append(
							Component.text((truncate((pitch ?: 1f) * 100) / 100).toString(), NamedTextColor.LIGHT_PURPLE)
								.decoration(TextDecoration.ITALIC, false)
						)
					)
			)
			addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

			displayName(
				Component.text("Vola Horn", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
			)
		}
		item.addEnchantmentGlint()
	}

	/**
	 * When renaming the goat horn in an anvil, apply the custom sound
	 */
	@EventHandler
	private fun onAnvil(ev: InventoryClickEvent) {
		val inv = ev.inventory as? AnvilInventory ?: return

		if (monitoringAnvils.contains(inv)) return

		updateAnvil(inv)
		monitoringAnvils[inv] = Scheduler.onceEvery(1) {
			if (inv.viewers.isEmpty()) {
				monitoringAnvils[inv]?.cancel()
				monitoringAnvils.remove(inv)
				return@onceEvery
			}
			updateAnvil(inv)
		}
	}

	private fun updateAnvil(inv: AnvilInventory) {
		if (inv.secondItem != null) return
		val first = inv.firstItem ?: return
		if (first.itemMeta?.persistentDataContainer?.hasFlag(IS_CUSTOM_HORN) != true) return

		val txtSplit = (inv.renameText ?: "").split(" ")

		val sound = txtSplit.getOrNull(0)
		val volume = txtSplit.getOrNull(1)?.toFloatOrNull()
		val pitch = txtSplit.getOrNull(2)?.toFloatOrNull()

//			applySfx(first, sound, volume, pitch)
		applySfx(inv.result ?: first, sound, volume, pitch)
	}

	@EventHandler
	fun onHornCall(ev: PlayerInteractEvent) {
		if (ev.action.isRightClick.not()) return

		val item = ev.item ?: return

		if (ev.player.hasCooldown(item.type)) return
		if (item.itemMeta?.persistentDataContainer?.hasFlag(IS_CUSTOM_HORN) != true) return
		val customSound = item.itemMeta?.persistentDataContainer?.getString(CUSTOM_SOUND) ?: return

		val volume = item.itemMeta?.persistentDataContainer?.getFloat(CUSTOM_SOUND_VOLUME) ?: 10f
		val pitch = item.itemMeta?.persistentDataContainer?.getFloat(CUSTOM_SOUND_PITCH) ?: 1f

		item.addEnchantmentGlint()

		if (customSound != "null") {
			ev.player.world.playSound(ev.player.location, customSound, volume, pitch)
			ev.player.setCooldown(Material.GOAT_HORN, 20)
		}
		ev.isCancelled = true
	}
}
