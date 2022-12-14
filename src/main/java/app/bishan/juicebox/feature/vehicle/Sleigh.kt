package app.bishan.juicebox.feature.vehicle

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.util.Mth.clamp
import org.bukkit.*
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Boat
import org.bukkit.entity.ChestBoat
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Slime
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.vehicle.VehicleDamageEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.exp

typealias CarrierEntity = Slime

private class Carrier(val index: Int, val ent: CarrierEntity) {
	companion object {
		const val MOTION_DELAY = 1.0
		const val MOTION_DELAY_MIN = 0.5
	}

	fun update(boat: ChestBoat) {
		var forward = boat.location.direction.normalize().add(boat.velocity.normalize()).normalize()

		if (forward.x.isNaN()) {
			forward = boat.location.direction.normalize()
		}

		val center = boat.location.add(forward.multiply(2.5))
		val right = boat.location.direction.rotateAroundY(Math.PI / 2)


		val stageOffset = (index / 2) * 0.6
		val columnOffset = ((index % 2) - 0.5) * 0.9

		val target = center.add(forward.multiply(stageOffset)).add(right.multiply(columnOffset))

		val stage = 1 - 2 * ((index / 2) / Sleigh.CARRIER_COUNT.toDouble())
		val motionDelay = exp(-MOTION_DELAY * stage) * (1 - stage) * (1 - MOTION_DELAY_MIN) + MOTION_DELAY_MIN
		val delta = target.subtract(ent.location).toVector().multiply(
			Vector(motionDelay, 0.6, motionDelay)
		)

		if (delta.x.isNaN() || delta.x.isInfinite()) return
		if (delta.y.isNaN() || delta.y.isInfinite()) return
		if (delta.z.isNaN() || delta.z.isInfinite()) return

		val newPos = ent.location.add(delta)

//		ent.size = 1 + (5.0 * delta.length()).toInt()
		newPos.yaw = center.yaw
		ent.teleport(newPos)
		ent.velocity = boat.velocity

//		if (boat.passengers.isEmpty()) return
//		val player = boat.passengers.first() as? CraftPlayer ?: return
//		val conn = player.handle.connection
//
//		val passengerDelta = ent.location.subtract(newPos.add(boat.velocity.multiply(3.0))).multiply(20.0)
//		conn.send(
//			ClientboundMoveEntityPacket.Pos(
//				ent.entityId,
//				passengerDelta.x.toInt().toShort(),
//				passengerDelta.y.toInt().toShort(),
//				passengerDelta.z.toInt().toShort(),
//				false
//			)
//		)
	}
}

object Sleigh : Feature("sleigh", true) {
	private val SLEIGH_CARRIERS get() = JuiceboxPlugin.key("Carriers")
	private val IS_SLEIGH = JuiceboxPlugin.key("IsSleigh")
	private val IS_CARRIER = JuiceboxPlugin.key("IsCarrier")
	private val OWNER_BOAT = JuiceboxPlugin.key("OwnerBoat")

	private const val HOTBAR_MAX_SPEED = 0.95
	private const val HOTBAR_SPEED_ROLLOFF = 1.4

	private val CARRIER_NAMES = listOf(
		Component.text("Snowmunch the Pretty", NamedTextColor.BLUE),
		Component.text("Snowmoo the adorbz", NamedTextColor.DARK_RED),
		Component.text("Voxen the AWOOOOOOGA HUMANA HUMANA HUMANA WOOOOOOOW NOW THATS A DAME", NamedTextColor.DARK_GREEN),
		Component.text("snowsam1212 the Ice Ice Baby", NamedTextColor.AQUA),
		Component.text("Celestial Polar Bears the Important", NamedTextColor.DARK_AQUA),
		Component.text("[VIP] Renideer the Cutest", NamedTextColor.LIGHT_PURPLE),
		Component.text("Roudolph the Red Nose Girlboss", NamedTextColor.RED),
		Component.text("Vola.", NamedTextColor.GREEN),
		Component.text("SnowTheScion the Scion", NamedTextColor.DARK_PURPLE),
		Component.text(
			"Jonah the Limited Time Flavored Lays Wavy Funyuns Onion Flavored Funyuns Onion Flavored Rings Potato Chips",
			NamedTextColor.GOLD
		),
	)
	val CARRIER_COUNT = CARRIER_NAMES.size

	private fun Entity.soundSleighEnter() = world.playSound(this, Sound.BLOCK_BELL_USE, 100f, 1.0f)
	private fun Entity.soundSleighLeave() = world.playSound(this, Sound.BLOCK_BELL_USE, 5f, 0.8f)

	@EventHandler
	fun onVehicleDamage(ev: VehicleDamageEvent) {
		ev.vehicle.asSleigh() ?: return

		if (ev.attacker is AbstractArrow)
			ev.isCancelled = true
	}

	@EventHandler
	fun onVehicleDamage(ev: ProjectileHitEvent) {
		ev.hitEntity?.asSleigh() ?: return
		ev.isCancelled = true
	}

