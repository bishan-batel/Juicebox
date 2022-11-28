package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object BucketOfOrca : Feature("bucket_of_orca", true) {
	private val KEY_ORCA_BUCKET get() = JuiceboxPlugin.key("has_orca")
	const val ORCA_DIMENSION_NAME = "world_orca_prison"
	const val GENERATOR_SETTINGS = """{
			 "layers": [{"block":"air","height":64},{"block":"cyan_concrete","height":1}, {"block": "water", "height": 3}],
			 "biome":"minecraft:bamboo_jungle"
}
"""

	private fun waterDimension(): World {
		val existingWorld = Bukkit.getWorld(ORCA_DIMENSION_NAME)
		if (existingWorld != null) return existingWorld

		// json object
//		generator.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)


		// superflat water world
		val wc = WorldCreator(ORCA_DIMENSION_NAME)
//		wc.biomeProvider(object : BiomeProvider() {
//			override fun getBiome(worldInfo: WorldInfo, x: Int, y: Int, z: Int): Biome {
//			}
//
//			override fun getBiomes(worldInfo: WorldInfo): MutableList<Biome> {
//			}
//		} )
		wc.environment(World.Environment.NORMAL)
		wc.generateStructures(false)
		wc.type(WorldType.FLAT)
		wc.generatorSettings(GENERATOR_SETTINGS)
		val world = Bukkit.getServer().createWorld(wc)!!
		world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
		world.setStorm(true)
		return world
	}


	@EventHandler
	fun dump(ev: PlayerInteractEvent) {
		if (ev.action != Action.RIGHT_CLICK_BLOCK) return
		val item = ev.item ?: return
		if (ev.player.hasCooldown(item.type)) return
		if (!item.itemMeta?.persistentDataContainer?.hasFlag(KEY_ORCA_BUCKET)!!) return

		val players = waterDimension().players
		// get player orca
		players.forEach { orca ->
			// teleport orca to player
			orca.teleport(ev.clickedBlock!!.location)

//			// remove orca from player
		}
		ev.player.setCooldown(Material.TROPICAL_FISH_BUCKET, 10)
		ev.isCancelled = true
	}

	@EventHandler
	fun onDeath(ev: PlayerDeathEvent) {
		if (ev.entity.world == waterDimension()) {
			ev.keepInventory = true
			ev.isCancelled = true
		}
	}

	@EventHandler
	fun interact(ev: PlayerInteractAtEntityEvent) {
		if (!isEntityUUID(ev.rightClicked, PlayersUUID.ORCAS)) return
		val orca = ev.rightClicked as Player

		val itemInHand = ev.player.inventory.getItem(ev.hand)


		if (itemInHand.type == Material.WATER_BUCKET) {
			val orcaBucket = ItemStack(Material.TROPICAL_FISH_BUCKET)
			val im = orcaBucket.itemMeta?.apply {
				displayName(Component.text("Bucket of Orca").decoration(TextDecoration.ITALIC, false))
				persistentDataContainer.setFlag(KEY_ORCA_BUCKET, true)
			}
			orcaBucket.itemMeta = im
			orcaBucket.addUnsafeEnchantment(Enchantment.CHANNELING, 1)

			ev.player.inventory.setItem(ev.hand, orcaBucket)
		} else if (itemInHand.itemMeta?.persistentDataContainer?.hasFlag(KEY_ORCA_BUCKET) != true) {
			return
		}
		if (ev.player.hasCooldown(itemInHand.type)) return

		ev.player.setCooldown(Material.TROPICAL_FISH_BUCKET, 10)

		// get all entities riding orca in a list
		val stack = mutableListOf(orca as Entity)

		var ent = stack[0]
		// gets bottom of stack
		while (ent.vehicle != null) ent = ent.vehicle!!

		stack.add(ent)
		while (ent.passengers.isNotEmpty()) {
			ent.passengers.forEach { stack.add(it) }
			ent = ent.passengers.last()
		}

		val loc = Location(waterDimension(), 127.0, 256.0, 0.0)

		stack.reversed().forEach { it.teleport(loc) }

		ent = stack[0]
		for (i in 1 until stack.size) {
			ent.addPassenger(stack[i])
			ent = stack[i]
		}

		ev.isCancelled = true
	}
}
