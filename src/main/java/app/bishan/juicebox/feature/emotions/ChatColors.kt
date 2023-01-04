package app.bishan.juicebox.feature.emotions

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.getInt
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler

object ChatColors : Feature("chat_colors", false, Scope.INTERNAL) {
	private val COLOR = JuiceboxPlugin.key("chat_color")

	@EventHandler
	private fun onMessage(ev: AsyncChatEvent) {
		val player = ev.player

		Bukkit.broadcast(
			Component.textOfChildren(
				Component.text("<", NamedTextColor.GRAY),
				player.displayName().color(player.getColor()),
				Component.text("> ", NamedTextColor.GRAY),
				ev.message()
			)
		)

		ev.isCancelled = true
	}

	private fun Player.getColor(): NamedTextColor {
		val color = persistentDataContainer.getInt(COLOR) ?: return NamedTextColor.WHITE
		return NamedTextColor.namedColor(color) ?: NamedTextColor.WHITE
	}
}
