package app.bishan.juicebox.feature.lore

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import io.papermc.paper.event.entity.WardenAngerChangeEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Warden
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPotionEffectEvent

object VolaImmunity : Feature("vola_immunity", true) {
	@EventHandler
	fun onDamaged(ev: EntityDamageByEntityEvent) {
		if (ev.damager !is Warden) return
		if (!isEntityUUID(ev.entity, PlayersUUID.VOLA)) return
		val warden = ev.damager as Warden
		warden.clearAnger(ev.entity)
		ev.isCancelled = true
	}

	@EventHandler
	fun onAnger(ev: WardenAngerChangeEvent) {
		if (!isEntityUUID(ev.target, PlayersUUID.VOLA)) return
		ev.newAnger = 0
		ev.entity.target = null
	}

	@EventHandler
	fun onBlinded(ev: EntityPotionEffectEvent) {
		if (!isEntityUUID(ev.entity, PlayersUUID.VOLA)) return
		if (ev.cause == EntityPotionEffectEvent.Cause.WARDEN) ev.isCancelled = true
	}
}
