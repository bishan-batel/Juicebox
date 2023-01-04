package app.bishan.juicebox.feature.internal

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.giveItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object GiveSubCommand : Feature("internal:give_sub_command", true, Scope.INTERNAL) {
	override fun onEnable() {
		addCommand("give", this::give, this::tab)
	}


	private fun give(sender: CommandSender, args: Array<out String>): Boolean {
		val features = allFeatures.values.filter { it.isActive() }.map { it.customItems }

		if (args.isEmpty()) {
			sender.sendMessage(Component.text("Please specify a feature", NamedTextColor.RED))
			return false
		}
		val name = args.first()

		for (feature in features) {
			feature[name]?.let {
				if (sender is Player) {
					sender.giveItem(it)
				} else {
					sender.sendMessage(Component.text("You must be a player to use this command", NamedTextColor.RED))
				}
				return true
			}
		}
		sender.sendMessage(Component.text("Item not found", NamedTextColor.RED))
		return false
	}

	private fun tab(sender: CommandSender, args: Array<out String>): List<String> {
		val items = allFeatures.values.filter { it.isActive() }.map { it.customItems }.flatMap { it.keys }.toList()

		return if (args.isNotEmpty())
			items.filter { it.startsWith(args.first()) }
		else items
	}
}
