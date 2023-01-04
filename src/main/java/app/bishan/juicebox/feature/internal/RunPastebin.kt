package app.bishan.juicebox.feature.internal

import app.bishan.juicebox.feature.Feature
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.net.URL

object RunPastebin : Feature("internal:run_pastebin", true, Scope.INTERNAL) {
	override fun onEnable() {
		addCommand("run_pastebin", this::runPastebin)
	}

	private fun runPastebin(sender: CommandSender, args: Array<out String>): Boolean {
		val code = args.firstOrNull()

		if (code == null) {
			// Error saying that there are too few arguments
			sender.sendMessage(Component.text("Specify a pastebin code", NamedTextColor.RED))
			return true
		}

		if (args.size > 1) {
			// Error saying that there are too many arguments
			sender.sendMessage(Component.text("Too many arguments", NamedTextColor.RED))
			return true
		}

		val url = "https://pastebin.com/raw/$code"
		sender.sendMessage(Component.text("Retrieving commands from $url", NamedTextColor.GREEN))

		val commands = try {
			URL(url).readText().lines()
		} catch (e: Exception) {
			sender.sendMessage(Component.text("Failed to retrieve commands", NamedTextColor.RED))
			return true
		}

		sender.sendMessage(Component.text("Running ${commands.size} commands", NamedTextColor.GREEN))

		commands.forEachIndexed { index, command ->
			sender.sendMessage(
				Component.textOfChildren(
					Component.text("[$index: ", NamedTextColor.GRAY, TextDecoration.ITALIC),
					Component.text(command, NamedTextColor.WHITE),
					Component.text("]", NamedTextColor.GRAY, TextDecoration.ITALIC)
				)
			)

			val ran = try {
				Bukkit.dispatchCommand(sender, command)
			} catch (e: Exception) {
				sender.sendMessage(Component.text("Failed to run command", NamedTextColor.RED))
				false
			}

			sender.sendMessage(
				Component.textOfChildren(
					Component.text("[$index: ", NamedTextColor.GRAY, TextDecoration.ITALIC),
					if (ran) Component.text("Completed", NamedTextColor.GREEN)
					else Component.text("Failed", NamedTextColor.RED),
					Component.text("]", NamedTextColor.GRAY, TextDecoration.ITALIC),
					Component.newline()
				)
			)
		}

		return true
	}
}
