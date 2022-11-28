package app.bishan.juicebox.cmd

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

typealias JuiceboxSubCommand = (sender: CommandSender, args: Array<out String>) -> Boolean
typealias JuiceboxTabCompleter = (sender: CommandSender, args: Array<out String>) -> List<String>

data class JuiceboxCommand(val cmd: JuiceboxSubCommand, val tab: JuiceboxTabCompleter?, val permission: String)

class JuiceboxCommandHandler : CommandExecutor, TabCompleter {
	companion object {
		const val DEFAULT_PERMISSION = "juicebox.feature_control"
	}

	private var handlers: MutableMap<String, JuiceboxCommand> = HashMap()

	init {
		registerCommand("feature", ::featureSubCommand, ::featureSubCommandTabCompleter)
	}

	@Suppress("UNUSED_PARAMETER")
	private fun featureSubCommandTabCompleter(commandSender: CommandSender, strings: Array<out String>): List<String> {
		return when (strings.size) {
			1 -> Feature.allFeatures.keys.toList()
			2 -> listOf("enable", "disable")
			else -> emptyList()
		}
	}

	private fun featureSubCommand(commandSender: CommandSender, args: Array<out String>): Boolean {
		if (args.isEmpty()) {
			return false
		}

		val featureName = args[0]
		val feature = JuiceboxPlugin.instance.getFeature(featureName)

		if (feature == null) {
			commandSender.sendMessage("Feature $featureName does not exist")
			return false
		}

		if (args.size < 2) {
			val state = if (feature.isActive()) "${ChatColor.GREEN}enabled" else "${ChatColor.RED}disabled"
			commandSender.sendMessage("Feature $featureName is $state")
			return true
		}

		when (args[1]) {
			"enable" -> {
				JuiceboxPlugin.instance.activateFeature(featureName, feature)
				commandSender.sendMessage("Feature $featureName had been ${ChatColor.GREEN}enabled")
			}

			"disable" -> {
				JuiceboxPlugin.instance.deactivateFeature(featureName, feature)
				commandSender.sendMessage("Feature $featureName has been ${ChatColor.RED}disabled")
			}

			else -> {
				return false
			}
		}
		return true
	}

	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
		if (args.isEmpty()) {
			return false
		}

		val subCommand = args[0]
		val subArgs = args.copyOfRange(1, args.size)

		val handler = handlers[subCommand]
		if (handler == null) {
			sender.sendMessage("Subcommand $subCommand does not exist")
			return false
		}

		return if (sender.hasPermission(handler.permission)) {
			handler.cmd(sender, subArgs)
		} else {
			sender.sendMessage(
				Component.text("You do not have permission to use this command").color(TextColor.color(0xFF0000))
			)
			false
		}
	}

	override fun onTabComplete(
		sender: CommandSender, command: Command, label: String, args: Array<out String>
	): MutableList<String>? {
		if (args.isEmpty()) return null

		if (args.size == 1)
			return handlers.filter { it.key.startsWith(args[0]) && sender.hasPermission(it.value.permission) }
				.keys.toMutableList()

		val subCommand = args[0]
		val subArgs = args.copyOfRange(1, args.size)
		val handler = handlers[subCommand] ?: return null

		return handler.tab?.invoke(sender, subArgs)?.toMutableList()
	}

	fun registerCommand(
		name: String, cmd: JuiceboxSubCommand, tabCompleter: JuiceboxTabCompleter?, permission: String? = null
	) {
		handlers[name] = JuiceboxCommand(cmd, tabCompleter ?: { _, _ -> listOf() }, permission ?: DEFAULT_PERMISSION)
	}

	fun unregisterCommand(name: String) {
		handlers.remove(name)
	}

}

