package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.Team

object CustomTags : Feature("custom_tags", true) {
	private const val VIP_TEAM = "vip"
	private const val TALL_GLASS_TEAM = "tall_glass_team"

	@EventHandler
	fun onPlayerJoin(ev: PlayerJoinEvent) {
		val player = ev.player

		(if (isEntityUUID(player, PlayersUUID.WATER_MUNCH)) {
			tallGlassTeam()
		} else if (isEntityUUID(player, PlayersUUID.RENI)) {
			vipTeam()
		} else {
			null
		})?.addPlayer(player)
	}

	private fun tallGlassTeam(): Team {
		val scoreboard = Bukkit.getScoreboardManager().mainScoreboard

		val existing = scoreboard.getTeam(TALL_GLASS_TEAM)
		if (existing != null) return existing

		val team = scoreboard.registerNewTeam(TALL_GLASS_TEAM)
		team.color(NamedTextColor.BLUE)
		team.prefix(Component.text("[tall glass of] ", NamedTextColor.BLUE))
		return team
	}

	private fun vipTeam(): Team {
		val scoreboard = Bukkit.getScoreboardManager().mainScoreboard

		val existing = scoreboard.getTeam(VIP_TEAM)
		if (existing != null) return existing

		val team = scoreboard.registerNewTeam(VIP_TEAM)
		team.color(NamedTextColor.LIGHT_PURPLE)
		team.prefix(Component.text("[VIP] ", NamedTextColor.LIGHT_PURPLE))
		return team
	}
}
