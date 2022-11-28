package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Team
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object LegacyBiplane : Feature("legacy_biplane", true) {
	private val BOAT_LAST_AIR_LOCATION_Y get() = NamespacedKey(JuiceboxPlugin.instance, "lastAirLocationY")
	private val BOAT_CRASH get() = NamespacedKey(JuiceboxPlugin.instance, "crash")
	private val BOAT_ROLL get() = NamespacedKey(JuiceboxPlugin.instance, "roll")
	private const val BOAT_SPEED = 10.0
	private const val BOAT_PITCH_SPEED = 0.33f
	private const val BOAT_MAX_PITCH = 90f

	private const val BOAT_ROLL_ANGLE_MAX = 35 * (Math.PI / 180.0)
	private const val BOAT_PITCH_ANGLE_MAX = BOAT_MAX_PITCH.toDouble() * (Math.PI / 180.0)

	private val BOAT_ARMOR_STAND get() = NamespacedKey(JuiceboxPlugin.instance, "stand")

	private val PLANE = run {
		val stack = ItemStack(Material.CARVED_PUMPKIN)
//		stack.editMeta {
//			it.displayName(Component.text("biplane"))
//		}
		val meta = stack.itemMeta
		if (meta != null) {
			meta.setDisplayName("biplane")
			stack.itemMeta = meta
		}
		stack
	}

	override fun onEnable() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(JuiceboxPlugin.instance, {
			Bukkit.getWorlds().forEach { world ->
				world.entities.forEach { ent ->
					if (ent is ChestBoat) tickBoatCrash(ent)
				}
			}
		}, 0L, 5L)

		Bukkit.getScheduler().scheduleSyncRepeatingTask(JuiceboxPlugin.instance, {
			Bukkit.getWorlds().forEach { world ->
				world.entities.forEach { ent ->
					if (ent is ChestBoat) {
						val prevStandId = ent.persistentDataContainer.getString(BOAT_ARMOR_STAND)
						if (prevStandId != null) {
							val stand = Bukkit.getEntity(UUID.fromString(prevStandId))
							if (stand != null) {

								val pitch = -(BOAT_PITCH_ANGLE_MAX * 20f * ent.velocity.y / BOAT_SPEED)
								val waterOffset = if (ent.isInWater) Vector(0.0, .35, 0.0) else Vector(0, 0, 0)
								stand.teleport(ent.location.add(waterOffset))
								val roll = ent.persistentDataContainer.getDouble(BOAT_ROLL) ?: 0.0
								(stand as ArmorStand).headPose = EulerAngle(pitch, 0.0, roll)
							}
						}
					}
				}
			}
		}, 0L, 1L)
	}

	private fun tickBoatCrash(boat: ChestBoat) {
		if (!boat.persistentDataContainer.hasFlag(BOAT_CRASH)) return
		boat.setGravity(true)
		if (!boat.isOnGround) return

		// do NOT google Mr.Hands
		val lastCoordY = boat.persistentDataContainer.getDouble(
			BOAT_LAST_AIR_LOCATION_Y
		) ?: boat.location.y

		val fallDist = abs(boat.location.y - lastCoordY)
		if (fallDist > 10) {
			boat.world.createExplosion(boat.location, 10f, false, false)
			val prevStandId = boat.persistentDataContainer.getString(BOAT_ARMOR_STAND)
			if (prevStandId != null) Bukkit.getEntity(UUID.fromString(prevStandId))?.remove()

		} else {
			boat.persistentDataContainer.setByte(BOAT_CRASH, 0)
		}
	}

	@EventHandler
	private fun onJoin(ev: PlayerJoinEvent) {
		if (!isNPC(ev.player)) return
		ev.player.isCustomNameVisible = false


		try {
			val team = Bukkit.getScoreboardManager()!!.mainScoreboard.registerNewTeam("PERRUINCHINO_NPC")
			team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
			team.addEntry(ev.player.name)
		} catch (_: Exception) {
		}
	}

	private fun isNPCBoat(vehicle: Vehicle?): Boolean =
		if (vehicle?.passengers?.isEmpty() != false) false else isNPCBoat(vehicle.passengers[0], vehicle)

	private fun isNPCBoat(passenger: Entity, vehicle: Vehicle?) =
		vehicle != null && isNPC(passenger) && vehicle.type == EntityType.CHEST_BOAT && (vehicle as ChestBoat).boatType == Boat.Type.MANGROVE

	@EventHandler
	private fun onInteract(ev: PlayerInteractAtEntityEvent) {
		if (ev.rightClicked.type != EntityType.PLAYER) return
		if (!isNPC(ev.rightClicked)) return

		ev.player.sendTitle("atlanta spelt backwards is", "is still atlanta", 10, 70, 20)
	}

	@EventHandler
	private fun onVehicleEnter(ev: VehicleEnterEvent) {
		if (!isNPCBoat(ev.entered, ev.vehicle)) return
		ev.vehicle.setGravity(false)

		val prevStandId = ev.vehicle.persistentDataContainer.getString(BOAT_ARMOR_STAND)
		if (prevStandId != null) {
			Bukkit.getEntity(UUID.fromString(prevStandId))?.remove()
		}

		val stand = ev.vehicle.world.spawnEntity(ev.vehicle.location, EntityType.ARMOR_STAND) as ArmorStand
		stand.isVisible = false
		stand.setGravity(false)
		stand.isSilent = true
		stand.setAI(false)
		stand.equipment.setHelmet(PLANE, true)
		stand.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING)
		stand.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING)
		stand.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.ADDING)
		stand.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.ADDING)
		ev.vehicle.customName = "biplane"
		ev.vehicle.persistentDataContainer.setString(BOAT_ARMOR_STAND, stand.uniqueId.toString())
	}

	@EventHandler
	private fun onVehicleExit(ev: VehicleExitEvent) {
		if (!isNPCBoat(ev.exited, ev.vehicle)) return
		ev.vehicle.setGravity(true)
	}

	@EventHandler
	private fun onVehicleDestroyed(ev: VehicleDestroyEvent) {
		val prevStandId = ev.vehicle.persistentDataContainer.getString(BOAT_ARMOR_STAND) ?: return
		Bukkit.getEntity(UUID.fromString(prevStandId))?.remove()
	}

	@EventHandler
	private fun onPlaneMove(ev: VehicleMoveEvent) {
		if (!isNPCBoat(ev.vehicle)) return
		val boat = ev.vehicle as ChestBoat
		val fuelItems = boat.inventory.filter { it != null && it.type.isFuel }

		ev.to.yaw - ev.from.yaw

		if (fuelItems.isEmpty()) {
			boat.persistentDataContainer.setFlag(BOAT_CRASH, true)
			boat.setGravity(true)
			return
		} else {
			boat.setGravity(false)
			boat.persistentDataContainer.setDouble(BOAT_LAST_AIR_LOCATION_Y, boat.location.y)
		}

		val speedBoost = run {
			if (fuelItems.isEmpty()) return@run 0.0
			val item = fuelItems[0]

			if (Math.random() < 0.03) item.amount--
			1.0
		} / 20

		val yawLoc = boat.location
		val yawVel = yawLoc.direction.multiply(speedBoost * BOAT_SPEED)
		val pitchVel =
			max(min(boat.passengers[0]!!.location.pitch / 90, BOAT_MAX_PITCH), -BOAT_MAX_PITCH) * BOAT_PITCH_SPEED

		boat.velocity = boat.velocity.add(yawVel.subtract(Vector(0f, pitchVel, 0f)))

		val turnStrength = (ev.to.yaw - ev.from.yaw) / Math.PI
		val roll = sigmoidSigned(turnStrength) * BOAT_ROLL_ANGLE_MAX
		boat.persistentDataContainer.setDouble(BOAT_ROLL, roll)
	}

	private fun isNPC(player: Entity) = isEntityUUID(player, PlayersUUID.BISHAN)
}
