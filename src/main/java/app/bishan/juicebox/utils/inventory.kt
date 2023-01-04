package app.bishan.juicebox.utils

import org.bukkit.Material
import org.bukkit.enchantments.EnchantmentWrapper
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class InventoryDataType : PersistentDataType<String, Map<Int, ItemStack?>> {
	override fun getPrimitiveType() = String::class.java

	@Suppress("UNCHECKED_CAST")
	override fun getComplexType(): Class<Map<Int, ItemStack?>> = Map::class.java as Class<Map<Int, ItemStack?>>

	override fun fromPrimitive(primitive: String, context: PersistentDataAdapterContext): Map<Int, ItemStack?> {
		val map = mutableMapOf<Int, ItemStack?>()
		val bytes = ByteArrayInputStream(Base64Coder.decodeLines(primitive))
		val data = BukkitObjectInputStream(bytes)

		val size = data.readInt()


		for (i in 0 until size) {
			val slot = data.readInt()

			if (data.readBoolean()) {
				map[slot] = data.readObject() as ItemStack
				continue
			}
			map[slot] = ItemStack(Material.AIR)
		}

		return map
	}

	override fun toPrimitive(inv: Map<Int, ItemStack?>, ctx: PersistentDataAdapterContext): String {
		val totalItems = inv.size

		val outStream = ByteArrayOutputStream()
		val data = BukkitObjectOutputStream(outStream)

		data.writeInt(totalItems)

		for ((slot, item) in inv) {
			data.writeInt(slot)
			if (item == null || item.type == Material.AIR) {
				data.writeBoolean(false)
			} else {
				data.writeBoolean(true)
				data.writeObject(item)
			}
		}

		data.close()
		return Base64Coder.encodeLines(outStream.toByteArray())
	}
}

fun Map<Int, ItemStack?>.populate(inv: Inventory) {
	for ((slot, item) in this) {
		inv.setItem(slot, item ?: ItemStack(Material.AIR))
	}
}

private val ENCHANT_GLINT = EnchantmentWrapper("juicebox_glint")
fun ItemStack.addEnchantmentGlint() = addUnsafeEnchantment(ENCHANT_GLINT, 1)
fun ItemStack.removeEnchantmentGlint() = removeEnchantment(ENCHANT_GLINT)
val ItemStack.isAir get() = type == Material.AIR
