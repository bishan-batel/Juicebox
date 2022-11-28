package app.bishan.juicebox.feature.lock

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import com.destroystokyo.paper.event.block.BlockDestroyEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.block.Container
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import java.util.*

object ChestLocking : Feature("chest_locking", true) {
	private const val LOCKING_PERMISSION = "juicebox.chest_locking.locking"
	private const val OVERRIDE_LOCKING_PERMISSION = "juicebox.chest_locking.override"

	private val CHEST_PLACER = JuiceboxPlugin.key("ChestPlacer")
	private val CHEST_LOCKED = JuiceboxPlugin.key("ChestLocked")
	private val CHEST_KEY_OWNERS = JuiceboxPlugin.key("ChestKeyOwners")

	override fun onEnable() {
		addCommand("lock", ::lockCmd, null, LOCKING_PERMISSION)
		addCommand("unlock", this::unlock, null, LOCKING_PERMISSION)
		addCommand("key_give", this::giveKey, this::giveKeyAutocomplete, LOCKING_PERMISSION)
		addCommand("key_revoke", this::revokeKey, this::revokeKeyAutocomplete, LOCKING_PERMISSION)
		addCommand("lockstatus", this::lockStatus, null, OVERRIDE_LOCKING_PERMISSION)
	}

	private fun lockStatus(sender: CommandSender, args: Array<out String>): Boolean {
		// if they have override privelages, give them the status of the chest they're looking at
		val player = sender as? Player ?: return false
		val block = player.getTargetBlock(5) ?: return false
		val container = block.state as? Container ?: return false
		val locked = container.persistentDataContainer.hasFlag(CHEST_LOCKED)

		val owner = container.persistentDataContainer.getString(CHEST_PLACER)
		val ownerName = if (owner != null) uuidToPlayerName(UUID.fromString(owner)) else {
			sender.sendMessage(Component.text("This chest has no properties"))
			return true
		} ?: "Unknown!"

		val owners = container.persistentDataContainer.getString(CHEST_KEY_OWNERS)?.split(",")
			?.map { uuidToPlayerName(UUID.fromString(it)) } ?: emptyList()

		sender.sendMessage(
			Component.text("This chest is ${if (locked) "locked" else "unlocked"}").color(NamedTextColor.GOLD)
		)
		sender.sendMessage(
			Component.text("Placed by: $ownerName").color(NamedTextColor.GOLD)
		)
		sender.sendMessage(
			Component.text("Key owners: ${if (owners.isEmpty()) "none" else owners.joinToString(", ")}")
				.color(NamedTextColor.GOLD)
		)
		// play sound effect
		sender.playSound(sender.location, "block.note_block.pling", 1f, 1f)
		return true
	}

	private fun giveKeyAutocomplete(sender: CommandSender, args: Array<out String>): List<String> {
		if (args.size != 1) return emptyList()
		if (sender !is Player) return emptyList()

		val container = sender.getTargetBlock(5)?.state as? Container ?: return emptyList()
		val keyOwners = container.persistentDataContainer.getString(CHEST_KEY_OWNERS)
			?.split(",")?.map {
				uuidToPlayerName(UUID.fromString(it)) ?: ""
			} ?: emptyList()

		return Bukkit.getOnlinePlayers().map { it.name }.filter { it !in keyOwners && it != sender.name }
	}

	private fun revokeKeyAutocomplete(sender: CommandSender, args: Array<out String>): List<String> {
		if (args.size != 1) return emptyList()
		if (sender !is Player) return emptyList()
		val container = sender.getTargetBlock(5)?.state as? Container ?: return emptyList()
		return container.persistentDataContainer.getString(CHEST_KEY_OWNERS)
			?.split(",")?.map {
				uuidToPlayerName(UUID.fromString(it)) ?: ""
			} ?: emptyList()
	}

	@Suppress("DuplicatedCode")
	private fun giveKey(sender: CommandSender, args: Array<out String>): Boolean {
		if (args.isEmpty()) {
			sender.sendMessage(Component.text("Must provide a player name").color(TextColor.color(NamedTextColor.RED)))
			return true
		}

		val chest = ownerChestFromSender(sender) ?: return false

		val playerName = args.first()
		val pid = playerNameToUUID(playerName)

		val keyOwners =
			chest.persistentDataContainer.getString(CHEST_KEY_OWNERS)?.split(",")?.toMutableList() ?: mutableListOf()
		if (keyOwners.filter { it.isNotBlank() }.map {
				UUID.fromString(it)
			}.contains(pid)) {
			sender.sendMessage(
				Component.text("Player $playerName already has a key").color(TextColor.color(NamedTextColor.RED))
			)
			return true
		}

		keyOwners.add(pid.toString())

		val str = keyOwners.joinToString(",", "", "")
		chest.persistentDataContainer.setString(CHEST_KEY_OWNERS, str)
		chest.update()

		// play lock sound
		chest.world.playSound(chest.location, "block.chest.locked", 1f, 1f)

		sender.sendMessage(Component.text("Gave key to $playerName").color(TextColor.color(NamedTextColor.GREEN)))
		return true
	}

