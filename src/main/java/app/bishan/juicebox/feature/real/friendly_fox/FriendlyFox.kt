package app.bishan.juicebox.feature.real.friendly_fox

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Fox
import org.bukkit.entity.LivingEntity

object FriendlyFox : Feature("Friendly Fox", false) {
	val VOX_FOX get() = JuiceboxPlugin.key("IsVoxFox")

	override fun onEnable() {
		addCommand("summon_vox_fox", this::summonFox, null)
	}

	private fun summonFox(sender: CommandSender, args: Array<out String>): Boolean {
		if (sender !is Entity) {
			sender.sendMessage("Must be an entity to use this command")
			return false
		}
		val loc = sender.location
		val world = sender.world

		val fox = world.spawnEntity(loc, EntityType.FOX) as Fox

		fox.pathfinder.setCanOpenDoors(true)

		var task = -1
		task = Bukkit.getScheduler().scheduleSyncRepeatingTask(JuiceboxPlugin.instance, fun() {
			if (!sender.isValid) {
				Bukkit.getScheduler().cancelTask(task)
				return
			}

			fox.pathfinder.moveTo(sender as LivingEntity)
		}, 0L, 1L)
		return true
	}
}
