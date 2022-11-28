package app.bishan.juicebox.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.UUID


fun isEntityUUID(player: Entity?, id: UUID): Boolean =
	player?.uniqueId == id || player?.uniqueId == PlayersUUID.NAHSIB

fun playerNameToUUID(name: String): UUID = Bukkit.getPlayer(name)?.uniqueId ?: Bukkit.getOfflinePlayer(name).uniqueId
fun uuidToPlayerName(uuid: UUID): String? = Bukkit.getPlayer(uuid)?.name ?: Bukkit.getOfflinePlayer(uuid).name
