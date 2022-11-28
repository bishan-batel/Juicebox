package app.bishan.juicebox.feature.cringe

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import app.bishan.juicebox.utils.raiseFlag
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Silverfish
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

object LittleGuy : Feature("little_guy", false) {
	private val LITTLE_GUY = JuiceboxPlugin.key("little_guy")

	@EventHandler
	fun onPumpkinPlace(ev: BlockPlaceEvent) {
		if (ev.blockPlaced.type != Material.CARVED_PUMPKIN) return

//		Bukkit.broadcast(Component.text("pumpkin placed"))

		// assert that block below is snow
		val blockBelow = ev.blockPlaced.location.clone().subtract(0.0, 1.0, 0.0).block
		val blockTwoBelow = ev.blockPlaced.location.clone().subtract(0.0, 2.0, 0.0).block
		if (blockBelow.type != Material.SNOW_BLOCK) return
		if (blockTwoBelow.type == Material.SNOW_BLOCK) return


//		Bukkit.broadcast(Component.text("snow below"))

		// spawn a little guy
		val littleGuy =
			ev.blockPlaced.world.spawnEntity(blockBelow.location.toCenterLocation(), EntityType.ARMOR_STAND) as ArmorStand
		littleGuy.persistentDataContainer.raiseFlag(LITTLE_GUY)
		littleGuy.isSmall = true
		littleGuy.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING)
		littleGuy.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.REMOVING_OR_CHANGING)
		littleGuy.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.REMOVING_OR_CHANGING)
		littleGuy.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.REMOVING_OR_CHANGING)
		littleGuy.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.REMOVING_OR_CHANGING)
		littleGuy.addEquipmentLock(EquipmentSlot.OFF_HAND, ArmorStand.LockType.REMOVING_OR_CHANGING)
		littleGuy.equipment.helmet = ItemStack(Material.CARVED_PUMPKIN)
		littleGuy.setArms(true)
		littleGuy.setBasePlate(false)

		val silverfish = ev.blockPlaced.world.spawnEntity(littleGuy.location, EntityType.SILVERFISH) as Silverfish
		silverfish.isInvisible = true
		silverfish.isSilent = true
		silverfish.addPassenger(littleGuy)

		// make little guy face player
		littleGuy.location.yaw = ev.player.location.yaw + Math.PI.toFloat()
		ev.blockPlaced.type = Material.AIR
		blockBelow.type = Material.AIR
	}
}
