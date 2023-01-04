package app.bishan.juicebox.feature.real

import app.bishan.juicebox.feature.Feature
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockFormEvent

object DeepSlateGenerator : Feature("deepslate_generator", true) {
	@EventHandler
	private fun onBlockForm(ev: BlockFormEvent) {
		if (ev.block.y >= 0) return

		val state = ev.newState

		val replace = when (state.type) {
			Material.STONE -> Material.DEEPSLATE
			Material.COBBLESTONE -> Material.COBBLED_DEEPSLATE
			else -> return
		}.createBlockData()

		ev.block.world.setBlockData(ev.block.location, replace)
		ev.block.world.playSound(ev.block.location, Sound.BLOCK_LAVA_EXTINGUISH, 1f, 1f)
		ev.isCancelled = true
	}
}
