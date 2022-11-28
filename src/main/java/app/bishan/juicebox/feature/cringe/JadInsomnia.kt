package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EnderPearl
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

object JadInsomnia : Feature("jad_insomnia", true) {
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
			holding.amount--
		}

		player.setCooldown(Material.ENDER_PEARL, 20)
		ev.isCancelled = true
	}


	@EventHandler
	private fun onSleep(ev: PlayerBedEnterEvent) {
		if (Math.random() > 0.5) return
		if (!isEntityUUID(ev.player, PlayersUUID.JAD)) return
		val jad = ev.player
		Bukkit.getScheduler().runTaskLater(JuiceboxPlugin.instance, { _ ->
			jad.kick(
				Component.text(
					"You may not rest now. There are hot singles in your area."
				)
			)
		}, 30)
	}
}
