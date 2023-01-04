package app.bishan.juicebox.feature.blizzard

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.ProjectileHitEvent

object RockSnowballs : Feature("rock_snowball", false) {
	override val description
		get() = Component.text(
			"""
				Snowballs will deal damage to entities when thrown
			""".trimIndent()
		)

	private val IS_STONED = JuiceboxPlugin.key("rock_snowball")
	private val TARGET = JuiceboxPlugin.key("snowball_target")

	@EventHandler
	fun onThrow(ev: PlayerLaunchProjectileEvent) {
		val ball = ev.projectile as? Snowball ?: return

		Scheduler.defer {
			ball.persistentDataContainer.raiseFlag(IS_STONED)

			ev.player.location.getNearbyEntities(50.0, 50.0, 50.0)
				.filterIsInstance<Damageable>()
				.firstOrNull { it != ev.player }
				?.let {
					ball.persistentDataContainer.setVector(TARGET, it.location.toVector())
				}

		}
	}

	@EventHandler
	fun onLand(ev: ProjectileHitEvent) {
		if (ev.entity.persistentDataContainer.missingFlag(IS_STONED)) return
		val ent = ev.hitEntity as? Damageable ?: return

		val shooter = ev.entity.shooter
		if (shooter !is Entity) ent.damage(1.0)
		else ent.damage(1.0, shooter)
	}
}
