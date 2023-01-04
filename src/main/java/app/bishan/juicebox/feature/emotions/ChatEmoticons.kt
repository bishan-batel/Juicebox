package app.bishan.juicebox.feature.emotions

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.feature.internal.ResourcePack
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler

object ChatEmoticons : Feature("chat_emoticons", false) {
	private val EMOTICONS = mutableMapOf(
		"skull" to "\uD83D\uDC80",
		"moyai" to "\uD83D\uDDFF",
	)
		.map {
			TextReplacementConfig.builder()
				.matchLiteral(":${it.key}:")
				.replacement(it.value)
				.build()
		}

	@EventHandler
	private fun onMessage(ev: AsyncChatEvent) {
		if (!ResourcePack.hasJuiceboxPack(ev.player)) return
		var msg = ev.message()
		for (emote in EMOTICONS) {
			msg = msg.replaceText(emote)
		}
		ev.message(msg)
//		Bukkit.broadcast(msg)
//		ev.isCancelled = true
	}
}
