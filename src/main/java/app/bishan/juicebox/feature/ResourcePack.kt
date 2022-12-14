package app.bishan.juicebox.feature

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.utils.hasFlag
import app.bishan.juicebox.utils.lowerFlag
import app.bishan.juicebox.utils.raiseFlag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object ResourcePack : Feature("juicepack", true) {
	private val REQUIRE_RESOURCE_PACK = JuiceboxPlugin.key("require-resource-pack")
	private var updateTask = -1
	private var juiceboxHash = ""

	override fun onEnable() {
		addCommand("resource_pack", this::pack, null, "juicebox.pack")

//		updateTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
//			JuiceboxPlugin.instance, fun() {
//			}, 0L, 20 * 60 * 30
//		)
	}

	@EventHandler
	private fun onJoin(ev: PlayerJoinEvent) {
		if (ev.player.persistentDataContainer.hasFlag(REQUIRE_RESOURCE_PACK)) sendResourcePack(ev.player)
	}

	private fun pack(sender: CommandSender, args: Array<out String>): Boolean {
		if (sender !is Player) return false

		val flag = sender.persistentDataContainer.hasFlag(REQUIRE_RESOURCE_PACK)

		if (flag) {
			sender.sendMessage(
				Component.textOfChildren(
					Component.text("You've"),
					Component.text(" disabled ", NamedTextColor.RED),
					Component.text("the juice pack, you can unload it by reloggin")
				)
			)
			sender.persistentDataContainer.lowerFlag(REQUIRE_RESOURCE_PACK)
		} else {
			sender.sendMessage(
				Component.textOfChildren(
					Component.text("You've"), Component.text(" enabled ", NamedTextColor.GREEN), Component.text("the juice pack")
				)
			)
			sender.persistentDataContainer.raiseFlag(REQUIRE_RESOURCE_PACK)
			sendResourcePack(sender)
		}
		return true
	}

	private fun sendResourcePack(sender: Player) {
		JuiceboxPlugin.instance.logger.info("Sending resource pack to ${sender.name} with hash: $juiceboxHash")
		HttpClient.newHttpClient().sendAsync(
			HttpRequest.newBuilder().uri(URI.create("https://slime-juicebox.web.app/juicebox.sha1")).build(),
			HttpResponse.BodyHandlers.ofString()
		).thenAccept {
			juiceboxHash = it.body().substring(0, 40)
			sender.setResourcePack(
				"https://slime-juicebox.web.app/juicebox.zip",
				juiceboxHash,
				true
			)
		}
	}
}
