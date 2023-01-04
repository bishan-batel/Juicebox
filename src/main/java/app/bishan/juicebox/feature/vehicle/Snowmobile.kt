package app.bishan.juicebox.feature.vehicle

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import io.papermc.paper.event.entity.EntityMoveEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.block.Dispenser
import org.bukkit.block.data.Directional
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import java.lang.Math.toRadians
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.truncate

object Snowmobile : Feature("snowmobile", true) {
	private val IS_SNOWMOBILE_ITEM = JuiceboxPlugin.key("snowmobile_item")
	private val SNOWMOBILE_ON_TEXTURE = JuiceboxPlugin.key("snowmobile_running")

	private val SNOWMOBILE_ITEM = ItemStack(Material.MINECART).apply {
		itemMeta = itemMeta.apply {
			persistentDataContainer.raiseFlag(IS_SNOWMOBILE_ITEM)
			displayName(Component.text("Snowmobile", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
		}
	}

	//	private const val MAX_SPEED = 0.35
	private const val NON_SNOW_SPEED_MODIFIER = 0.2

	private const val ACCELERATION = 0.1
	private const val LAYER_IMPULSE_COEFF = 0.2
	private const val MAX_Y_VEL = 0.5

	private val SNOWMOBILE_TRACK_MATERIALS = setOf(
		Material.SNOW_BLOCK,
		Material.SNOW,
		Material.PACKED_ICE,
		Material.BLUE_ICE,
		Material.ICE,
		Material.FROSTED_ICE,
		Material.POWDER_SNOW,
		Material.POWDER_SNOW_CAULDRON,
	)

	private const val GAS_DECAY = 0.02
	private const val GAS_DELTA_DECAY = 0.3
	private const val GAS_BOOST = 0.02
	private const val GAS_BREAK = 0.06
	private val GAS_DELTA_RANGE = -0.1..0.075

	private val FUEL_RANGE = 0.0..100.0
	private val FUELS = mapOf(
		Material.COAL to 1.0,
		Material.CHARCOAL to 1.0,
		Material.COAL_BLOCK to 9.0,
		Material.BLAZE_ROD to 1.0,
		Material.BLAZE_POWDER to 0.6,
		Material.STICK to 0.1
	)
	private const val FUEL_CONSUMPTION_RATE = 0.0085
	private const val FUEL_BASE_CONSUMPTION = 1E-3

	private val SNOWMOBILE_GAS = JuiceboxPlugin.key("snowmobile_gas")
	private val SNOWMOBILE_GAS_DELTA = JuiceboxPlugin.key("snowmobile_gas_delta")

	private val SNOWMOBILE_FUEL = JuiceboxPlugin.key("snowmobile_fuel")

	/**
	 * Is the entity a snowmobile?
	 */
	private val ENT_SNOWMOBILE = JuiceboxPlugin.key("snowmobile")

	/**
	 * Is the entity a model for a snowmobile?
	 */
	private val ENT_SNOWMOBILE_MODEL = JuiceboxPlugin.key("snowmobile_stand")

	/**
	 * Tag containing UUID of the owning snowmobile from a component of the snowmobile
	 */
	private val COMP_TO_SNOWMOBILE_REF = JuiceboxPlugin.key("snowmobileModelMobileRef")

	/**
	 * Tag containing UUID of the owning snowmobile to the model of the snowmobile
	 */
	private val SNOWMOBILE_TO_MODEL_REF = JuiceboxPlugin.key("snowmobileModelRef")

	/**
	 * Visual model offset
	 */
	private const val MODEL_Y_OFFSET = 0.98
	private val snowMobileTasks = mutableMapOf<String, BukkitTask>()

	override fun onEnable() {
		addCustomItem("snowmobile", SNOWMOBILE_ITEM)

		snowMobileTasks.forEach { (_, u) -> u.cancel() }
		snowMobileTasks.clear()
	}

	override fun onDisable() {
		snowMobileTasks.forEach { (_, task) ->
			task.cancel()
		}
		snowMobileTasks.clear()
	}

	private val XZ_VECTOR = Vector(1.0, 0.0, 1.0)

	private fun createSteerTask(snowMobile: ArmorStand) = Scheduler.onceEvery(1) {
		val model = snowMobile.getModel()

		if (snowMobile.isValid.not()) {
			model.remove() // kill
			return@onceEvery
		}

		val player = snowMobile.passengers.firstOrNull() as? Player?
		if (player == null) {
			snowMobileTasks.remove(snowMobile.uniqueId.toString())?.cancel()
			return@onceEvery
		}

		val currGas = snowMobile.persistentDataContainer.getDouble(SNOWMOBILE_GAS) ?: 0.0
		val fuel = snowMobile.persistentDataContainer.getDouble(SNOWMOBILE_FUEL) ?: 0.0
		val hasFuel = fuel > 0.0

		val modelLoc = model.location
		val currYaw = modelLoc.yaw
		val eye = player.eyeLocation

		val shortAngleDist = run {
			val da = (eye.yaw - currYaw) % 360
			2 * da % 360 - da
		}

		val turnSpeed = exp(snowMobile.velocity.multiply(XZ_VECTOR).length() * -2.4)
		val yaw = (currYaw + (shortAngleDist * turnSpeed).coerceIn(-15.0, 15.0)).toFloat()


		if (hasFuel) {
			model.setRotation(yaw, modelLoc.pitch)
			snowMobile.setRotation(yaw, modelLoc.pitch)

			var lowestBlock = snowMobile.location.block
			// get the lowest block that is not air
			var i = 0
			while (lowestBlock.type == Material.AIR && i < 8) {
				lowestBlock = lowestBlock.getRelative(BlockFace.DOWN)
				i++
			}

			var acc = ACCELERATION * currGas

			if (SNOWMOBILE_TRACK_MATERIALS.contains(lowestBlock.type).not())
				acc *= NON_SNOW_SPEED_MODIFIER

			snowMobile.velocity = snowMobile.velocity.add(model.location.direction.multiply(acc))
		} else {
//			snowMobile.persistentDataContainer.setDouble(SNOWMOBILE_GAS, 0.0)
//			snowMobile.persistentDataContainer.setDouble(SNOWMOBILE_GAS_DELTA, 0.0)
		}

		// update gas pedal
		val currGasDelta =
			(snowMobile.persistentDataContainer.getDouble(SNOWMOBILE_GAS_DELTA) ?: 0.0).coerceIn(GAS_DELTA_RANGE)

//		Bukkit.broadcast(Component.text("Gas: ${truncate(currGas * 100) / 100}\nGasDelta: ${truncate(currGasDelta * 100) / 100}"))

		val updatedGas = ((1 - GAS_DECAY) * currGas + currGasDelta).coerceIn(0.0..1.0)
		snowMobile.persistentDataContainer.setDouble(SNOWMOBILE_GAS, updatedGas)

		val updatedGasDelta = ((1 - GAS_DELTA_DECAY) * currGasDelta)
		snowMobile.persistentDataContainer.setDouble(SNOWMOBILE_GAS_DELTA, updatedGasDelta)


		// decrease fuel
		snowMobile.persistentDataContainer.setDouble(
			SNOWMOBILE_FUEL, (fuel - (FUEL_BASE_CONSUMPTION + FUEL_CONSUMPTION_RATE * updatedGas)).coerceIn(FUEL_RANGE)
		)

		val gasColorR = (updatedGas * 255).toInt()
		val gasColorG = ((1 - updatedGas) * 255).toInt()

		// break message as a line of  █ characters from the minimum gas delta (DELTA_GAS_RANGE) to 0
		// gas message as a line of █ characters, with the length of the line being the current gas level
		val gasMessage = Component.textOfChildren(
			Component.text("█".repeat((updatedGas * 12).roundToInt()), TextColor.color(gasColorR, gasColorG, 0)),
			Component.text("█".repeat((12 * (1 - updatedGas)).roundToInt())).color(NamedTextColor.DARK_GRAY)
		)


		player.sendActionBar(
			Component.textOfChildren(
				Component.text("RPM: ").color(NamedTextColor.RED),
				Component.text("|", NamedTextColor.GRAY),
				gasMessage,
				Component.text("|", NamedTextColor.GRAY),
				Component.text("Fuel: ").color(NamedTextColor.RED),
				// fuel number
				Component.text("${truncate((fuel / FUEL_RANGE.endInclusive) * 100 * 10) / 10}%").color(NamedTextColor.WHITE),
			)
		)

		// send the gas message to the player


		/**
		 * Collision Smoothing, to prevent the snowmobile from getting stuck on snow & slide off of it
		 */
		val vel = snowMobile.velocity
		if (hasFuel && vel.y < MAX_Y_VEL) {
			val right = snowMobile.velocity.normalize().crossProduct(Vector(0.0, 1.0, 0.0))
			val loc = snowMobile.location

			val collidesOnPath = listOf(
				vel.clone().add(right.multiply(0.25)), vel.clone().add(right.multiply(-0.25)), vel
			).distinct().any {
				snowMobile.collidesAt(loc.add(it))
			}

			if (collidesOnPath) {
				// velocity slightly tilted upwards
				val tiltedVel = vel.clone().add(Vector(0.0, 1.56, 0.0)).normalize()
				val freeUpwardsCollisionPath = listOf(
//					tiltedVel.clone().add(right.multiply(0.25)),
//					tiltedVel.clone().add(tiltedVel.multiply(-0.25)),
					tiltedVel
				).distinct().none {
					snowMobile.collidesAt(loc.add(it))
				}


				if (freeUpwardsCollisionPath) {
					vel.y += LAYER_IMPULSE_COEFF
				}
				snowMobile.velocity = vel
			}
		}


		/**
		 * Particles & SFX
		 */
		if (hasFuel.not()) return@onceEvery

		if (snowMobile.velocity.multiply(XZ_VECTOR).lengthSquared() > 0.2) {
			player.world.spawnParticle(
				Particle.SNOWFLAKE,
				model.location.add(0.0, 1.2, 0.0).subtract(model.location.direction.multiply(0.75)),
				4,
				0.13,
				0.13,
				0.13,
				0.0
			)
		}

		// sound pitch varied by gas pedal & noise
		val soundPitch = (0.1 + 1.02 * updatedGas) + Math.random() * 0.05
		val volume = 0.05f + 0.35f * updatedGas
		snowMobile.world.playSound(
			snowMobile,
			Sound.ITEM_SHIELD_BLOCK,
			SoundCategory.PLAYERS,
			volume.toFloat(),
			soundPitch.toFloat()
		)
	}

	@EventHandler
	private fun onBreak(ev: PlayerInteractEvent) {
		if (ev.isBlockInHand) return
		if (ev.hasItem()) return
		if (ev.action.isLeftClick.not()) return

		val snowMobile = ev.player.vehicle ?: return
		if (snowMobile.persistentDataContainer.missingFlag(ENT_SNOWMOBILE)) return

		val delta = snowMobile.persistentDataContainer.getDouble(SNOWMOBILE_GAS_DELTA) ?: 0.0
		snowMobile.persistentDataContainer.setDouble(SNOWMOBILE_GAS_DELTA, delta - GAS_BREAK)
		ev.isCancelled = true
	}

	@EventHandler
	private fun onGasBoost(ev: PlayerSwapHandItemsEvent) {
		if (ev.offHandItem?.type != Material.AIR) return
		val vehicle = ev.player.vehicle ?: return
//		Bukkit.broadcast(Component.text("Gas boost"))

		if (vehicle.persistentDataContainer.missingFlag(ENT_SNOWMOBILE)) return

		val delta = vehicle.persistentDataContainer.getDouble(SNOWMOBILE_GAS_DELTA) ?: 0.0
		vehicle.persistentDataContainer.setDouble(SNOWMOBILE_GAS_DELTA, delta + GAS_BOOST)
		ev.isCancelled = true
	}

	@EventHandler
	private fun onPlace(ev: PlayerInteractEvent) {
		if (ev.action.isRightClick.not()) return

		val player = ev.player
		val item = player.inventory.getItem(ev.hand ?: return)

		if (item.isSnowMobileItem.not()) return

		// create snowmobile vehicle

		val loc = ev.clickedBlock?.let {
			if (it.isSolid.not()) it
			else it.getRelative(ev.blockFace)
		}?.location ?: return
		if (loc.block.type.isSolid) return

		loc.yaw = player.location.yaw

		val ent = (player.world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand).apply {
			isVisible = false
			isSmall = true
			customName(item.displayName())
			persistentDataContainer.raiseFlag(ENT_SNOWMOBILE)
			addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.ADDING_OR_CHANGING)
			addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING_OR_CHANGING)
			addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.ADDING_OR_CHANGING)
			addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.ADDING_OR_CHANGING)

