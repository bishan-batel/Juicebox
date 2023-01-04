package app.bishan.juicebox.feature.lore

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.Scheduler
import app.bishan.juicebox.utils.notEntity
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler

object NPC : Feature("npc", true) {
	override val description: Component
		get() = Component.text("NPC Behaviour")

	private const val DEVIL_GUN_MODE = "Press all buttons to activate Devil Gun Mode"
	private val DEVIL_GUN_SFX = NamespacedKey("minecraft", "npc.devil_gun_mode")

	@EventHandler
	private fun onChat(ev: AsyncChatEvent) {
		if (ev.player notEntity PlayersUUID.BISHAN) return

		val msg = ev.originalMessage()
		if (msg.toString().contains(DEVIL_GUN_MODE, true)) {
			Scheduler.defer {
				devilGunMode(ev.player)
			}
		}
	}

	private fun devilGunMode(player: Player) {
		player.world.playSound(
			Sound.sound(
				DEVIL_GUN_SFX,
				Sound.Source.MASTER,
				1.0f,
				1.0f
			),
			player.location.x,
			player.location.y,
			player.location.z,
		)
//		player.world.stopSound(SoundStop.named(DEVIL_GUN_SFX))
	}
}
