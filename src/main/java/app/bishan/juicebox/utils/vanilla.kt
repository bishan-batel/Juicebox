package app.bishan.juicebox.utils

import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

// give item to inventory holder, if inventory is full then drop item on ground
fun InventoryHolder.giveItem(item: ItemStack) {
	if (this is Entity && inventory.firstEmpty() == -1) {
		world.dropItem(location, item)
	} else {
		inventory.addItem(item)
	}
}

fun PlayerInventory.isHoldingItem(item: ItemStack) = itemInMainHand.isSimilar(item) || itemInOffHand.isSimilar(item)
fun PlayerInventory.isHoldingItem(mat: Material) = itemInMainHand.type == mat || itemInOffHand.type == mat

fun PlayerInventory.filterItemInHand(filter: (item: ItemStack) -> Boolean) =
	itemInMainHand.takeIf(filter) ?: itemInOffHand.takeIf(filter)