	@EventHandler
	fun onVehicle(ev: VehicleEnterEvent) {
		if (ev.entered.persistentDataContainer.hasFlag(IS_CARRIER)) {
			ev.isCancelled = true
			return
		}

		val notBishan = !isEntityUUID(ev.entered, PlayersUUID.BISHAN)

		if (ev.vehicle.persistentDataContainer.hasFlag(IS_SLEIGH) && notBishan) {
			ev.isCancelled = true
			return
		}

		if (notBishan) return
		val boat = ev.vehicle.asSleigh(false) ?: return

		boat.carriers().forEach { it.ent.remove() }
		boat.persistentDataContainer.remove(SLEIGH_CARRIERS)
		boat.persistentDataContainer.remove(IS_SLEIGH)
		createSleigh(boat)
		boat.soundSleighEnter()
	}

	@EventHandler
	private fun onVehicleCollision(ev: VehicleEntityCollisionEvent) {
		ev.vehicle.asSleigh() ?: return
		if (ev.entity.persistentDataContainer.hasFlag(IS_CARRIER)) ev.isCancelled = true
	}

	@EventHandler
	private fun onDestroy(ev: EntityDeathEvent) {
		if (ev.entity.persistentDataContainer.hasFlag(IS_CARRIER)) {
			val boat = ev.entity.persistentDataContainer.getString(OWNER_BOAT)
			if (boat == null || Bukkit.getEntity(UUID.fromString(boat)) == null) {
				ev.entity.remove()
			}
			ev.isCancelled = true
		}
	}

	private fun createSleigh(boat: ChestBoat): List<Carrier> {
		val carriers = mutableListOf<Carrier>()

		for (i in 0 until CARRIER_COUNT) {
			val ent = boat.world.spawnEntity(boat.location, EntityType.SLIME) as CarrierEntity
			ent.setAI(false)
			ent.isInvulnerable = true
			ent.lootTable = null
			ent.size = 1
			ent.setWander(false)
			ent.customName(CARRIER_NAMES[i])
			ent.persistentDataContainer.raiseFlag(IS_CARRIER)
			ent.isCollidable = false
			ent.persistentDataContainer.setString(OWNER_BOAT, boat.uniqueId.toString())

			if (i < 2) ent.setLeashHolder(boat)
			else ent.setLeashHolder(carriers[i - 2].ent)
			carriers += Carrier(i, ent)
		}

		boat.setGravity(true)
		boat.persistentDataContainer.setString(SLEIGH_CARRIERS, carriers.joinToString(separator = "") {
			"${it.index}:${it.ent.uniqueId}\n"
		})
		boat.persistentDataContainer.raiseFlag(IS_SLEIGH)
		return carriers
	}

	@EventHandler
	private fun onVehicleMove(ev: VehicleMoveEvent) {
		if (ev.vehicle.passengers.isEmpty()) return
		val boat = ev.vehicle.asSleigh() ?: return

		if (isEntityUUID(boat.passengers.first(), PlayersUUID.BISHAN)) {
			val player = boat.passengers.first() as Player

			if (player.inventory.itemInOffHand.type == Material.BELL) {
				val boostPercentage = 1 - clamp(player.inventory.heldItemSlot / 8.0, 0.0, 1.0)
				val boost = exp(-boostPercentage * HOTBAR_SPEED_ROLLOFF) * (1 - boostPercentage) * HOTBAR_MAX_SPEED
				val loc = boat.location
				loc.pitch = player.eyeLocation.pitch
				boat.velocity = loc.direction.multiply(boost)
				boat.setGravity(false)
			} else {
				boat.setGravity(true)
			}
		}

		boat.carriers().forEach { it.update(boat) }
	}

	@EventHandler
	private fun onVehicleLeft(ev: VehicleExitEvent) {
		val boat = ev.vehicle.asSleigh() ?: return
		boat.setGravity(false)
		boat.carriers().forEach {
			it.ent.setWander(true)
			it.ent.setAI(true)
			it.ent.isCollidable = true
		}
		boat.setGravity(true)
		boat.soundSleighLeave()
	}

	@EventHandler
	private fun onVehicleDestroyed(ev: VehicleDestroyEvent) {
		val boat = ev.vehicle.asSleigh() ?: return
		boat.carriers().forEach { it.ent.remove() }
		boat.soundSleighLeave()
	}