			persistentDataContainer.setDouble(
				SNOWMOBILE_FUEL, item.itemMeta.persistentDataContainer.getDouble(SNOWMOBILE_FUEL) ?: 0.0
			)
		}

		createSnowMobileModel(ent)

		if (player.gameMode != GameMode.CREATIVE) {
			item.subtract()
		}
	}

	private fun createSnowMobileModel(ent: ArmorStand): ArmorStand = (ent.world.spawnEntity(
		ent.location.subtract(0.0, MODEL_Y_OFFSET, 0.0), EntityType.ARMOR_STAND
	) as ArmorStand).apply {
		isVisible = false
		setGravity(false)
		equipment.helmet = SNOWMOBILE_ITEM.clone().apply {
			persistentDataContainer.raiseFlag(SNOWMOBILE_ON_TEXTURE)
		}
		persistentDataContainer.raiseFlag(ENT_SNOWMOBILE_MODEL)
		persistentDataContainer.setUUID(COMP_TO_SNOWMOBILE_REF, ent.uniqueId)

		addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING)
		addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING_OR_CHANGING)
		addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.ADDING_OR_CHANGING)
		addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.ADDING_OR_CHANGING)

		ent.persistentDataContainer.setUUID(SNOWMOBILE_TO_MODEL_REF, uniqueId)
	}

	@EventHandler
	fun rideSnowmobile(ev: PlayerInteractAtEntityEvent) {
		val player = ev.player
		val ent = ev.rightClicked as? ArmorStand ?: return

		if (ent.persistentDataContainer.missingFlag(ENT_SNOWMOBILE_MODEL)) return

		val mobile = ent.getSnowMobile()

		if (mobile == null) {
			ent.remove()
			return
		}

		val handItem = player.inventory.itemInMainHand

		// if hand item is a pickaxe, break the snowmobile
		if (player.isSneaking) {
			when (handItem.type) {
				Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE -> {

					player.world.dropItemNaturally(mobile.location, ent.equipment.helmet.apply {
						val fuel = 0.6 * (mobile.persistentDataContainer.getDouble(SNOWMOBILE_FUEL) ?: 0.0)
						itemMeta = itemMeta.apply {
							val fuelMsg = Component.textOfChildren(
								Component.text("Fuel: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
								Component.text("${(fuel / FUEL_RANGE.endInclusive * 100).roundToInt()}%", NamedTextColor.GREEN)
									.decoration(TextDecoration.ITALIC, false)
							)
							lore(listOf(fuelMsg))
							persistentDataContainer.setDouble(SNOWMOBILE_FUEL, fuel)
							persistentDataContainer.lowerFlag(SNOWMOBILE_ON_TEXTURE)
						}
					})
					mobile.remove()

					Scheduler.defer {
						ent.remove()
					}
					return
				}

				else -> {}
			}
		}

		if (fuelUp(mobile, handItem)) {
			return
		}

		if (mobile.passengers.isEmpty()) {
			mobile.addPassenger(player)


			Scheduler.defer {
				snowMobileTasks[mobile.uniqueId.toString()] = createSteerTask(mobile)
			}
		}
	}

	@EventHandler
	private fun onDropperFuel(ev: BlockDispenseEvent) {
		// if block is a dispenser, try to fuel up the snowmobile with the item
		val block = ev.block
		if (block.type != Material.DISPENSER) return


		// get block in front of dispenser
		val dir = block.blockData as Directional
		val frontBlock = block.getRelative(dir.facing)

		val mobile = frontBlock.location.getNearbyEntities(1.0, 1.0, 1.0).firstOrNull {
			it is ArmorStand && it.persistentDataContainer.missingFlag(ENT_SNOWMOBILE_MODEL)
		} as? ArmorStand ?: return

		val dispenser = block.state as Dispenser

		@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
		dispenser.inventory.storageContents!!.filterNotNull().random().let {
			if (fuelUp(mobile, it)) {
				ev.isCancelled = true
			}
		}
	}

	private fun fuelUp(mobile: ArmorStand, fuel: ItemStack): Boolean {
		val amt = FUELS[fuel.type] ?: return false
		val currFuel = mobile.persistentDataContainer.getDouble(SNOWMOBILE_FUEL) ?: 0.0


		val newFuel = currFuel + amt

		if (newFuel !in FUEL_RANGE) {
			// play noteblock sound
			mobile.world.playSound(mobile.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f)
			return true
		}

		mobile.persistentDataContainer.setDouble(SNOWMOBILE_FUEL, newFuel.coerceIn(FUEL_RANGE))


		mobile.world.spawnParticle(
			Particle.SOUL, mobile.location.add(0.0, 1.0, 0.0), 10, 0.13, 0.13, 0.13, 0.0
		)
		fuel.subtract()
		return true
	}

	@EventHandler
	fun onSnowMobileMove(ev: EntityMoveEvent) {
		val snowmobile = ev.entity as? ArmorStand ?: return
		if (snowmobile.persistentDataContainer.missingFlag(ENT_SNOWMOBILE)) return

		snowmobile.updateModel()
	}

	private fun ArmorStand.updateModel() {
		val pos = location.subtract(0.0, MODEL_Y_OFFSET, 0.0)
		val model = getModel()
		model.teleport(pos)

		val targetPitch = (-velocity.y / MAX_Y_VEL * 30)
		val currPitch = model.location.pitch
		val pitch = currPitch + (targetPitch - currPitch) * 0.2
		headPose = EulerAngle(toRadians(pitch), 0.0, 0.0)
		model.headPose = headPose

		model.equipment.helmet = model.equipment.helmet.apply {

			itemMeta = itemMeta.apply {
				persistentDataContainer.setFlag(SNOWMOBILE_ON_TEXTURE, velocity.multiply(XZ_VECTOR).lengthSquared() > 0.03)
			}
		}
	}

	/**
	 * Get the snowmobile from this component
	 */
	private fun ArmorStand.getSnowMobile(): ArmorStand? {
		val uuid = persistentDataContainer.getUUID(COMP_TO_SNOWMOBILE_REF) ?: return null
		return world.getEntity(uuid) as? ArmorStand?
	}

	/**
	 * Get the model of the snowmobile
	 */
	private fun ArmorStand.getModel(): ArmorStand {
		return persistentDataContainer.getUUID(SNOWMOBILE_TO_MODEL_REF).run {
			if (this == null) {
				createSnowMobileModel(this@getModel)
			} else {
				val ent = this@getModel.world.getEntity(this)
				if (ent == null) {
					createSnowMobileModel(this@getModel)
				} else {
					ent as ArmorStand
				}
			}
		}
	}

	private val ItemStack.isSnowMobileItem
		get() = type != Material.AIR && itemMeta.persistentDataContainer.hasFlag(IS_SNOWMOBILE_ITEM)
}
