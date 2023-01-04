package app.bishan.juicebox.cmd

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

typealias JuiceboxSubCommand = (sender: CommandSender, args: Array<out String>) -> Boolean
typealias JuiceboxTabCompleter = (sender: CommandSender, args: Array<out String>) -> List<String>


class JuiceboxCommandHandler : CommandExecutor, TabCompleter {
	companion object {
		const val DEFAULT_PERMISSION = "juicebox.feature_control"

		data class JuiceboxCommand(val cmd: JuiceboxSubCommand, val tab: JuiceboxTabCompleter?, val permission: String)
	}

	private var handlers: MutableMap<String, JuiceboxCommand> = HashMap()

	init {
		registerCommand("feature", ::featureSubCommand, ::featureSubCommandTabCompleter)
		registerCommand("listActiveFeatures", ::listActiveFeaturesSubCommand, null)
	}

	private fun listActiveFeaturesSubCommand(sender: CommandSender, args: Array<out String>): Boolean {
		val activeFeatures = Feature.allFeatures.filter { it.value.isActive() }.keys.sorted()

		if (activeFeatures.isEmpty()) {
			sender.sendMessage(Component.text("No active features").color(TextColor.color(0xFF0000)))
			return true
		}

		sender.sendMessage(Component.text("Active features:").color(NamedTextColor.GREEN))
		for ((i, feature) in activeFeatures.withIndex()) {
			sender.sendMessage(
				Component.text(" - $feature").color(
					if (i % 2 == 0) NamedTextColor.GRAY else NamedTextColor.WHITE
				)
			)
		}
		return true
	}

	@Suppress("UNUSED_PARAMETER")
	private fun featureSubCommandTabCompleter(sender: CommandSender, args: Array<out String>): List<String> {
		return when (args.size) {
			1 -> Feature.allFeatures.filterValues { it.scope != Feature.Scope.INTERNAL }.keys.toList()

			2 -> listOf("help", "status", "enable", "disable", "reload")
			else -> emptyList()
		}.filter { it.startsWith(args.lastOrNull() ?: "") }
	}

	private fun featureSubCommand(sender: CommandSender, args: Array<out String>): Boolean {
		if (args.isEmpty()) {
			// print out all features
			sender.sendMessage(Component.text("Available features:").color(NamedTextColor.GREEN))
			for (feature in Feature.allFeatures) {
				sender.sendMessage(
					Component.text(" - ${feature.key}").color(
						if (feature.value.isActive()) NamedTextColor.GREEN else NamedTextColor.RED
					)
				)
			}
			return true
		}

		val featureName = args[0]
		val feature = JuiceboxPlugin.instance.getFeature(featureName)

		if (feature == null) {
			sender.sendMessage("Feature $featureName does not exist")
			return false
		}

		if (feature.scope == Feature.Scope.INTERNAL) {
			sender.sendMessage("Feature $featureName is internal, cannot modify")
			return false
		}

		if (args.size < 2) {
			sender.sendMessage("Usage: /feature $featureName <enable|disable|reload|status>")
			return true
		}

		when (args[1]) {
			"enable" -> {
				JuiceboxPlugin.instance.activateFeature(featureName, feature)
				sender.sendMessage("Feature $featureName had been ${ChatColor.GREEN}enabled")
			}

			"disable" -> {
				JuiceboxPlugin.instance.deactivateFeature(featureName, feature)
				sender.sendMessage("Feature $featureName has been ${ChatColor.RED}disabled")
			}

			"reload" -> {
				sender.sendMessage("Reloading Feature $featureName...")

				JuiceboxPlugin.instance.deactivateFeature(featureName, feature)
				JuiceboxPlugin.instance.activateFeature(featureName, feature)

				sender.sendMessage("Feature $featureName has been ${ChatColor.GREEN}reloaded")
			}

			"status" -> {
				val state = if (feature.isActive()) "${ChatColor.GREEN}enabled" else "${ChatColor.RED}disabled"
				sender.sendMessage("Feature $featureName is $state")
			}

			"help" -> {
				sender.sendMessage(Component.text {
					it.append(Component.text("Feature $featureName", NamedTextColor.GREEN))
					it.append(Component.newline())
					it.append(feature.description)
					it.append(Component.newline())
				})
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
				Component.text("You do not have permission to use this command").color(NamedTextColor.RED)
			)
			false
		}
	}

	override fun onTabComplete(
		sender: CommandSender, command: Command, label: String, args: Array<out String>
	): MutableList<String>? {
		if (args.isEmpty()) return null

		if (args.size == 1) return handlers.filter {
			it.key.startsWith(args[0]) && sender.hasPermission(it.value.permission)
		}.keys.filter { !it.startsWith("hidden:") }.toMutableList()

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

