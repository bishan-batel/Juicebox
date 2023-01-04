package app.bishan.juicebox.feature.internal

import app.bishan.juicebox.JuiceboxPlugin
import app.bishan.juicebox.feature.Feature
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentWrapper

object CustomEnchants : Feature("internal:custom_enchants", true, Scope.INTERNAL) {
	private val customEnchants = mutableSetOf<Enchantment>()

	fun registerEnchantment(enchant: Enchantment) {
		customEnchants.add(enchant)

		try {
			val accepting = Enchantment::class.java.getDeclaredField("acceptingNew")
			accepting.isAccessible = true
			accepting.set(null, true)
			accepting.isAccessible = false

			EnchantmentWrapper.registerEnchantment(enchant)

		} catch (e: Exception) {
			JuiceboxPlugin.instance.logger.warning(e.stackTraceToString())
		}
		Enchantment.stopAcceptingRegistrations()
	}

	fun unregisterEnchantment(enchant: Enchantment) {
		customEnchants.remove(enchant)

		try {
			val key = enchant.key
			val byKey = Enchantment::class.java.getDeclaredField("byKey")
			byKey.isAccessible = true
			byKey.set(null, byKey.get(null) as MutableMap<*, *> - key)
			byKey.isAccessible = false
		} catch (e: Exception) {
			JuiceboxPlugin.instance.logger.warning(e.stackTraceToString())
		}
	}
}
