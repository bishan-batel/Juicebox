package app.bishan.juicebox.utils

import app.bishan.juicebox.JuiceboxPlugin
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask


object Scheduler {
	private inline val scheduler get() = Bukkit.getScheduler()

	fun deferAsync(task: () -> Unit) {
		val runnable = task as BukkitRunnable
		runnable.runTaskAsynchronously(JuiceboxPlugin.instance)
	}

	fun deferAsync(delay: Long, task: () -> Unit): BukkitTask =
		scheduler.runTaskLaterAsynchronously(JuiceboxPlugin.instance, task, delay)

	fun defer(task: () -> Unit) =
		scheduler.runTask(JuiceboxPlugin.instance, task)

	fun defer(delay: Long, task: () -> Unit) =
		scheduler.runTaskLater(JuiceboxPlugin.instance, task, delay)

	fun onceEvery(ticks: Long, action: () -> Unit) = onceEvery(0, ticks, action)

	fun onceEvery(delay: Long, ticks: Long, action: () -> Unit) = Bukkit.getScheduler().runTaskTimer(
		JuiceboxPlugin.instance, action, 0, ticks
	)

	// formatting
	fun timeLeft(delta: Long): String {
		val days = delta / (1000 * 60 * 60 * 24)
		val hours = delta / (1000 * 60 * 60) % 24
		val minutes = delta / (1000 * 60) % 60
		val seconds = delta / 1000 % 60


		fun plural(num: Long) = if (num == 1L) "" else "s"

		return when {
			days > 0 -> "$days day${plural(days)} and $hours hour${plural(hours)}"
			hours > 0 -> "$hours hour${plural(days)} and $minutes minute${plural(minutes)}"
			minutes > 0 -> "$minutes minute${plural(days)} and $seconds second${plural(seconds)}"
			else -> "$seconds second${plural(days)}"
		}
	}
}
