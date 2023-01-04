package app.bishan.juicebox.feature.blizzard

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.Scheduler
import net.kyori.adventure.text.Component
import net.minecraft.util.Mth.clamp
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Snow
import org.bukkit.command.BlockCommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.noise.PerlinNoiseGenerator
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

object DynamicSnow : Feature("dynamic_snow", true) {
	override val description get() = Component.text("Dynamic Snow")

	private val snowTasks = mutableMapOf<World, BukkitTask>()
	private val SNOW_BLOCK_DATA = Material.SNOW.createBlockData() as Snow

	private const val MAX_NOISE_COEFF = 0.9
	private const val CHECK_RADIUS = 2
	private const val NOISE_SCALE = 0.15
	private const val LIGHT_FALLOFF = 1.5
	private const val LIGHT_MIN = 0.0
	private const val TEMPATURE_FALLOFF = 1.2


	override fun onEnable() {
		addCommand("testing:map", { sender, _ ->
//			if (sender !is Player) return@addCommand true

			// get the locatio & world if sender is a entity or a block entity
			val loc =
				(sender as? Player)?.location ?: (sender as? BlockCommandSender)?.block?.location ?: return@addCommand true
			val world = loc.world


			for (x in -15..15) {
				for (z in -15..15) {
					snow(world, loc.x + x.toDouble(), loc.z + z.toDouble())
				}
			}
			true
		})

		Bukkit.getWorlds().forEach {
			if (!it.isClearWeather) snowTasks[it] = createSnowTask(it)
		}
	}

	override fun onDisable() {
		snowTasks.forEach { (_, task) ->
			task.cancel()
		}
	}

	@EventHandler
	fun onWeatherChange(ev: WeatherChangeEvent) {
		if (ev.toWeatherState()) {
			// wait 1 second
			Scheduler.deferAsync(20) {
				if (!snowTasks.containsKey(ev.world)) snowTasks[ev.world] = createSnowTask(ev.world)
			}

		} else {
			snowTasks.remove(ev.world)?.cancel()
		}
	}

	private fun createSnowTask(world: World) = Scheduler.onceEvery(1L) {
		val chance = if (world.isThundering) 0.22
		else 0.1

		for (chunk in world.loadedChunks) {
			if (Math.random() > chance) continue

			val x = chunk.x * 16 + (0..15).random().toDouble()
			val z = chunk.z * 16 + (0..15).random().toDouble()
			snow(world, x, z)
		}
	}

	private fun snow(world: World, x: Double, z: Double) {
		var block = world.getHighestBlockAt(x.toInt(), z.toInt()).getRelative(BlockFace.UP)

		if (block.type == Material.SNOW) {
			while (block.type == Material.SNOW) {
				val snow = block.blockData as Snow
				if (snow.layers < 8) break
				block = block.getRelative(BlockFace.UP)
			}
		}

		val y = block.y.toDouble()

		if (!applySnowAt(Location(world, x, y, z))) {
			// try again for the 8 blocks around it
			for (x2 in -1..1) {
				for (z2 in -1..1) {
					if (x2 == 0 && z2 == 0) continue

					// apply snow below the block

					if (applySnowAt(Location(world, (x + x2), y, (z + z2)))) break
				}
			}
		}

	}

	private fun applySnowAt(loc: Location): Boolean {
		val block = loc.block
		if (block.temperature > 0.1) return true

		when (block.type) {
			Material.AIR, Material.SNOW -> {
				val data = Bukkit.createBlockData(Material.SNOW)

				if (data.isSupported(loc)) {
					val currentLayers = (block.blockData as? Snow)?.layers ?: 0
					val targetLayer = locNoise(loc)

					if (currentLayers == targetLayer) return false

					data.apply {
						this as Snow
						layers = currentLayers + (targetLayer - currentLayers).coerceAtMost((1..3).random())
					}


					// attempt to call event
					val formEvent = BlockFormEvent(block, block.state.apply {
						blockData = data
					})

					try {
						Bukkit.getPluginManager().callEvent(formEvent)
					} catch (ex: Exception) {
						ex.printStackTrace()
					}

					if (formEvent.isCancelled) return false
					loc.world.setBlockData(loc, formEvent.newState.blockData)
					return true
				}
			}

			else -> return false
		}
		return false
	}

	private fun snowDepth(loc: Location): Int {
		var snow = loc.world.getHighestBlockAt(loc.blockX, loc.blockZ)
		var depth = 0

		while (snow.type == Material.SNOW) {
			snow = snow.getRelative(BlockFace.DOWN)
			depth += (snow.blockData as Snow).layers
		}
		return depth
	}

	private fun locNoise(loc: Location): Int {

		val world = loc.world
		val x = loc.blockX
		val y = loc.blockY
		val z = loc.blockZ


		val noise = MAX_NOISE_COEFF * PerlinNoiseGenerator.getNoise(
			x * NOISE_SCALE, y * NOISE_SCALE, z * NOISE_SCALE, 3, 0.6, 0.7
		).absoluteValue

		val neighborVal = run {
			var n = 0.0

			for (dx in -CHECK_RADIUS..CHECK_RADIUS) {
				for (dz in -CHECK_RADIUS..CHECK_RADIUS) {
//					n += world.snowFunction(x + dx, y, z + dz) * fastInvSqrt(2.0 * (dx * dx + dz * dz).toDouble())
					n += world.snowFunction(x + dx, y, z + dz) * exp(-((dx * dx + dz * dz).toDouble() / 3.5))
				}
			}

//			for (dx in -1..1) {
//				for (dz in -1..1) {
//					n += world.snowFunction(x + dx, y + 1, z + dz) * exp(-((dx * dx + dz * dz).toDouble() / 2.0))
//				}
//			}
			n
		}

		val light = run {
			val lightPercentage =
				(loc.block.lightFromBlocks.coerceAtLeast(LIGHT_MIN.toInt().toByte()) - LIGHT_MIN) / (15.0 - LIGHT_MIN)

			exp(LIGHT_FALLOFF * -lightPercentage) * (1 - lightPercentage)
		}

		val n = neighborVal + noise

		var maxLevel = (7.0 - sqrt((loc.block.temperature * TEMPATURE_FALLOFF).coerceAtLeast(0.0)))

//		if (snowDepth() > 2.0) maxLevel -= 2.0
		maxLevel = maxLevel.coerceAtMost(7.0)

		return (1 + (light * clamp(7.0 * n, 0.0, maxLevel)).roundToInt())
	}

	private fun World.snowFunction(x: Int, y: Int, z: Int): Double {
		val block = getBlockAt(x, y, z)
		if (block.temperature > 0.2) return -block.temperature

		when (block.type) {
			Material.AIR, Material.SNOW -> return 0.0
			else -> {}
		}

		return if (Tag.LEAVES.isTagged(block.getRelative(BlockFace.DOWN).type)) 0.1
		else if (SNOW_BLOCK_DATA.isSupported(block.getRelative(0, 1, 0))) 1.0
		else -0.5
	}
}
