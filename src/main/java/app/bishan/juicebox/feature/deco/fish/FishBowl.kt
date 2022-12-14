package app.bishan.juicebox.feature.deco.fish

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Marker
import org.bukkit.entity.TropicalFish
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent

object FishBowl : Feature("fishbowls", false) {

	@EventHandler
	private fun onFillFishBowl(ev: PlayerInteractEvent) {
		if (ev.action != Action.RIGHT_CLICK_BLOCK) return
		if (!ev.hasItem()) return

		val item = ev.item!!
		if (item.type != Material.POTION) return

		val block = ev.clickedBlock!!
		if (block.type != Material.GLASS) return
//		item.type = Material.GLASS_BOTTLE

		block.type = Material.WATER

//		val fallingBlock =
//			block.world.spawnFallingBlock(
//				block.location.toCenterLocation().subtract(0.0, 0.5, 0.0),
//				Material.GLASS.createBlockData()
//			)
//		fallingBlock.dropItem = false
//		fallingBlock.setHurtEntities(false)
//		fallingBlock.setGravity(false)
//		fallingBlock.isInvulnerable = true
//		fallingBlock.isSilent = true

		// add goldfish
		val goldfish = Goldfish(block.location.toCenterLocation())
		(block.world as CraftWorld).handle.addFreshEntity(goldfish)


		val loc = block.location.toCenterLocation()
		Bukkit.getScheduler().runTaskLater(JuiceboxPlugin.instance, fun() {
//			goldfish.teleportTo(loc.x, loc.y, loc.z)
		}, 1L)
		ev.isCancelled = true
	}

}
