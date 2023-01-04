package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.isEntityUUID
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.Enderman
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.util.Vector

object MultishotEnderpearl : Feature("multishot_enderpearl", true) {
	@EventHandler
	private fun onPearl(ev: PlayerInteractEvent) {
		val player = ev.player

		if (!ev.hasItem()) return
		val item = ev.item!!

		if (item.type != Material.ENDER_PEARL) return
		if ((item.enchantments[Enchantment.MULTISHOT] ?: return) < 1) return
		if (player.getCooldown(Material.ENDER_PEARL) > 0) return

		for (i in 0..(3..10).random()) {
			val proj = player.launchProjectile(EnderPearl::class.java)
			val speed = proj.velocity.length()
			val offset = Vector.getRandom().multiply(0.25)
			proj.velocity = proj.velocity.add(offset).normalize().multiply(speed)
		}

		if (player.gameMode != GameMode.CREATIVE) {
			val holding = player.inventory.getItem(ev.hand ?: return)
			holding.subtract()
		}

		player.setCooldown(Material.ENDER_PEARL, 20)
		ev.isCancelled = true
	}

	@EventHandler
	private fun onEndermanDeath(ev: EntityDeathEvent) {
		val ent = ev.entity as? Enderman ?: return

		if (Math.random() < 0.1 && ent.world.isNatural) {
			for (drop in ev.drops) {
				drop.addUnsafeEnchantment(Enchantment.MULTISHOT, 1)
			}
		}
	}
}
