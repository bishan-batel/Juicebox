package app.bishan.juicebox.feature.internal

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.Scheduler
import app.bishan.juicebox.utils.getLong
import app.bishan.juicebox.utils.setLong
import app.bishan.juicebox.utils.Scheduler.timeLeft
import com.destroystokyo.paper.event.server.WhitelistToggleEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitTask
import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.activity.ActivityType
import org.javacord.api.entity.intent.Intent
import org.javacord.api.entity.user.User

object JuiceBot : Feature("internal:juice_bot", true, Scope.PUBLIC) {
	private var bot: DiscordApi? = null
	private val token get() = config()["token"] as? String? ?: throw Exception("Bot token not found in config.yml")
	private val serverId get() = config()["serverId"] as? String? ?: throw Exception("Server ID not found in config.yml")
	private val TIME_WARNED = JuiceboxPlugin.key("bot_discord_warned")
	private var checkWhitelistTask: BukkitTask? = null

	// 1 day in millis
	private const val WARN_TIME = 60 * 60 * 24 * 1000L
	private val WARN_TIME_LEFT get() = config()["warnTime"] as? Long ?: WARN_TIME
//	private const val WARN_TIME = 2 * 55 * 60 * 1000L

	override fun onEnable() {
		this.customData()

		bot?.disconnect()
		checkWhitelistTask?.cancel()

		bot = DiscordApiBuilder().setToken(token).addIntents(Intent.GUILD_MEMBERS).login().join()!!

		val api = bot!!

		api.updateActivity(ActivityType.WATCHING, "me when when me me when when me when \uD83D\uDE0E")

		checkWhitelistTask = Scheduler.onceEvery(20L, 20L * 60L * 10L) {
			Bukkit.getOnlinePlayers().forEach(::checkPlayer)
		}

		addCommand("whitelist", ::whitelist, ::whitelistTab)

		addCommand("reload_bot", { sender, args ->
			sender.sendMessage(Component.text("Reloading bot...").color(NamedTextColor.GREEN))
			try {
				onDisable()
			} catch (e: Exception) {
				JuiceboxPlugin.instance.logger.severe(e.stackTraceToString())
				sender.sendMessage(Component.text("Failed to disable bot").color(NamedTextColor.RED))
			}

			sender.sendMessage(Component.text("Reloaded bot").color(NamedTextColor.GREEN))
			true
		})
	}


	private fun whitelist(sender: CommandSender, args: Array<out String>): Boolean {
		val subArgs = args.slice(1 until args.size)

		return when (args.getOrNull(0)) {
			"add" -> whitelistAdd(sender, subArgs)
			"remove" -> whitelistRemove(sender, subArgs)
			else -> {
				sender.sendMessage(Component.text("Usage: /whitelist <add, remove>").color(NamedTextColor.RED))
				true
			}
		}
	}

	private fun whitelistTab(sender: CommandSender, args: Array<out String>): List<String> {
		return when (args.size) {
			1 -> listOf("add", "remove")
			2 -> Bukkit.getOnlinePlayers().map { it.name }
			else -> emptyList()
		}
	}

	private fun whitelistRemove(sender: CommandSender, args: List<String>): Boolean {
		if (args.isEmpty()) return false
		val offline = Bukkit.getOfflinePlayer(args[0])

		offline.isWhitelisted = false
		offline.discordId = null

		sender.sendMessage(Component.text("Unwhitelisted ${offline.name}").color(NamedTextColor.GREEN))

		enforceMembership(offline.player ?: return true)
		return true
	}

