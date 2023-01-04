package app.bishan.juicebox.feature.lore

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Ravager
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

object VolaRavager : Feature("vola_ravager", false) {
	private val TAMED = JuiceboxPlugin.key("vola_ravager_tamed")
	private val INVENTORY = JuiceboxPlugin.key("vola_ravager_inventory")

	private val openInventories = mutableMapOf<String, Inventory>()

	override fun onEnable() {
		addCommand("create_ravager", this::createRavager)
	}

	private fun createRavager(sender: CommandSender, args: Array<out String>): Boolean {
		if (sender !is Entity) {
			sender.sendMessage(Component.text("You must be an entity to use this command", NamedTextColor.RED))
			return true
		}

		val ent = sender.world.spawnEntity(sender.location, EntityType.RAVAGER) as Ravager
		ent.persistentDataContainer.raiseFlag(TAMED)
		ent.isPersistent = true
		ent.isPatrolLeader = false
		ent.setLeashHolder(sender)
		return true
	}

	@EventHandler
	private fun onDeath(ev: EntityDeathEvent) {
		val tamed = ev.entity.asTamed() ?: return

		Scheduler.defer {
			if (tamed.isValid && tamed.isDead.not()) return@defer
			openInventories.remove(tamed.uniqueId.toString())

			tamed.inventory.forEach { item ->
				tamed.world.dropItem(tamed.location, item)
			}
		}
	}

	@EventHandler
	private fun onTargetEntity(ev: EntityTargetLivingEntityEvent) {
		ev.entity.asTamed() ?: return
		val target = ev.target ?: return

		if (target is HumanEntity) {
			ev.isCancelled = true
		}
	}

	@EventHandler
	private fun interactWithTamed(ev: PlayerInteractAtEntityEvent) {
		if (ev.hand != EquipmentSlot.HAND) return
		val tamed = ev.rightClicked.asTamed() ?: return

		val player = ev.player

		if (player.isSneaking) {
			val uuid = tamed.uniqueId.toString()

			val inv = openInventories[uuid] ?: tamed.inventory
			player.openInventory(inv)
			openInventories[uuid] = inv


			var task: BukkitTask? = null

			task = Scheduler.onceEvery(1) {
				if (inv.viewers.isEmpty() || tamed.isValid.not() || tamed.isDead) {
					task?.cancel()
					openInventories.remove(uuid)
					return@onceEvery
				}

				tamed.inventory = inv
			}
			return
		}

		val hand = player.inventory.itemInMainHand
		val offhand = player.inventory.itemInOffHand

		if (player.inventory.isHoldingItem(ItemStack(Material.LEAD)) && !tamed.isLeashed) {
			tamed.removePassenger(player)

			(if (hand.type == Material.LEAD) hand else offhand).amount--
			Scheduler.defer {
				tamed.setLeashHolder(player)
			}
		} else tamed.addPassenger(player)
	}

	private var Ravager.inventory: Inventory
		set(v) = persistentDataContainer.set(
			INVENTORY, InventoryDataType(), v.mapIndexed { index, itemStack ->
				index to itemStack
			}.toMap()
		)
		get() {
			val inv = Bukkit.createInventory({ inventory }, InventoryType.CHEST)
			persistentDataContainer.get(INVENTORY, InventoryDataType())?.populate(inv)
			return inv
		}

	private fun Entity?.asTamed(): Ravager? {
		if (this == null) return null
		if (type != EntityType.RAVAGER) return null
		this as Ravager
		return if (persistentDataContainer.hasFlag(TAMED)) this else null
	}

}
