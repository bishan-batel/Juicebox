package app.bishan.juicebox.feature.emotions

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object HeadPat : Feature("headpat", true) {
	private val HEADPAT_COUNT get() = JuiceboxPlugin.key("HeadPatCount")
	private val DISALLOW_HEADPATS get() = JuiceboxPlugin.key("DisallowHeadPats")

	override fun onEnable() {
		addCommand("headpat", this::headpat, this::headpatTabComplete, "juicebox.headpat")
		addCommand("headpat_toggle", this::toggleHeadpat, null, "juicebox.headpat")
	}

	private fun toggleHeadpat(sender: CommandSender, args: Array<out String>): Boolean {
		if (sender !is Player) {
			sender.sendMessage("Must be a player!")
			return false
		}

		if (args.isNotEmpty()) {
			sender.sendMessage("Too many arguments! (${args.size} > 0)")
			return false
		}

		val disallow = !sender.persistentDataContainer.hasFlag(DISALLOW_HEADPATS)
		sender.sendMessage(
			Component.textOfChildren(
				Component.text("You are now "),
				if (disallow)
					Component.text("blocking", NamedTextColor.RED)
				else Component.text(
					"allowing",
					NamedTextColor.GREEN
				),
				Component.text(" any headpats"),
			)
		)
		sender.persistentDataContainer.setFlag(DISALLOW_HEADPATS, disallow)
		return true
	}

	private fun headpatTabComplete(sender: CommandSender, args: Array<out String>): List<String> {
		if (args.size > 1) {
			return listOf()
		}

		val names = getAllUnvanishedPlayers().filterNot { it == sender }.map { it.name }
		if (args.isEmpty()) return names
		return names.filter { it.startsWith(args.first()) }
	}

	private fun headpat(sender: CommandSender, args: Array<out String>): Boolean {
		if (args.isEmpty()) {
			sender.sendMessage(Component.text("Please specify a name", NamedTextColor.RED))
			return false
		}

		val player = Bukkit.getPlayer(args[0])

		if (player == null || !player.isOnline || player !in getAllUnvanishedPlayers()) {
			sender.sendMessage(Component.text("${args[0]} is not online!", NamedTextColor.RED))
			return false
		}

		if (player == sender) {
			sender.sendMessage(Component.text("You cannot headpat yourself!", NamedTextColor.RED))
			return false
		}

		if (player.persistentDataContainer.hasFlag(DISALLOW_HEADPATS)) {
			sender.sendMessage(Component.text("${player.name} is not accepting headpats!", NamedTextColor.RED))
			return false
		}

		val count = (player.persistentDataContainer.getLong(HEADPAT_COUNT) ?: 1) + 1
		player.persistentDataContainer.setLong(HEADPAT_COUNT, count)

		val countTxt = ("$count" + when ("$count".last()) {
			'1' -> "st"
			'2' -> "nd"
			'3' -> "rd"
			else -> "th"
		})

		Bukkit.broadcast(
			Component.textOfChildren(
				Component.text(player.name, NamedTextColor.LIGHT_PURPLE),
				Component.text(" has been patted for the "),
				Component.text(countTxt, NamedTextColor.BLUE),
				Component.text(" time by "),
				Component.text(sender.name, NamedTextColor.LIGHT_PURPLE)
			)
		)

		player.world.spawnParticle(
			Particle.HEART,
			player.location.add(0.0, 1.3, 0.0),
			6,
			0.3,
			0.3,
			0.3
		)
		player.world.playSound(
			player,
			Sound.ENTITY_VILLAGER_CELEBRATE,
			1f,
			1f
		)
		return true
	}
}