	private fun whitelistAdd(sender: CommandSender, args: List<String>): Boolean {
		val player = args.firstOrNull()?.let { Bukkit.getOfflinePlayer(it) }
		if (player == null) {
			sender.sendMessage(Component.text("Player not found", NamedTextColor.RED))
			return true
		}

		// discord id second argument
		val dId = args.getOrNull(1)?.toLongOrNull()
		if (dId == null) {
			sender.sendMessage(Component.text("Discord ID not found", NamedTextColor.RED))
			return true
		}

		// check if discord id is valid
		val user = bot?.getUserById(dId)
		if (user == null) {
			sender.sendMessage(Component.text("Discord ID is invalid", NamedTextColor.RED))
			return true
		}

		// attempt to add to whitelist
		player.discordId = dId

		if (!player.isInDiscordServer()) {
			sender.sendMessage(Component.text("Player is not in the discord server", NamedTextColor.RED))
			return true
		}

		JuiceboxPlugin.instance.logger.info("Whitelisting ${player.name} with discord id $dId")

		player.isWhitelisted = true
		sender.sendMessage(Component.text("Whitelisted ${player.name}").color(NamedTextColor.GREEN))

//		player.persistentDataContainer.setLong(DISCORD_ID, discordId)
		return true
	}

	@EventHandler
	private fun onPlayerJoin(ev: PlayerJoinEvent) = checkPlayer(ev.player)

	private fun checkPlayer(player: Player) {
		if (player.hasAttachedDiscordAccount()) return

		val timeWarned = run {
			val time = player.persistentDataContainer.getLong(TIME_WARNED)
			if (time == null) {
				player.persistentDataContainer.setLong(TIME_WARNED, System.currentTimeMillis())
				System.currentTimeMillis()
			} else time
		}

		val timeSinceWarn = System.currentTimeMillis() - timeWarned

		if (timeSinceWarn >= WARN_TIME) {
			enforceMembership(player)
			return
		}

		val modMsg = if (player.isOp) "Use /jb link_discord_account <username> <discord name or ID>"
		else "Contact a moderator to link your account\n You have ${timeLeft(WARN_TIME - timeSinceWarn)} until this will be enforced!"


		val msg = Component.textOfChildren(
			Component.text("[\n", NamedTextColor.GRAY, TextDecoration.ITALIC),
			Component.text(
				"Your discord account is not linked to your minecraft account. \n$modMsg", NamedTextColor.RED
			),
			Component.text("\n]", NamedTextColor.GRAY, TextDecoration.ITALIC),
		)

		// send the message 20 ticks later
		Scheduler.defer(20L) { player.sendMessage(msg) }
	}

	private fun enforceMembership(player: Player) {
		if (player.isOp) return

		if (player.isInDiscordServer()) return

		// unwhitelist player & kick them with a link to the discord

		player.isWhitelisted = false

		Scheduler.defer {
			player.kick(
				Component.textOfChildren(
					Component.text("You must be in the discord server to play on this server.\n"),
					Component.text("https://discord.gg/RHKpNdHc", NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED)
						.clickEvent(ClickEvent.openUrl("https://discord.gg/RHKpNdHc"))
				)
			)
		}
	}

	/**
	 * Returns the user of the discord account linked to this player
	 * This will block the thread until the state of the user if resolved
	 */
	fun OfflinePlayer.attachedDiscordAccount(): User? {
		val id = discordId ?: return null
		return bot?.getUserById(id)?.join()
	}

	private fun OfflinePlayer.hasAttachedDiscordAccount() = attachedDiscordAccount() != null

	private fun OfflinePlayer.isInDiscordServer(): Boolean {
		val id = discordId ?: return false

		return bot?.getServerById(serverId)?.orElse(null)?.getMemberById(id)?.orElse(null) != null
	}


	/**
	 * Returns the attached discrd account ID or null if no account is attached
	 */
	var OfflinePlayer.discordId: Long?
		set(value) {
			val data = customData()
			val config = YamlConfiguration.loadConfiguration(data)

			config.set(uniqueId.toString(), value)
			config.save(data)
		}
		get() {
			val config = YamlConfiguration.loadConfiguration(customData())
			return config["$uniqueId"] as? Long?
		}

	override fun onDisable() {
		bot?.disconnect()
		checkWhitelistTask?.cancel()
		bot = null
	}
}
