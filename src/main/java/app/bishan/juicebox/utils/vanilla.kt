package app.bishan.juicebox.utils

import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

// give item to inventory holder, if inventory is full then drop item on ground
fun InventoryHolder.giveItem(item: ItemStack) {
	if (this is Entity && inventory.firstEmpty() == -1) {
		world.dropItem(location, item)
	} else {
		inventory.addItem(item)
	}
}
