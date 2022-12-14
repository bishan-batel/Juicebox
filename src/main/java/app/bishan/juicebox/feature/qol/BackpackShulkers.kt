package app.bishan.juicebox.feature.qol

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.hasFlag
import app.bishan.juicebox.utils.lowerFlag
import app.bishan.juicebox.utils.raiseFlag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.block.ShulkerBox
import org.bukkit.command.CommandSender
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta

object BackpackShulkers : Feature("backpack_shulkers", false) {
	private val OPEN_BACKPACK = JuiceboxPlugin.key("BackpackOpen")
	private var mutableShulkers = false

	override fun onEnable() {
		val dataFile = customData()
		if (dataFile.exists()) dataFile.readText().toBoolean().also { mutableShulkers = it }

		addCommand("mutable_backpacks", this::setMutableBackpacks, { _, _ -> listOf("enable", "disable", "query") })
	}

	private fun setMutableBackpacks(sender: CommandSender, args: Array<out String>): Boolean {
		if (args.isEmpty() || args[0] == "query") {
			sender.sendMessage(
				Component.text("Mutable backpacks are ").color(NamedTextColor.GRAY).append(
					Component.text(if (mutableShulkers) "enabled" else "disabled")
						.color(if (mutableShulkers) NamedTextColor.GREEN else NamedTextColor.RED)
				)
			)
			return true
		}

		if (args[0] != "enable" && args[0] != "disable") {
			sender.sendMessage(Component.text("Invalid argument: ${args[0]}").color(NamedTextColor.RED))
			return false
		}

		mutableShulkers = args[0] == "enable"
		customData().writeText(mutableShulkers.toString())

		sender.sendMessage(
			Component.text("Backpacks are now ${if (mutableShulkers) "mutable" else "immutable"}")
				.color(if (mutableShulkers) NamedTextColor.GREEN else NamedTextColor.RED)
		)
		return true
	}

	override fun onDisable() {
		customData().writeText(mutableShulkers.toString())
	}

	@EventHandler
	private fun onRightClickShulker(ev: PlayerInteractEvent) {
		if (ev.action != Action.RIGHT_CLICK_AIR) return
		val item = ev.item ?: return

//		if (item.type != Material.SHULKER_BOX) return

		val meta = item.itemMeta
		if (meta !is BlockStateMeta) return

		val shulker = meta.blockState
		if (shulker !is ShulkerBox) return

		val player = ev.player
		openShulker(player, shulker, item, meta)
//		Bukkit.broadcast(Component.text("Scheduled task $task"))
	}

	private fun openShulker(
		player: HumanEntity, shulker: ShulkerBox, item: ItemStack, meta: BlockStateMeta
	) {
		player.persistentDataContainer.raiseFlag(OPEN_BACKPACK)

		val inv = Bukkit.createInventory(player, shulker.inventory.type, item.displayName())
		inv.contents = shulker.inventory.contents
		player.openInventory(inv)


		// creates update task
		var task = -1
		task = Bukkit.getScheduler().scheduleSyncRepeatingTask(
			JuiceboxPlugin.instance,
			fun() {
				if (!isBackpackInventory(inv)) {
					Bukkit.getScheduler().cancelTask(task)
					//					Bukkit.broadcast(Component.text("Cancelled task $task"))
					return
				}

				shulker.inventory.contents = inv.contents
				meta.blockState = shulker
				item.itemMeta = meta
			},
			0L,
			1L,
		)
		player.world.playSound(player.location, Sound.BLOCK_SHULKER_BOX_OPEN, 1f, 1f)
	}

	private fun isBackpackInventory(inv: Inventory): Boolean {
		val holder = inv.holder
		if (holder !is Player) return false
		return holder.persistentDataContainer.hasFlag(OPEN_BACKPACK)
	}

	@EventHandler
	fun onShulkerClose(ev: InventoryCloseEvent) {
		if (isBackpackInventory(ev.inventory)) {
			ev.player.persistentDataContainer.lowerFlag(OPEN_BACKPACK)
			ev.player.world.playSound(ev.player.location, Sound.BLOCK_SHULKER_BOX_CLOSE, 1f, 1f)
		}
	}

	// prevent items from being taken from the shulker
	@EventHandler
	fun onShulkerClick(ev: InventoryClickEvent) {
		if (mutableShulkers) return
		if (!(isBackpackInventory(ev.inventory) || ev.clickedInventory?.let { isBackpackInventory(it) } == true)) return
		if (ev.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			ev.isCancelled = true
			return
		}
		if (ev.clickedInventory == ev.whoClicked.inventory) return
		ev.isCancelled = true
	}

//	@EventHandler
//	private fun onRightClickShulkerInInventory(ev: InventoryClickEvent) {
//		ev.whoClicked.sendMessage("what_3: ${ev.isRightClick}, ${ev.action.name}")
//		if (!ev.isRightClick) return
//		ev.whoClicked.sendMessage("_3")
//		val player = ev.whoClicked
//		if (ev.clickedInventory != player.inventory) return
//		ev.whoClicked.sendMessage("_4")
//		if (player.persistentDataContainer.hasFlag(OPEN_BACKPACK)) return
//		ev.whoClicked.sendMessage("_5")
//
//		val item = ev.currentItem ?: return
//		val meta = item.itemMeta
//		if (meta !is BlockStateMeta) return
//
//		val shulker = meta.blockState
//		if (shulker !is ShulkerBox) return
//		ev.isCancelled = true
//
//		openShulker(player, shulker, item, meta)
//		Bukkit.getScheduler().runTaskLater(JuiceboxPlugin.instance, fun() {
//			if (player.itemOnCursor.isSimilar(item)) {
//				player.setItemOnCursor(null)
//			}
//		}, 1L)
//	}

	@EventHandler
	fun onShulkerDrag(ev: InventoryDragEvent) {
		if (mutableShulkers) return
		if (!isBackpackInventory(ev.inventory)) return
		ev.isCancelled = true
//		if (!ev.whoClicked.persistentDataContainer.hasFlag(OPEN_BACKPACK)) return
//		if (ev.inventory == ev.whoClicked.inventory) return
//		ev.isCancelled = true
	}

	@EventHandler
	fun onShulkerMove(ev: InventoryMoveItemEvent) {
		if (mutableShulkers) return
		if (!(isBackpackInventory(ev.destination) || isBackpackInventory(ev.source))) return
		ev.isCancelled = true
	}

	@EventHandler
	fun onShulkerDrop(ev: PlayerDropItemEvent) {
		if (!ev.player.persistentDataContainer.hasFlag(OPEN_BACKPACK)) return

		val droppedItem = ev.itemDrop.itemStack

		val shulker = (droppedItem.itemMeta as? BlockStateMeta)?.blockState as? ShulkerBox ?: return

		if (shulker.inventory.contents.contentEquals(ev.player.openInventory.topInventory.contents)) {
			ev.player.closeInventory()
		}
	}
}
