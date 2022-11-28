package app.bishan.juicebox.feature.lore

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerMoveEvent

object JesusGoose : Feature("jesus_goose", false) {
	@EventHandler
	private fun onPlayerMove(ev: PlayerMoveEvent) {
		val player = ev.player
		if (!isEntityUUID(player, PlayersUUID.BISHAN)) return
		val blockBelow = player.location.subtract(0.0, 1.0, 0.0).block
		if (blockBelow.type == Material.WATER) {
			player.sendBlockChange(blockBelow.location, Material.LIGHT_BLUE_STAINED_GLASS.createBlockData())
		} else if (blockBelow.type == Material.LAVA) {
			player.sendBlockChange(blockBelow.location, Material.RED_STAINED_GLASS.createBlockData())
		}
	}
}
