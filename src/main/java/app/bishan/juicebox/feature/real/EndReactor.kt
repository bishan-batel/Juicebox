package app.bishan.juicebox.feature.real

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Rabbit
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.generator.structure.StructureType
import org.bukkit.util.Vector
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

object EndReactor : Feature("end_reactor", false) {
	@EventHandler
	private fun onInteractEndCrystal(ev: PlayerInteractAtEntityEvent) {
		val player = ev.player
		val crystal = ev.rightClicked
		if (crystal !is EnderCrystal) return

		val itemInHand = player.inventory.getItem(ev.hand)
		if (itemInHand.type != Material.NETHER_STAR) return
		itemInHand.subtract()

		crystal.isInvulnerable = true

		val world = crystal.world
		val rabbit = crystal.world.spawn(crystal.location, Rabbit::class.java)
		rabbit.velocity = Vector(0.0, 0.8, 0.0)
		rabbit.isInvisible = true
		rabbit.isSilent = true
		rabbit.isInvulnerable = true
		rabbit.isCollidable = false
		rabbit.setGravity(false)

		rabbit.addPassenger(crystal)

		val originalY = crystal.location.y

		var task = 0
		task = Bukkit.getScheduler().scheduleSyncRepeatingTask(
			JuiceboxPlugin.instance,
			fun() {
				if (rabbit.velocity.y < 0.5) {
					// summon ender dragon
					Bukkit.getScheduler().cancelTask(task)

					world.createExplosion(crystal.location, 3.0f, false, false)

					// generate a end city structure
//					Bukkit.getServer().structureManager.getStructure(
//						org.bukkit.structure.Structure

//					)


//					if (Bukkit.getCommandMap().dispatch(
//							player,
//							"/place structure minecraft:end_city"
//						)
//					) {
//					}
//					Bukkit.getStructureManager().getStructure(StructureType.END_CITY.key)!!.place(
//						crystal.location,
//						true,
//						StructureRotation.NONE,
//						Mirror.NONE,
//						0,
//						1f,
//						Random()
//					)
//					val structure = io.papermc.paper.world.structure.ConfiguredStructure.END_CITY
//					io.papermc.paper.world.structure.PaperConfiguredStructure.init()

					crystal.remove()
					rabbit.remove()
				}
				// spawn end particles in a circle around the crystal
				val loc = crystal.location
				val radius = 4.0
				val steps = 30

				for (i in 0 until steps) {
					val angle = 2 * Math.PI * i / steps + loc.y

					val r = radius - (loc.y - originalY) / 6
					val x = r * cos(angle)
					val z = r * sin(angle)
					loc.add(x, 0.0, z)
					loc.world.spawnParticle(org.bukkit.Particle.END_ROD, loc, 1, 0.0, 0.0, 0.0, 0.0)
					loc.subtract(x, 0.0, z)
				}


			}, 0L, 1L
		)
	}
}
