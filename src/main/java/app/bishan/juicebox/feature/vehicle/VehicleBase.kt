package app.bishan.juicebox.feature.vehicle

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector

abstract class VehicleBase<V : Entity>(name: String, defaultActive: Boolean) : Feature("vehicle:$name", defaultActive) {
	val vehicleName = name

	override val description: TextComponent
		get() = Component.text {
			it.append(Component.text("Vehicle: $vehicleName"))
		}

	private var models: List<EntityComponent<V, out Entity>> = listOf()
	private val vehicleDrivingTask = mutableMapOf<String, List<BukkitTask>>()

	// persistent data keys
	private val entityFlag = JuiceboxPlugin.key("is_vehicle_$vehicleName")
	private val fuelLevelKey = JuiceboxPlugin.key("vehicle_${vehicleName}_fuel")

	open val maxFuel get() = 1
	open val maxRPM get() = 1

	protected var V.fuelLevel: Double
		set(value) = persistentDataContainer.setDouble(fuelLevelKey, value)
		get() = persistentDataContainer.getDouble(fuelLevelKey) ?: 0.0

	// RPM
	private var vehicleRPMS = mutableMapOf<V, Double>()
	protected var V.rpm: Double
		set(value) = vehicleRPMS.set(this, value)
		get() = vehicleRPMS.getOrDefault(this, 0.0)

	// RPM Delta
	private var vehicleRPMsDelta = mutableMapOf<V, Double>()
	protected var V.rpmDelta: Double
		set(value) = vehicleRPMsDelta.set(this, value)
		get() = vehicleRPMsDelta.getOrDefault(this, 0.0)

	protected abstract fun createAllModels(): List<EntityComponent<V, out Entity>>

	override fun onEnable() {
		killVehicleTasks()
		models = createAllModels()
	}

	override fun onDisable() {
		killVehicleTasks()
	}

	/**
	 * Returns how much fuel an item is worth.
	 */
	open fun getFuelWorth(item: ItemStack) = when (item.type) {
		Material.COAL -> 1.0
		Material.COAL_BLOCK -> 9.0
		Material.LAVA_BUCKET -> 2.0
		Material.CHARCOAL -> 1.0
		Material.STICK -> 0.1
		else -> 0.0
	}

	abstract fun V.vehicleDrivingTick()

	protected fun V.startVehicleTask(freq: Long = 1) {
		vehicleDrivingTask["$uniqueId"] = listOf(Scheduler.onceEvery(freq) {
			vehicleDrivingTick()
		})
	}

	protected fun V.cancelVehicleTask() {
		vehicleDrivingTask["$uniqueId"]?.forEach { it.cancel() }
		vehicleDrivingTask.remove("$uniqueId")
	}

	fun killVehicleTasks() {
		vehicleDrivingTask.values.forEach {
			it.forEach { task -> task.cancel() }
		}
		vehicleDrivingTask.clear()
	}

	/**
	 * Spawn a vehicle at the given location
	 */
	fun spawn(loc: Location): V {
		val entity = createVehicleEntity(loc)
		entity.persistentDataContainer.raiseFlag(entityFlag)
		return entity
	}

	/**
	 * Creates the vehicle entity at the given location, should not be called
	 * by itself
	 */
	protected abstract fun createVehicleEntity(loc: Location): V

	/**
	 * Updates all the models of the vehicle, given the vehicle's location and direction.
	 * @param vehicle The vehicle to update the models of.
	 * @param forward The direction the vehicle is facing.
	 * @param right The direction to the right of the vehicle.
	 * @param up The direction above the vehicle.
	 * @param baseAngle The base angle to use for the models.
	 */
	protected open fun updateModels(vehicle: V, forward: Vector, right: Vector, up: Vector, baseAngle: EulerAngle) {
		models.forEach {
			it.updateModel(vehicle, forward, right, up, baseAngle)
		}
	}

	/**
	 * Attempts to cast the entity to the vehicle type.
	 * @return The vehicle if the entity is a vehicle, null otherwise.
	 */
	fun Entity?.asVehicleBase(): V? {
		if (this == null) return null
		Bukkit.broadcast(Component.text("$entityFlag"))
		if (persistentDataContainer.missingFlag(entityFlag)) return null

		@Suppress("UNCHECKED_CAST") return this as? V
	}

	/**
	 * Entity component for a vehicle.
	 */
	open class EntityComponent<V : Entity, S : Entity>(base: VehicleBase<V>, private val id: String) {
		private val componentFlag = JuiceboxPlugin.key("is_vehicle${base.vehicleName}_$id")
		private val baseReferenceToSelf = JuiceboxPlugin.key("vehicle_${base.vehicleName}_component_$id")
		private val referenceToBase = JuiceboxPlugin.key("vehicle_${base.vehicleName}_base_ref")

		private var componentBuilder: ((V) -> S)? = null
		protected var positionalOffset = Vector(0.0, 0.0, 0.0)
		protected var angleOffset = EulerAngle(0.0, 0.0, 0.0)

		fun onCreate(builder: (V) -> S): EntityComponent<V, S> {
			componentBuilder = builder
			return this
		}

		fun position(offset: Vector) {
			positionalOffset = offset
		}

		fun angle(offset: EulerAngle) {
			angleOffset = offset
		}

		fun updateModel(vehicle: V, forward: Vector, right: Vector, up: Vector, baseAngle: EulerAngle) {
			Bukkit.broadcast(Component.text("updateModel $componentFlag"))
			val model = getComponentFor(vehicle)
			updateModel(vehicle, model, forward, right, up, baseAngle)
		}

		open fun updateModel(vehicle: V, comp: S, forward: Vector, right: Vector, up: Vector, baseAngle: EulerAngle) {
			val loc = vehicle.location

			loc.add(forward.clone().multiply(positionalOffset.x))
			loc.add(up.clone().multiply(positionalOffset.y))
			loc.add(right.clone().multiply(positionalOffset.z))

			// rotational offset
			val angle = baseAngle.add(angleOffset.x, angleOffset.y, angleOffset.z)
			loc.yaw = angle.x.toFloat()
			loc.pitch = angle.y.toFloat()

			comp.teleport(loc)
		}

		private infix fun createFor(vehicle: V): S {
			val ent = componentBuilder?.invoke(vehicle) ?: error("No entity builder for $id")

			ent.persistentDataContainer.raiseFlag(componentFlag)
			ent.persistentDataContainer.setUUID(referenceToBase, vehicle.uniqueId)
			vehicle.persistentDataContainer.setUUID(baseReferenceToSelf, ent.uniqueId)

			return ent
		}

		fun getComponentFor(vehicle: V): S {
			val uuid = vehicle.persistentDataContainer.getUUID(baseReferenceToSelf) ?: return createFor(vehicle)
			val ent = vehicle.world.getEntity(uuid) ?: return createFor(vehicle)
			@Suppress("UNCHECKED_CAST") return ent as S
		}
	}

	/**
	 * Entity component for a vehicle.
	 */
	class ArmorStandComponent<V : Entity>(base: VehicleBase<V>, id: String) : EntityComponent<V, ArmorStand>(base, id) {
		override fun updateModel(
			vehicle: V, comp: ArmorStand, forward: Vector, right: Vector, up: Vector, baseAngle: EulerAngle
		) {
			positionalOffset.y -= comp.eyeHeight
			super.updateModel(vehicle, comp, forward, right, up, baseAngle)
			positionalOffset.y += comp.eyeHeight
			comp.headPose = baseAngle.add(angleOffset.x, angleOffset.y, angleOffset.z)
		}
	}
}
