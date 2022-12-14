package app.bishan.juicebox.utils

import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

class InventoryDataType : PersistentDataType<String, Map<Int, ItemStack>> {
	companion object {
		const val SEPARATOR = "\n\r"
	}

	override fun getPrimitiveType() = String::class.java

	@Suppress("UNCHECKED_CAST")
	override fun getComplexType(): Class<Map<Int, ItemStack>> = Map::class.java as Class<Map<Int, ItemStack>>

	override fun fromPrimitive(primitive: String, context: PersistentDataAdapterContext): Map<Int, ItemStack> {
		val map = mutableMapOf<Int, ItemStack>()
		val items = primitive.split(SEPARATOR)

		for (item in items) {
			val split = item.split(":")
			val slot = split[0].toInt()
			val itemStack = ItemStack.deserializeBytes(split[1].toByteArray())
			map[slot] = itemStack
		}
		return map
	}

	override fun toPrimitive(inv: Map<Int, ItemStack>, ctx: PersistentDataAdapterContext): String {
		val sb = StringBuilder()

		inv.forEach { (slot, item) ->
			sb.append("$slot:${item.serialize()}$SEPARATOR")
		}
		return sb.toString()
	}
}

