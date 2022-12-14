package app.bishan.juicebox.feature.deco.fish

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.utils.getVector
import app.bishan.juicebox.utils.setVector
import net.kyori.adventure.text.Component
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.animal.TropicalFish
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftTropicalFish
import org.bukkit.util.Vector

class Goldfish(location: Location) : TropicalFish(EntityType.TROPICAL_FISH, (location.world as CraftWorld).handle) {
	companion object {
		val BOWL_LOCATION get() = JuiceboxPlugin.key("bowl_location")
	}

	private val craftFish get() = CraftTropicalFish(Bukkit.getServer() as CraftServer, this)
	private var bowlLocation: Vector
		set(value) = craftFish.persistentDataContainer.setVector(BOWL_LOCATION, value)
		get() = craftFish.persistentDataContainer.getVector(BOWL_LOCATION)!!


	init {
		variant = 17039360
		// move toward the center of the block
		goalSelector.addGoal(0, RandomStrollGoal(this, 0.5))
		setPos(Vec3(location.x, location.y, location.z))
		bowlLocation = location.toVector()
	}

	override fun tick() {
		super.tick()
		return

		val fish = craftFish
		val bowl = bowlLocation.toLocation(fish.world)

		// constrain position to fishbowl
		if (fish.location.distanceSquared(bowl) > 1.0) {
			Bukkit.broadcast(Component.text("currPos: ${fish.location}"))
			Bukkit.broadcast(Component.text("bowlPos: $bowl"))
			Bukkit.broadcast(Component.text("fishbowl is too far away!"))
			setPos(Vec3(bowl.x, bowl.y, bowl.z))
		}
	}
}