	@Suppress("DuplicatedCode")
	private fun revokeKey(sender: CommandSender, args: Array<out String>): Boolean {
		if (args.isEmpty()) {
			sender.sendMessage(Component.text("Must provide a player name").color(TextColor.color(NamedTextColor.RED)))
			return true
		}

		val chest = ownerChestFromSender(sender) ?: return false

		val playerName = args.first()
		val pid = playerNameToUUID(playerName)

		val ownersUUID =
			chest.persistentDataContainer.getString(CHEST_KEY_OWNERS)?.split(",")?.toMutableList() ?: mutableListOf()

		if (!ownersUUID.filter { it.isNotBlank() }.map { UUID.fromString(it) }.contains(pid)) {
			sender.sendMessage(
				Component.text("Player $playerName does not have a key").color(TextColor.color(NamedTextColor.RED))
			)
			return true
		}

		ownersUUID.remove(pid.toString())
		if (ownersUUID.isEmpty()) chest.persistentDataContainer.remove(CHEST_KEY_OWNERS)
		else chest.persistentDataContainer.setString(CHEST_KEY_OWNERS, ownersUUID.joinToString(",", "", ""))

		chest.update()

		chest.world.playSound(chest.location, "block.chest.locked", 1f, 1f)
		sender.sendMessage(Component.text("Revoked key from $playerName").color(TextColor.color(NamedTextColor.GREEN)))
		return true
	}

	private fun unlock(sender: CommandSender, _args: Array<out String>): Boolean {
		val chest = ownerChestFromSender(sender) ?: return false

		if (!chest.persistentDataContainer.hasFlag(CHEST_LOCKED)) {
			sender.sendMessage(Component.text("This chest is not locked").color(NamedTextColor.RED))
			return true
		}

		chest.persistentDataContainer.remove(CHEST_LOCKED)
		chest.persistentDataContainer.remove(CHEST_KEY_OWNERS)
		chest.update()

		sender.sendMessage(Component.text("Chest unlocked").color(NamedTextColor.GREEN))
		return true
	}

	private fun lockCmd(sender: CommandSender, _args: Array<out String>): Boolean {
		val chest = ownerChestFromSender(sender) ?: return false
		if (chest.persistentDataContainer.hasFlag(CHEST_LOCKED)) {
			sender.sendMessage(Component.text("This chest is already locked").color(NamedTextColor.RED))
			return true
		}

		// raise lock flag
		chest.persistentDataContainer.raiseFlag(CHEST_LOCKED)
		chest.update()

		sender.sendMessage(Component.text("Locked chest").color(NamedTextColor.GREEN))
		chest.block.world.playSound(chest.block.location, "block.chest.locked", 1f, 1f)
		return true
	}

	@EventHandler
	private fun openChest(ev: InventoryOpenEvent) {
//		if (ev.action != Action.RIGHT_CLICK_BLOCK) return
		val chest = ev.inventory.holder
		if (chest !is Container) return

		if (!chest.persistentDataContainer.hasFlag(CHEST_LOCKED)) return

		val owner = UUID.fromString(chest.persistentDataContainer.getString(CHEST_PLACER) ?: return)

		if (owner == ev.player.uniqueId) return
		if (ev.player.hasPermission(OVERRIDE_LOCKING_PERMISSION)) {
			ev.player.sendActionBar(Component.text("You have overridden the chest lock!").color(NamedTextColor.RED))
			return
		}

		val owners = chest.persistentDataContainer.getString(CHEST_KEY_OWNERS)?.split(",")?.map { UUID.fromString(it) }
		if (owners?.contains(ev.player.uniqueId) == true) return

		ev.player.sendActionBar(Component.text("This chest is locked!"))
		// play locked sfx
		ev.player.world.playSound(chest.block.location, "block.chest.locked", 1f, 1f)
		ev.isCancelled = true
	}

	@EventHandler
	private fun onPlaceChest(ev: BlockPlaceEvent) {
		val chest = ev.blockPlaced.state
		if (chest !is Container) return
		val player = ev.player
		chest.persistentDataContainer.setString(CHEST_PLACER, player.uniqueId.toString())
		chest.update()
	}

	@EventHandler
	private fun onDestroyChest(ev: BlockBreakEvent) {
		// prevent breaking locked chests if they are not the owner
		val chest = ev.block.state
		if (chest !is Container) return
		val player = ev.player
		if (!chest.persistentDataContainer.hasFlag(CHEST_LOCKED)) return
		val owner = UUID.fromString(chest.persistentDataContainer.getString(CHEST_PLACER) ?: return)
		if (owner == player.uniqueId) return
		if (player.hasPermission(OVERRIDE_LOCKING_PERMISSION)) {
			player.sendActionBar(Component.text("You have overridden the chest lock!").color(NamedTextColor.RED))
			return
		}
		ev.isCancelled = true
	}

	@EventHandler
	private fun itemMoveFromLockedChest(ev: InventoryMoveItemEvent) {
		// prevent items from being moved out of a locked chest
		val chest = ev.source.holder
		if (chest !is Container) return
		if (!chest.persistentDataContainer.hasFlag(CHEST_LOCKED)) return
		ev.isCancelled = true
	}

	private fun ownerChestFromSender(sender: CommandSender): Container? {
		if (sender !is Player) {
			sender.sendMessage(Component.text("Only players can use this command").color(NamedTextColor.RED))
			return null
		}

		val block = sender.getTargetBlock(5)
		val chest = block?.state as? Container
		if (chest !is Container) {
			sender.sendMessage(Component.text("You must be looking at a container").color(NamedTextColor.RED))
			return null
		}

		val placer = chest.persistentDataContainer.getString(CHEST_PLACER)
		if (placer != null || sender.uniqueId != UUID.fromString(placer)) {
			sender.sendMessage(Component.text("You do not own this chest").color(NamedTextColor.RED))
			return null
		}
		return chest
	}

	@EventHandler
	private fun onPiston(ev: BlockPistonExtendEvent) {
		val chest = ev.block.state
		if (chest !is Container) return
		if (!chest.persistentDataContainer.hasFlag(CHEST_LOCKED)) return
		ev.isCancelled = true
	}

	@EventHandler
	private fun onBlownUp(ev: BlockExplodeEvent) {
		val chest = ev.block.state
		if (chest !is Container) return
		if (!chest.persistentDataContainer.hasFlag(CHEST_LOCKED)) return
		ev.isCancelled = true
	}
}
