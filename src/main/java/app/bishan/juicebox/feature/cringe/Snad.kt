package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.PlayersUUID
import app.bishan.juicebox.utils.isEntityUUID
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack

object Snad : Feature("snad", true) {
	@EventHandler
	private fun onBlockBreak(ev: BlockBreakEvent) {
		if (!isEntityUUID(ev.player, PlayersUUID.BISHAN)) return
		if (!ev.isDropItems) return
		if (ev.block.type != Material.SAND) return

		// clears block
		ev.block.world.setBlockData(ev.block.location, Material.AIR.createBlockData())

		// spawns snad
		val stack = ItemStack(Material.SAND)
		val im = stack.itemMeta
		if (im != null) {
			im.displayName(Component.text("Snad").decoration(TextDecoration.ITALIC, false))
			stack.itemMeta = im
		}
		ev.block.world.dropItemNaturally(ev.block.location, stack)

		ev.isCancelled = true
	}
}