	// Present Mechanics
	private val IS_PRESENT get() = JuiceboxPlugin.key("isPresent")
	private val PRESENT_TEXTURES = setOf(
		"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGQyOWIwNmM3YzM3Y2JkMmE3NjU5MDgyNzdmYThlYWQwZTRkYzY2YTExM2YzNDdkZTNiYWI5MWZhZGU0NjkxMiJ9fX0=",
		"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDZlMTJmZGFlMWZjZWJhNjg3OWY2NTk3OTYxMzJhN2ZmYTA4Y2Q5MmEyNmNiN2ExMDY3ZDQ5NzAzZDdiMWI0YiJ9fX0=",
		"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlNDJlMzcyNWMyYjRhZTY5MDA1ODBjNGUyYTZiODMwZjZlY2EwMjExZjdhMzY0MTQzM2ZjNjdmYmM0M2QzZiJ9fX0=",
		"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTM3NTA2MWQwOGYxZDdiMzE3Njc1YWE3ZmE4ODAwZDZmMjA2NmUwMThkOWY5MWVjZGRmOWNhZjMwNGU5N2U5MiJ9fX0=",
		"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTcyNmQ5ZDA2MzJlNDBiZGE1YmNmNjU4MzliYTJjYzk4YTg3YmQ2MTljNTNhZGYwMDMxMGQ2ZmM3MWYwNDJiNSJ9fX0="
	)
	private val PRESENT_CONTENT_BYTES get() = JuiceboxPlugin.key("presentContentBytes")

	@EventHandler
	private fun onRightClickBell(ev: PlayerDropItemEvent) {
		val player = ev.player
		if (!isEntityUUID(player, PlayersUUID.BISHAN)) return
		if (!player.isInsideVehicle) return

		var vehicle = player.vehicle!!
		while (vehicle.isInsideVehicle) vehicle = vehicle.vehicle!!

		val boat = vehicle.asSleigh()
			?: if (vehicle.persistentDataContainer.hasFlag(IS_CARRIER))
				vehicle
			else return

		val presentContentsBytes = ev.itemDrop.itemStack.serializeAsBytes()


		val present = boat.world.spawnEntity(boat.location, EntityType.ARMOR_STAND) as ArmorStand
		present.persistentDataContainer.setByteArray(PRESENT_CONTENT_BYTES, presentContentsBytes)

		val presentItem = ItemStack(Material.PLAYER_HEAD).apply {
			itemMeta = (itemMeta as SkullMeta).apply {
				playerProfile = Bukkit.createProfile(PlayersUUID.BISHAN).apply {
					setProperty(
						ProfileProperty(
							"textures", PRESENT_TEXTURES.random()
						)
					)
				}
			}
		}
		present.velocity = boat.velocity.add(ev.itemDrop.velocity)
		ev.itemDrop.remove()

		present.equipment.helmet = presentItem
		present.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.REMOVING_OR_CHANGING)
		present.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.REMOVING_OR_CHANGING)
		present.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.REMOVING_OR_CHANGING)
		present.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING)
		present.isInvulnerable = true
		present.isInvisible = true
		present.isCollidable = false
//		present.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, false, false, false))
		present.persistentDataContainer.raiseFlag(IS_PRESENT)
//		present.isInvisible = treu

//		val conn = (player as CraftPlayer).handle.connection
//		conn.send(ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 0f))

		var task = -1
		task = Bukkit.getScheduler().scheduleSyncRepeatingTask(
			JuiceboxPlugin.instance, fun() {
				if (!present.isValid || present.isDead) {
					Bukkit.getScheduler().cancelTask(task)
					return
				}

				if (!present.isOnGround) return

				present.setGravity(false)
				present.setAI(false)
				present.teleport(present.location.subtract(0.0, 1.45, 0.0))
				Bukkit.getScheduler().cancelTask(task)
			}, 0L, 2L
		)
	}

	@EventHandler
	private fun onOpenPresent(ev: PlayerInteractAtEntityEvent) {
		val present = ev.rightClicked as? ArmorStand ?: return

		if (!present.persistentDataContainer.hasFlag(IS_PRESENT)) return

		val player = ev.player

		val content =
			present.persistentDataContainer.getByteArray(PRESENT_CONTENT_BYTES) ?: ItemStack(Material.AIR).serializeAsBytes()
		val item = ItemStack.deserializeBytes(content)


		player.world.playSound(present, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
		present.world.spawnParticle(Particle.CRIT, present.location, 100, 0.3, 0.3, 0.3, 0.5)
		player.world.dropItemNaturally(present.location, item)
		present.remove()
	}

	private fun ChestBoat.carriers(): List<Carrier> {
		val carrierUUIDString = persistentDataContainer.getString(SLEIGH_CARRIERS) ?: return createSleigh(this)


		val slimes = carrierUUIDString.split("\n").mapNotNull { it ->
			val split = it.split(":")
			if (split.any(String::isEmpty)) return@mapNotNull null

			val index = split[0].toInt()
			val uuid = UUID.fromString(split[1])
			val slime = Bukkit.getEntity(uuid)

			if (slime == null) null else Carrier(index, slime as CarrierEntity)
		}

		return if (slimes.size < CARRIER_COUNT) {
			slimes.forEach { it.ent.remove() }
			createSleigh(this)
		} else slimes
	}

	private fun Entity?.asSleigh(checkFlag: Boolean = true): ChestBoat? {
		this ?: return null
		if (this !is ChestBoat) return null
		val boat = this
		if (boat.boatType != Boat.Type.SPRUCE) return null

		return if (!checkFlag || persistentDataContainer.hasFlag(IS_SLEIGH)) boat
		else null
	}
}
