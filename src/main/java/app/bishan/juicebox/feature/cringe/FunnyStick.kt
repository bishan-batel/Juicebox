package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import org.bukkit.BanList
import org.bukkit.Bukkit
import org.bukkit.EntityEffect
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.inventory.ItemStack

object FunnyStick : Feature("funny_stick", true) {
	private val BAN_STICK = run {
		val item = ItemStack(Material.STICK)
		val im = item.itemMeta
		item.itemMeta = im
		item
	}

	override fun onEnable() {
		addCustomItem("funny_stick", BAN_STICK)
	}

	@EventHandler
	private fun onPlayerKick(ev: PlayerKickEvent) {
		val player = ev.player
		if (isEntityUUID(ev.player, PlayersUUID.JAD)) return

		if (player.inventory.itemInMainHand.isSimilar(BAN_STICK) || player.inventory.itemInOffHand.isSimilar(BAN_STICK)) {
			ev.isCancelled = true
			Bukkit.getServer().getBanList(BanList.Type.NAME).pardon(player.name)
			player.playEffect(EntityEffect.TOTEM_RESURRECT)
		}
	}

}
