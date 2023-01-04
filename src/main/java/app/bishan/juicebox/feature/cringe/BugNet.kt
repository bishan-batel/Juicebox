package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.feature.internal.ResourcePack
import app.bishan.juicebox.feature.internal.WanderingRecipe
import app.bishan.juicebox.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import java.util.*
import kotlin.math.sqrt

object BugNet : Feature("bug_net", true) {
	private val IS_BUGNET get() = JuiceboxPlugin.key("IsBugnet")
	private val BUGNET_CAPTURE get() = JuiceboxPlugin.key("bugnet_capture_uuid")
	private const val THROW_PARTICLE_LIFE = 1000
	private const val NET_COOLDOWN_TICKS = 15
	private const val EXPERIENCE_COST_COOLDOWN = 2
	private const val LAUNCH_SPEED = 1.1

	private val BUGNET = ItemStack(Material.GOLDEN_SWORD).apply {
		itemMeta = itemMeta.apply {
			persistentDataContainer.raiseFlag(IS_BUGNET)
			displayName(
				Component.text("Orcas Bug Net", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)
			)
			lore(
				listOf(
					Component.text("Right click a mob to capture it!", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false),
				) + ResourcePack.NO_TEXTURE_MESSAGE
			)
		}
		addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
	}
	private val BUGNET_TRADE = WanderingRecipe(BUGNET, 1).chance(0.01 * 0.1).minTemp(.25).maxTemp(.901)

	override fun onEnable() {
		addCustomItem("bug_net", BUGNET)
		addWanderingTrade(BUGNET_TRADE)
	}

	@EventHandler
	private fun onDrop(ev: PlayerInteractEvent) {
		val net = ev.player.inventory.getItem(ev.hand ?: return)
		if (!net.isBugNet) return

		val player = ev.player

		if (player.hasCooldown(net.type)) return
		val captured = net.captured ?: return

		ev.isCancelled = true

		net.itemMeta = net.itemMeta.apply {
			persistentDataContainer.remove(BUGNET_CAPTURE)
			displayName(displayName()!!.color(NamedTextColor.BLUE))
		}

		val launchSpeed = sqrt(net.getEnchantmentLevel(Enchantment.KNOCKBACK) + 1.0) * LAUNCH_SPEED
		captured.velocity = player.location.direction.multiply(launchSpeed)
		player.world.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.6f)

		val startTime = System.currentTimeMillis()

		var task = -1
		task = Bukkit.getScheduler().scheduleSyncRepeatingTask(JuiceboxPlugin.instance, fun() {
			if (System.currentTimeMillis() - startTime > THROW_PARTICLE_LIFE || !captured.isValid) {
				Bukkit.getScheduler().cancelTask(task)
				return
			}
			captured.world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, captured.location, 7, 0.3, 0.3, 0.3, 0.01)
		}, 0L, 1L)
	}

	@EventHandler
	private fun onCatch(ev: PlayerInteractAtEntityEvent) {
		val net = ev.player.inventory.getItem(ev.hand)
		if (!net.isBugNet) return
		val player = ev.player
		if (player.hasCooldown(net.type)) return
		if (net.captured != null) return

		ev.isCancelled = true

		if (player.gameMode != GameMode.CREATIVE) {
			if (player.level == 0) {
				player.world.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f)
				return
			}
			player.level--
			net.itemMeta = net.itemMeta.apply {
				val item = this as? Damageable ?: return@apply

				val unbreaking = item.getEnchantLevel(Enchantment.DURABILITY)
				if (Math.random() < 1.0 / (unbreaking + 1)) {
					item.damage++
				}
			}
		}
		player.world.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f)

		val ent = ev.rightClicked


		player.setCooldown(net.type, NET_COOLDOWN_TICKS)
		net.itemMeta = net.itemMeta.apply {
			persistentDataContainer.setString(BUGNET_CAPTURE, ent.uniqueId.toString())
			displayName(displayName()!!.color(NamedTextColor.GREEN))
		}

		ent.fireTicks += net.getEnchantmentLevel(Enchantment.FIRE_ASPECT) * 100

		var task = -1
		var count = 0
		task = Bukkit.getScheduler().scheduleSyncRepeatingTask(
			JuiceboxPlugin.instance, fun() {
				val captured = net.captured

				val noNetInHand = !(player.inventory.itemInMainHand.isBugNet || player.inventory.itemInOffHand.isBugNet)
				if (!player.isValid || noNetInHand || captured == null || !captured.isValid || (player.gameMode != GameMode.CREATIVE && player.level == 0)) {
					Bukkit.getScheduler().cancelTask(task)
					net.itemMeta = net.itemMeta.apply {
						persistentDataContainer.remove(BUGNET_CAPTURE)
						displayName(displayName()!!.color(NamedTextColor.BLUE))
					}
					return
				}

				val targetBlock = player.getTargetBlockExact(4)
				val target = if (targetBlock != null) {
					val dist = targetBlock.location.distance(player.location)
					player.eyeLocation.add(player.eyeLocation.direction.multiply(dist))
				} else player.eyeLocation.add(player.eyeLocation.direction.multiply(3))

				captured.teleport(target)

				if ((count++) % EXPERIENCE_COST_COOLDOWN == 0) player.giveExp(-1)
			}, 0L, 1L
		)
	}

	private val ItemStack.isBugNet get() = itemMeta?.persistentDataContainer?.hasFlag(IS_BUGNET) == true

	private val ItemStack.captured: Entity?
		get() {
			val uuid = itemMeta.persistentDataContainer.getString(BUGNET_CAPTURE)
			uuid ?: return null
			return Bukkit.getEntity(UUID.fromString(uuid))
		}
}
