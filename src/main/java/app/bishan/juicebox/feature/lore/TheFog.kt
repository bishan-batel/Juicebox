package app.bishan.juicebox.feature.lore

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.TitlePart
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object TheFog : Feature("the_fog", false) {
	override fun onEnable() {
		addCommand("fog", this::fog, this::fogTabComplete)
	}

	private fun fogTabComplete(ignored: CommandSender, args: Array<out String>): List<String> {
		if (args.size != 1) {
			return emptyList()
		}
		return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0]) }
	}

	private fun fog(sender: CommandSender, args: Array<out String>): Boolean {
		if (sender !is Player) return false

		if (args.isEmpty()) {
			sender.sendMessage("Please specify a player name")
			return false
		}

		val target = Bukkit.getPlayer(args[0])
		if (target == null) {
			sender.sendMessage("Player ${args[0]} not found")
			return false
		}

		target.sendTitlePart(TitlePart.TITLE, Component.text("The Fog is Coming.", NamedTextColor.RED))

		var task = -1
		task = Bukkit.getScheduler().scheduleSyncRepeatingTask(
			JuiceboxPlugin.instance,
			fun() {
				if (!target.isValid || target.sendViewDistance <= 2) {
					Bukkit.getScheduler().cancelTask(task)
					return
				}
				target.sendViewDistance = target.sendViewDistance - 1
			},
			0L, 20L
		)
		return false
	}
}
