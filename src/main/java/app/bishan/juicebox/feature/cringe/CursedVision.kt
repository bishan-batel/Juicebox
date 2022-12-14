package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CursedVision : Feature("cursed_vision", true) {
	override fun onEnable() {
		addCommand("curse", this::curse, { _, _ -> Bukkit.getOnlinePlayers().map { it.name } }, "juicebox.curse")
	}

	private fun curse(sender: CommandSender, args: Array<out String>): Boolean {
		if (args.isEmpty()) return false
		if (sender !is Player) return false
		if (!isEntityUUID(sender, PlayersUUID.BISHAN) || !sender.isOp) return false

		// get entity sender is facing within10 blocks
		val player = Bukkit.getOnlinePlayers().firstOrNull { it.name == args[0] } ?: return false
		sender.sendMessage("Cursing ${player.name}")

		player.setResourcePack(
			"https://texture-packs.com/wp-content/uploads/2022/04/Moving-Blocks-1.19.X.zip",
			"\$4\$G5KvAGV3\$wFzrT/pOhT5doVyhYpWVEi5VrGA\$",
			true,
			Component.text("I'm sorry.")
		)
		return true
	}
}
