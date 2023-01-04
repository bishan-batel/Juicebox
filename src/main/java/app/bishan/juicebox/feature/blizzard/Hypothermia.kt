package app.bishan.juicebox.feature.blizzard

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.Scheduler
import app.bishan.juicebox.utils.getDouble
import app.bishan.juicebox.utils.setDouble
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitTask
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.math.truncate

object Hypothermia : Feature("hypothermia", true) {
	override val description
		get() = Component.text {
			it.append(Component.text("Players will start to freeze when the temperature low enough while it is snowing,"))
			it.append(Component.newline())
			it.append(Component.text("which can be hindered by light, leather armor, and a roof over your head."))
		}
	private val LEATHER_ARMOR = arrayOf(
		Material.LEATHER_HELMET,
		Material.LEATHER_CHESTPLATE,
		Material.LEATHER_LEGGINGS,
		Material.LEATHER_BOOTS,
		Material.TURTLE_HELMET
	)
	private val THERMIA_PRECISE = JuiceboxPlugin.key("thermia_precise")
	private var freezingSpeed = 0.0
	private var maxColdTemperature = 0.0
	private var maxColdLightLevel = 0
	private var task: BukkitTask? = null

	override fun onEnable() {
		config().let {
			freezingSpeed = 1 / (it["freezingSpeed"] as? Double? ?: (2000.0))
			maxColdLightLevel = it["maxColdLightLevel"] as? Int? ?: 4
			maxColdTemperature = it["maxColdTemperature"] as? Double? ?: 0.1
		}

		task = Scheduler.onceEvery(1) {
			Bukkit.getOnlinePlayers().forEach(::applyHypothermia)
		}
	}

	override fun onDisable() {
		task?.cancel()
	}

	private fun applyHypothermia(player: Player) {
		if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return

		val world = player.world

		val block = player.location.block
		val temp = block.temperature

		val leatherArmorCount = player.equipment.armorContents.count {
			LEATHER_ARMOR.contains(it?.type)
		}
		if (leatherArmorCount >= 4) return

		val highestBlock = world.getHighestBlockAt(block.location)


		// if player does not have direct sunlight, subtract light level by 3
		val diff = block.y - highestBlock.y
		val light = (if (diff < 0) {
			block.lightLevel.toInt() + 1
		} else {
			block.lightFromBlocks.toInt()
		}) - maxColdLightLevel + leatherArmorCount

		if (world.hasStorm().not() || temp > maxColdTemperature || light > 0) {
			player.persistentDataContainer.setDouble(THERMIA_PRECISE, player.freezeTicks.toDouble())
			return
		}

		// deep cover for cave edge cases
		run {
			if (block.y > 64) return@run
			var seek = block.getRelative(BlockFace.UP)
			var depth = 0
			while (seek.y < highestBlock.y) {
				if (seek.type.isSolid) {
					depth++
					if (depth >= 10) return
				}
				seek = seek.getRelative(BlockFace.UP)
			}
		}

		val targetTicks = (player.maxFreezeTicks * exp(-light * 0.25))

		val delta = (targetTicks - player.freezeTicks) * freezingSpeed / sqrt(1.0 + leatherArmorCount)

		// add delta
		player.persistentDataContainer.setDouble(
			THERMIA_PRECISE, (player.persistentDataContainer.getDouble(THERMIA_PRECISE) ?: 0.0) + delta
		)

		player.freezeTicks = player.persistentDataContainer.getDouble(THERMIA_PRECISE)?.toInt() ?: 0
//		Bukkit.broadcast(Component.text("${player.freezeTicks}"))
	}

}
