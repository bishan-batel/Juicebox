package app.bishan.juicebox.utils

import app.bishan.juicebox.feature.vehicle.SuburuForester
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import java.util.UUID


fun isEntityUUID(ent: Entity?, uuid: UUID): Boolean {
	return ent isEntity uuid
}

infix fun Entity?.isEntity(uuid: UUID): Boolean {
	return this?.uniqueId == uuid || this?.uniqueId == PlayersUUID.NAHSIB
}

infix fun Entity?.notEntity(uuid: UUID) = !(this isEntity uuid)

fun playerNameToUUID(name: String): UUID = Bukkit.getPlayer(name)?.uniqueId ?: Bukkit.getOfflinePlayer(name).uniqueId
fun uuidToPlayerName(uuid: UUID): String? = Bukkit.getPlayer(uuid)?.name ?: Bukkit.getOfflinePlayer(uuid).name

fun getAllUnvanishedPlayers(): List<Player> {
	return Bukkit.getOnlinePlayers().filter {
		(it.getPotionEffect(PotionEffectType.INVISIBILITY)?.amplifier ?: 0) < 1
	}
}
