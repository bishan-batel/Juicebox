package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import org.bukkit.*
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack

object MooMilk : Feature("moo_milk", true) {
	@EventHandler
	fun onInteractMadi(ev: PlayerInteractAtEntityEvent) {
		if (!isMadi(ev.rightClicked)) return
		val player = ev.player

		val itemInHand = player.inventory.getItem(ev.hand)
		val madi = ev.rightClicked as Player

		when (itemInHand.type) {
			Material.BUCKET -> onMilkInteract(ev, player, itemInHand, madi)
			Material.WHEAT -> onWheatInteract(ev, player, itemInHand, madi)
			else -> {}
		}
	}

	private fun onWheatInteract(
		ev: PlayerInteractAtEntityEvent, player: Player, itemInHand: ItemStack, madi: Player
	) {
		madi.world.playSound(madi, Sound.ENTITY_COW_AMBIENT, 1f, 1f)
		madi.world.spawnParticle(Particle.HEART, madi.location.add(0.0, 1.5, 0.0), 4, 0.15, 0.15, 0.15)
//		madi.world
		if (player.gameMode != GameMode.CREATIVE) itemInHand.subtract()
		madi.foodLevel++
	}

	private fun onMilkInteract(
		ev: PlayerInteractAtEntityEvent, player: Player, itemInHand: ItemStack, madi: Player
	) {
		if (player.gameMode == GameMode.CREATIVE || itemInHand.amount > 1) {
			player.inventory.addItem(ItemStack(Material.MILK_BUCKET))
		} else {
			player.inventory.setItem(ev.hand, ItemStack(Material.MILK_BUCKET))
		}

		madi.world.playSound(madi.location, "entity.cow.milk", 1f, 1f)
		madi.world.playSound(madi.location, "entity.cow.ambient", 1f, 1f)
	}


	private const val WHEAT_CANCEL_CHANCE = 0.001
	private const val WHEAT_EFFECT_RADIUS = 5.0
	private const val WHEAT_SPEED = 0.1

	@EventHandler
	fun onHoldWhat(ev: PlayerItemHeldEvent) {
		val player = ev.player
		val inv = player.inventory
		val item = inv.getItem(ev.newSlot) ?: return
		if (item.type != Material.WHEAT) return

		// if no players in world that are madi, return
		if (Bukkit.getOnlinePlayers().none { isMadi(it) }) return

		var task = -1
		task = Bukkit.getScheduler().scheduleSyncRepeatingTask(
			JuiceboxPlugin.instance, fun() {
				val notHoldingWheat = inv.itemInMainHand.type != Material.WHEAT && inv.itemInOffHand.type != Material.WHEAT

				if (!player.isValid || notHoldingWheat || Math.random() < WHEAT_CANCEL_CHANCE) {
					Bukkit.getScheduler().cancelTask(task)
					return
				}

//				player.setResourcePack()
				// force any player in a 5 block radius to look at the player
				player.world.getNearbyEntities(
					player.location, WHEAT_EFFECT_RADIUS, WHEAT_EFFECT_RADIUS, WHEAT_EFFECT_RADIUS, ::isMadi
				).forEach {
					val loc = it.location
					val toPlayer = player.location.toVector().subtract(loc.toVector()).normalize()
					loc.direction = toPlayer

					if (loc.distance(player.location) > 1.5) loc.add(loc.direction.multiply(WHEAT_SPEED))
					it.teleport(loc)
				}
			}, 0L, 1L
		)
	}

	private fun isMadi(human: Entity): Boolean = isEntityUUID(human, PlayersUUID.MADI)
}
